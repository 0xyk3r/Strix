package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.ai.AiAttachmentResolver;
import cn.projectan.strix.core.module.ai.AiChatClient;
import cn.projectan.strix.core.module.ai.provider.AiProviderAdapter;
import cn.projectan.strix.core.module.ai.provider.AiProviderRegistry;
import cn.projectan.strix.core.module.ai.provider.AiUsageDetail;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiSession;
import cn.projectan.strix.model.dict.system.AiMessageStatus;
import cn.projectan.strix.model.request.system.module.ai.AiAttachment;
import cn.projectan.strix.model.response.system.ai.AiSseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

/**
 * AI 核心服务
 * <p>
 * 统一使用 {@link AiChatClient}（OkHttp）调用 OpenAI 兼容端点，
 * 通过 {@link AiProviderAdapter} 处理各提供商特有参数差异。
 * <p>
 * 两种调用方式：
 * <ul>
 *   <li>在线对话（SSE 流式）：{@link #streamChat} / {@link #streamRegenerate}</li>
 *   <li>程序化调用（同步阻塞）：{@link #chat} / {@link #analyzeMedia}</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-06-25 (重构自 Spring AI 版本)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiAttachmentResolver aiAttachmentResolver;
    private final AiChatClient aiChatClient;
    private final AiProviderRegistry providerRegistry;
    private final AiModelConfigService aiModelConfigService;
    private final AiSessionService aiSessionService;
    private final AiMessageService aiMessageService;

    // ============================================================
    //  流式在线对话（SSE）
    // ============================================================

    /**
     * 流式 AI 对话，将结果推送到 {@link SseEmitter}。应在虚拟线程中调用。
     *
     * @param sessionId   会话 ID
     * @param content     用户输入文本
     * @param attachments 附件列表（结构化附件对象），可为 null
     * @param emitter     SSE emitter
     * @param managerId   当前管理员 ID
     */
    public void streamChat(String sessionId, String content, List<AiAttachment> attachments,
                           SseEmitter emitter, String managerId) {
        // 1. 加载并校验会话
        AiSession session = aiSessionService.getById(sessionId);
        if (session == null || !session.getManagerId().equals(managerId)) {
            sendSseError(emitter, "会话不存在或无权限");
            return;
        }

        // 校验消息内容：文本和附件至少有一个非空
        boolean hasContent = StringUtils.hasText(content);
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        if (!hasContent && !hasAttachments) {
            sendSseError(emitter, "消息内容不能为空");
            return;
        }

        AiModelConfig config = aiModelConfigService.getById(session.getModelConfigId());
        if (config == null || config.getStatus() == null || config.getStatus() != 1) {
            sendSseError(emitter, "AI 模型配置不可用");
            return;
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 2. 解析附件（fileId → URL/base64）
        List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments = aiAttachmentResolver.resolve(attachments);

        // 3. 保存用户消息（存储原始 AiAttachment 而非解析后的 URL）
        AiMessage userMsg = new AiMessage()
                .setSessionId(sessionId)
                .setRole("user")
                .setContent(content)
                .setAttachments(attachmentsToJson(attachments))
                .setModelConfigId(config.getId())
                .setStatus(AiMessageStatus.COMPLETED);
        aiMessageService.save(userMsg);

        // 4. 保存 assistant 占位消息（生成中）
        AiMessage assistantMsg = new AiMessage()
                .setSessionId(sessionId)
                .setRole("assistant")
                .setContent("")
                .setStatus(AiMessageStatus.GENERATING);
        aiMessageService.save(assistantMsg);

        // 5. 加载历史上下文并构建 messages
        List<AiMessage> history = aiMessageService.listContextMessages(sessionId);
        List<Map<String, Object>> messages = buildRawMessages(config, history, content, resolvedAttachments);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);

        AiProviderAdapter adapter = providerRegistry.getAdapter(config);
        adapter.applyStreamingParams(body, config);

        runStreaming(config, body, adapter, assistantMsg.getId(), userMsg.getId(), emitter, startTime, sessionId);
    }

    /**
     * 重新生成最后一条 AI 回复（SSE 流式）。应在虚拟线程中调用。
     *
     * @param sessionId 会话 ID
     * @param emitter   SSE emitter
     * @param managerId 当前管理员 ID
     */
    public void streamRegenerate(String sessionId, SseEmitter emitter, String managerId) {
        // 1. 校验会话
        AiSession session = aiSessionService.getById(sessionId);
        if (session == null || !session.getManagerId().equals(managerId)) {
            sendSseError(emitter, "会话不存在或无权限");
            return;
        }

        AiModelConfig config = aiModelConfigService.getById(session.getModelConfigId());
        if (config == null || config.getStatus() == null || config.getStatus() != 1) {
            sendSseError(emitter, "AI 模型配置不可用");
            return;
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 2. 删除最后一条 assistant 消息（按雪花 id 取最新，避免秒级时间戳并列导致的顺序错乱）
        AiMessage lastAssistant = aiMessageService.lambdaQuery()
                .eq(AiMessage::getSessionId, sessionId)
                .eq(AiMessage::getRole, "assistant")
                .orderByDesc(AiMessage::getId)
                .last("LIMIT 1")
                .one();
        if (lastAssistant != null) {
            aiMessageService.removeById(lastAssistant.getId());
        }

        // 3. 找到最后一条 user 消息
        AiMessage lastUser = aiMessageService.lambdaQuery()
                .eq(AiMessage::getSessionId, sessionId)
                .eq(AiMessage::getRole, "user")
                .orderByDesc(AiMessage::getId)
                .last("LIMIT 1")
                .one();
        if (lastUser == null) {
            sendSseError(emitter, "没有可以重新生成的用户消息");
            return;
        }

        // 4. 解析附件 JSON → AiAttachment → resolve
        List<AiAttachment> attachments = parseAiAttachmentsJson(lastUser.getAttachments());
        List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments = aiAttachmentResolver.resolve(attachments);

        // 5. 保存新的 assistant 占位消息
        AiMessage assistantMsg = new AiMessage()
                .setSessionId(sessionId)
                .setRole("assistant")
                .setContent("")
                .setStatus(AiMessageStatus.GENERATING);
        aiMessageService.save(assistantMsg);

        // 6. 加载上下文 → 构建 messages → 流式调用
        List<AiMessage> history = aiMessageService.listContextMessages(sessionId);
        List<Map<String, Object>> messages = buildRawMessages(config, history, lastUser.getContent(), resolvedAttachments);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);

        AiProviderAdapter adapter = providerRegistry.getAdapter(config);
        adapter.applyStreamingParams(body, config);

        runStreaming(config, body, adapter, assistantMsg.getId(), null, emitter, startTime, sessionId);
    }

    /**
     * 统一流式执行（单一 OkHttp 路径，同时处理纯文本和多模态）
     */
    private void runStreaming(AiModelConfig config, Map<String, Object> body, AiProviderAdapter adapter,
                              String assistantMsgId, String userMsgId,
                              SseEmitter emitter, long startTime, String sessionId) {
        StringBuilder fullContent = new StringBuilder();
        StringBuilder thinkingContent = new StringBuilder();
        AiUsageDetail[] usageHolder = {AiUsageDetail.EMPTY};

        try {
            aiChatClient.streamChat(config.getBaseUrl(), config.getApiKey(), body, chunk -> {
                JsonNode usageNode = chunk.get("usage");
                if (usageNode != null && !usageNode.isNull()) {
                    usageHolder[0] = adapter.parseUsage(usageNode);
                }

                JsonNode choices = chunk.get("choices");
                if (choices == null || choices.isEmpty()) return;
                JsonNode choice = choices.get(0);
                JsonNode delta = choice.get("delta");
                if (delta == null) return;

                String thinkingDelta = adapter.extractReasoningContent(delta);
                if (StringUtils.hasText(thinkingDelta)) {
                    thinkingContent.append(thinkingDelta);
                    sendSseEvent(emitter, AiSseEvent.THINKING, Map.of("content", thinkingDelta));
                }

                JsonNode contentNode = delta.get("content");
                if (contentNode != null && !contentNode.isNull()) {
                    String contentDelta = contentNode.asText("");
                    if (!contentDelta.isEmpty()) {
                        fullContent.append(contentDelta);
                        sendSseEvent(emitter, AiSseEvent.CONTENT, Map.of("content", contentDelta));
                    }
                }
            });

            AiUsageDetail usage = usageHolder[0];
            Long durationMs = System.currentTimeMillis() - startTime;

            aiMessageService.markCompleted(assistantMsgId,
                    fullContent.toString(),
                    thinkingContent.isEmpty() ? null : thinkingContent.toString(),
                    usage.promptTokens(), usage.completionTokens(),
                    usage.cacheHitTokens(), usage.cacheWriteTokens(), usage.reasoningTokens(),
                    config.getId(), durationMs);

            Map<String, Object> doneData = new HashMap<>();
            doneData.put("messageId", assistantMsgId);
            if (userMsgId != null) doneData.put("userMessageId", userMsgId);
            doneData.put("modelConfigId", config.getId());
            doneData.put("modelConfigName", config.getName());
            if (usage.promptTokens() != null) doneData.put("promptTokens", usage.promptTokens());
            if (usage.completionTokens() != null) doneData.put("completionTokens", usage.completionTokens());
            if (usage.cacheHitTokens() != null) doneData.put("cacheHitTokens", usage.cacheHitTokens());
            if (usage.cacheWriteTokens() != null) doneData.put("cacheWriteTokens", usage.cacheWriteTokens());
            if (usage.reasoningTokens() != null) doneData.put("reasoningTokens", usage.reasoningTokens());
            sendSseEvent(emitter, AiSseEvent.DONE, doneData);
            emitter.complete();

        } catch (Exception e) {
            log.error("AI 流式调用出错: sessionId={}", sessionId, e);
            aiMessageService.markError(assistantMsgId, e.getMessage());
            sendSseError(emitter, "AI 调用出错: " + e.getMessage());
        }
    }

    // ============================================================
    //  程序化调用 - 文本/视觉对话（同步阻塞）
    // ============================================================

    /**
     * 同步文本对话（程序化调用）
     *
     * @param configKey 模型配置 key
     * @param messages  消息列表（[{"role":"user","content":"..."}]）
     * @return AI 响应文本
     */
    public String chat(String configKey, List<Map<String, Object>> messages) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        AiProviderAdapter adapter = providerRegistry.getAdapter(config);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);
        adapter.applyNonStreamingParams(body, config);

        try {
            JsonNode response = aiChatClient.chat(config.getBaseUrl(), config.getApiKey(), body);
            return response.at("/choices/0/message/content").asText("");
        } catch (IOException e) {
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 同步文本对话（简单单轮，程序化调用）
     *
     * @param configKey 模型配置 key
     * @param userInput 用户输入
     * @return AI 响应文本
     */
    public String chat(String configKey, String userInput) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        List<Map<String, Object>> messages = new ArrayList<>();
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", userInput));
        return chat(configKey, messages);
    }

    /**
     * 视觉模型分析图片/视频（程序化调用）
     *
     * @param configKey 模型配置 key
     * @param prompt    文本提示
     * @param mediaUrls 媒体 URL 列表（图片或视频公网 URL）
     * @param mimeTypes 对应的 MIME 类型（如 "image/jpeg"），与 mediaUrls 一一对应
     * @return AI 分析结果文本
     */
    public String analyzeMedia(String configKey, String prompt, List<String> mediaUrls, List<String> mimeTypes) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        AiProviderAdapter adapter = providerRegistry.getAdapter(config);

        List<Map<String, Object>> contentParts = new ArrayList<>();
        if (StringUtils.hasText(prompt)) {
            contentParts.add(Map.of("type", "text", "text", prompt));
        }
        for (int i = 0; i < mediaUrls.size(); i++) {
            String mediaUrl = mediaUrls.get(i);
            String mimeType = (mimeTypes != null && i < mimeTypes.size()) ? mimeTypes.get(i) : null;
            boolean isVideo = mimeType != null && mimeType.toLowerCase().startsWith("video/");
            if (isVideo) {
                contentParts.add(Map.of("type", "video_url", "video_url", Map.of("url", mediaUrl)));
            } else {
                contentParts.add(Map.of("type", "image_url", "image_url", Map.of("url", mediaUrl)));
            }
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", (Object) contentParts));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);
        adapter.applyNonStreamingParams(body, config);

        try {
            JsonNode response = aiChatClient.chat(config.getBaseUrl(), config.getApiKey(), body);
            return response.at("/choices/0/message/content").asText("");
        } catch (IOException e) {
            throw new RuntimeException("AI 媒体分析失败: " + e.getMessage(), e);
        }
    }

    // ============================================================
    //  内部辅助方法
    // ============================================================

    /**
     * 根据历史消息和当前输入构建原始 messages（Map 结构，直接序列化为 JSON）。
     * 包级可见以便单元测试。
     */
    List<Map<String, Object>> buildRawMessages(AiModelConfig config, List<AiMessage> history,
                                               String currentContent,
                                               List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments) {
        List<Map<String, Object>> messages = new ArrayList<>();

        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
        }

        int skipLast = 1;
        List<AiMessage> contextHistory = history.size() > skipLast
                ? history.subList(0, history.size() - skipLast)
                : List.of();

        for (AiMessage msg : contextHistory) {
            if ("user".equals(msg.getRole())) {
                messages.add(Map.of("role", "user",
                        "content", msg.getContent() != null ? msg.getContent() : ""));
            } else if ("assistant".equals(msg.getRole())) {
                Map<String, Object> assistantMsg = new LinkedHashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", msg.getContent() != null ? msg.getContent() : "");
                messages.add(assistantMsg);
            }
        }

        if (resolvedAttachments != null && !resolvedAttachments.isEmpty()) {
            List<Map<String, Object>> parts = new ArrayList<>();
            if (StringUtils.hasText(currentContent)) {
                parts.add(Map.of("type", "text", "text", currentContent));
            }
            for (AiAttachmentResolver.ResolvedAttachment att : resolvedAttachments) {
                switch (att.getType()) {
                    case "image" -> parts.add(Map.of("type", "image_url",
                            "image_url", Map.of("url", att.getDataUrl())));
                    case "video" -> parts.add(Map.of("type", "video_url",
                            "video_url", Map.of("url", att.getDataUrl())));
                    case "audio" -> {
                        Map<String, Object> audioData = new LinkedHashMap<>();
                        audioData.put("data", att.getDataUrl());
                        if (att.getFormat() != null) audioData.put("format", att.getFormat());
                        parts.add(Map.of("type", "input_audio", "input_audio", audioData));
                    }
                }
            }
            messages.add(Map.of("role", "user", "content", (Object) parts));
        } else {
            messages.add(Map.of("role", "user",
                    "content", currentContent != null ? currentContent : ""));
        }

        return messages;
    }

    private String attachmentsToJson(List<AiAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(attachments);
        } catch (Exception e) {
            log.warn("AI: 序列化附件失败", e);
            return null;
        }
    }

    private List<AiAttachment> parseAiAttachmentsJson(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<AiAttachment>>() {
            });
        } catch (Exception e) {
            log.warn("解析附件 JSON 失败: {}", json, e);
            return null;
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.debug("AI SSE 发送事件失败: event={}", eventName);
        }
    }

    private void sendSseError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name(AiSseEvent.ERROR).data(Map.of("message", message)));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }

    // ============================================================
    //  FIM (Fill-In-Middle) 续写（Beta 端点）
    // ============================================================

    /**
     * FIM 文本续写（同步阻塞）
     * <p>
     * 调用 DeepSeek Beta {@code /completions} 端点。提供 suffix 时使用 FIM 填充模式，
     * 不提供 suffix 时退化为纯续写（模型在 prompt 后续写）。
     *
     * @param configKey   模型配置 key（必须是支持 FIM 的提供商，如 DeepSeek）
     * @param prompt      前缀文本（必填）
     * @param suffix      后缀文本（可选）
     * @param maxTokens   最大生成 Token 数（null 时：优先使用模型配置 maxTokens，否则默认 1024）
     * @param temperature 温度覆盖（null 时使用模型配置）
     * @return FIM 续写响应
     */
    public cn.projectan.strix.model.response.system.ai.AiFimResp fim(
            String configKey, String prompt, String suffix,
            Integer maxTokens, java.math.BigDecimal temperature) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        AiProviderAdapter adapter = providerRegistry.getAdapter(config);
        org.springframework.util.Assert.isTrue(adapter.supportsFim(),
                "所选模型（" + configKey + "）不支持 FIM 续写功能，请选择 DeepSeek 提供商的模型");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("prompt", prompt);
        if (StringUtils.hasText(suffix)) {
            body.put("suffix", suffix);
        }
        // maxTokens 优先级：请求参数 > 模型配置 > 默认 1024
        int resolvedMaxTokens = maxTokens != null ? maxTokens
                : (config.getMaxTokens() != null ? config.getMaxTokens() : 1024);
        body.put("max_tokens", resolvedMaxTokens);
        // temperature 优先级：请求参数 > 模型配置
        java.math.BigDecimal resolvedTemp = temperature != null ? temperature : config.getTemperature();
        if (resolvedTemp != null) {
            body.put("temperature", resolvedTemp.doubleValue());
        }

        try {
            JsonNode response = aiChatClient.fim(config.getBaseUrl(), config.getApiKey(), body);
            cn.projectan.strix.model.response.system.ai.AiFimResp resp =
                    new cn.projectan.strix.model.response.system.ai.AiFimResp();
            resp.setText(response.at("/choices/0/text").asText(""));
            String finishReason = response.at("/choices/0/finish_reason").asText(null);
            resp.setFinishReason("null".equals(finishReason) ? null : finishReason);
            JsonNode usageNode = response.get("usage");
            if (usageNode != null && !usageNode.isNull()) {
                int pt = usageNode.path("prompt_tokens").asInt(-1);
                int ct = usageNode.path("completion_tokens").asInt(-1);
                resp.setPromptTokens(pt >= 0 ? pt : null);
                resp.setCompletionTokens(ct >= 0 ? ct : null);
            }
            return resp;
        } catch (IOException e) {
            throw new RuntimeException("AI FIM 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * FIM / Chat Prefix 流式续写（SSE）
     * <p>
     * 根据 {@code chatPrefix} 参数选择模式：
     * <ul>
     *   <li><b>FIM 模式</b>（chatPrefix=false/null）：调用 DeepSeek Beta {@code /completions}</li>
     *   <li><b>对话前缀续写</b>（chatPrefix=true）：调用 DeepSeek Beta {@code /chat/completions}，
     *       构造 messages 数组，最后一条 assistant 消息带 {@code "prefix":true}</li>
     * </ul>
     * SSE 事件格式：
     * <ul>
     *   <li>{@code event: content}  {@code data: {"content":"..."}} — 每个文本 chunk</li>
     *   <li>{@code event: done}     {@code data: {"promptTokens":n,"completionTokens":n}} — 完成</li>
     *   <li>{@code event: error}    {@code data: {"message":"..."}} — 出错</li>
     * </ul>
     */
    public void streamFim(String configKey, String prompt, String suffix,
                          String systemPrompt, String userContent, Boolean chatPrefix,
                          Integer maxTokens, java.math.BigDecimal temperature,
                          SseEmitter emitter) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        AiProviderAdapter adapter = providerRegistry.getAdapter(config);
        org.springframework.util.Assert.isTrue(adapter.supportsFim(),
                "所选模型（" + configKey + "）不支持 FIM 续写功能，请选择 DeepSeek 提供商的模型");

        int resolvedMaxTokens = maxTokens != null ? maxTokens
                : (config.getMaxTokens() != null ? config.getMaxTokens() : 1024);
        java.math.BigDecimal resolvedTemp = temperature != null ? temperature : config.getTemperature();

        int[] promptTokensRef = {-1};
        int[] completionTokensRef = {-1};

        try {
            if (Boolean.TRUE.equals(chatPrefix)) {
                streamChatPrefixInternal(config, prompt, suffix, systemPrompt, userContent,
                        resolvedMaxTokens, resolvedTemp, promptTokensRef, completionTokensRef, emitter);
            } else {
                streamFimInternal(config, prompt, suffix, resolvedMaxTokens, resolvedTemp,
                        promptTokensRef, completionTokensRef, emitter);
            }

            Map<String, Object> doneData = new HashMap<>();
            if (promptTokensRef[0] >= 0) doneData.put("promptTokens", promptTokensRef[0]);
            if (completionTokensRef[0] >= 0) doneData.put("completionTokens", completionTokensRef[0]);
            sendSseEvent(emitter, AiSseEvent.DONE, doneData);
            emitter.complete();
        } catch (Exception e) {
            log.error("AI FIM 流式调用出错: configKey={}", configKey, e);
            sendSseError(emitter, "AI 续写调用出错: " + e.getMessage());
        }
    }

    /**
     * FIM Beta /completions 模式
     */
    private void streamFimInternal(AiModelConfig config, String prompt, String suffix,
                                   int maxTokens, java.math.BigDecimal temperature,
                                   int[] promptTokensRef, int[] completionTokensRef,
                                   SseEmitter emitter) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("prompt", prompt);
        if (StringUtils.hasText(suffix)) body.put("suffix", suffix);
        body.put("max_tokens", maxTokens);
        if (temperature != null) body.put("temperature", temperature.doubleValue());

        aiChatClient.streamFim(config.getBaseUrl(), config.getApiKey(), body, chunk -> {
            extractFimUsage(chunk, promptTokensRef, completionTokensRef);
            JsonNode choices = chunk.get("choices");
            if (choices == null || choices.isEmpty()) return;
            JsonNode textNode = choices.get(0).get("text");
            if (textNode != null && !textNode.isNull()) {
                String text = textNode.asText("");
                if (!text.isEmpty()) sendSseEvent(emitter, AiSseEvent.CONTENT, Map.of("content", text));
            }
        });
    }

    /**
     * Chat Prefix Beta /chat/completions 模式
     */
    private void streamChatPrefixInternal(AiModelConfig config, String assistantPrefix, String suffix,
                                          String systemPrompt, String userContent,
                                          int maxTokens, java.math.BigDecimal temperature,
                                          int[] promptTokensRef, int[] completionTokensRef,
                                          SseEmitter emitter) throws Exception {
        // 构造 messages 数组：system（可选）→ user（可选）→ assistant prefix（必须最后且含 prefix:true）
        List<Map<String, Object>> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        if (StringUtils.hasText(userContent)) {
            messages.add(Map.of("role", "user", "content", userContent));
        }
        Map<String, Object> assistantMsg = new LinkedHashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", assistantPrefix);
        assistantMsg.put("prefix", true); // DeepSeek Chat Prefix Completion 关键字段
        messages.add(assistantMsg);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);
        body.put("max_completion_tokens", maxTokens);
        if (temperature != null) body.put("temperature", temperature.doubleValue());

        // Chat Prefix 使用 /beta/chat/completions，baseUrl 需含 /beta
        String betaBaseUrl = ensureBetaBaseUrl(config.getBaseUrl());

        aiChatClient.streamChat(betaBaseUrl, config.getApiKey(), body, chunk -> {
            extractChatUsage(chunk, promptTokensRef, completionTokensRef);
            JsonNode choices = chunk.get("choices");
            if (choices == null || choices.isEmpty()) return;
            JsonNode delta = choices.get(0).get("delta");
            if (delta == null) return;
            JsonNode contentNode = delta.get("content");
            if (contentNode != null && !contentNode.isNull()) {
                String text = contentNode.asText("");
                if (!text.isEmpty()) sendSseEvent(emitter, AiSseEvent.CONTENT, Map.of("content", text));
            }
        });
    }

    /**
     * 确保 baseUrl 含有 /beta 路径（Chat Prefix / FIM 所需）
     */
    private String ensureBetaBaseUrl(String baseUrl) {
        String url = baseUrl.replaceAll("/+$", "");
        return url.endsWith("/beta") ? url : url + "/beta";
    }

    private void extractFimUsage(JsonNode chunk, int[] promptRef, int[] completionRef) {
        JsonNode usage = chunk.get("usage");
        if (usage != null && !usage.isNull()) {
            int pt = usage.path("prompt_tokens").asInt(-1);
            int ct = usage.path("completion_tokens").asInt(-1);
            if (pt >= 0) promptRef[0] = pt;
            if (ct >= 0) completionRef[0] = ct;
        }
    }

    private void extractChatUsage(JsonNode chunk, int[] promptRef, int[] completionRef) {
        JsonNode usage = chunk.get("usage");
        if (usage != null && !usage.isNull()) {
            int pt = usage.path("prompt_tokens").asInt(-1);
            int ct = usage.path("completion_tokens").asInt(-1);
            if (pt >= 0) promptRef[0] = pt;
            if (ct >= 0) completionRef[0] = ct;
        }
    }
}