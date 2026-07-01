package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.ai.AiAttachmentResolver;
import cn.projectan.strix.core.module.ai.AiChatClient;
import cn.projectan.strix.core.module.ai.AiJson;
import cn.projectan.strix.core.module.ai.AiStreamRegistry;
import cn.projectan.strix.core.module.ai.provider.AiProviderAdapter;
import cn.projectan.strix.core.module.ai.provider.AiProviderRegistry;
import cn.projectan.strix.core.module.ai.provider.AiUsageDetail;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiSession;
import cn.projectan.strix.model.dict.system.AiMessageStatus;
import cn.projectan.strix.model.request.system.module.ai.AiAttachment;
import cn.projectan.strix.model.response.system.ai.AiSseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

    private static final ObjectMapper OBJECT_MAPPER = AiJson.mapper();

    private final AiAttachmentResolver aiAttachmentResolver;
    private final AiChatClient aiChatClient;
    private final AiProviderRegistry providerRegistry;
    private final AiModelConfigService aiModelConfigService;
    private final AiSessionService aiSessionService;
    private final AiMessageService aiMessageService;
    private final AiStreamRegistry aiStreamRegistry;

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

        // 并发保护：同一会话已有进行中的生成时拒绝（前端 streaming 互斥的服务端兜底）
        if (aiStreamRegistry.get(sessionId) != null) {
            sendSseError(emitter, "当前会话正在生成回复，请稍候");
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

        // 注册进行中的生成：生成过程与客户端连接解耦，emitter 仅作为首个订阅者
        AiStreamRegistry.ActiveGeneration generation =
                aiStreamRegistry.start(sessionId, assistantMsg.getId(), userMsg.getId(), emitter);
        if (generation == null) {
            // 极小概率：并发竞态下 putIfAbsent 落败。回滚占位消息并提示
            aiMessageService.removeById(assistantMsg.getId());
            sendSseError(emitter, "当前会话正在生成回复，请稍候");
            return;
        }

        runStreaming(config, body, adapter, generation, startTime, sessionId);
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

        // 并发保护：同一会话已有进行中的生成时拒绝
        if (aiStreamRegistry.get(sessionId) != null) {
            sendSseError(emitter, "当前会话正在生成回复，请稍候");
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

        // 注册进行中的生成：重新生成场景 userMsgId 为 null（不新建 user 消息）
        AiStreamRegistry.ActiveGeneration generation =
                aiStreamRegistry.start(sessionId, assistantMsg.getId(), null, emitter);
        if (generation == null) {
            aiMessageService.removeById(assistantMsg.getId());
            sendSseError(emitter, "当前会话正在生成回复，请稍候");
            return;
        }

        runStreaming(config, body, adapter, generation, startTime, sessionId);
    }

    /**
     * 统一流式执行（单一 OkHttp 路径，同时处理纯文本和多模态）。
     * <p>
     * 生成过程通过 {@link AiStreamRegistry.ActiveGeneration} 向所有订阅者广播增量，与客户端连接解耦：
     * 单个订阅者断开只是观众离场，上游流始终跑完并落库。仅当用户显式点击停止（{@code isStopRequested}）
     * 时才主动中止上游流并把已生成部分落库。
     */
    private void runStreaming(AiModelConfig config, Map<String, Object> body, AiProviderAdapter adapter,
                              AiStreamRegistry.ActiveGeneration generation, long startTime, String sessionId) {
        String assistantMsgId = generation.getAssistantMsgId();
        String userMsgId = generation.getUserMsgId();
        AiUsageDetail[] usageHolder = {AiUsageDetail.EMPTY};

        try {
            aiChatClient.streamChat(config.getBaseUrl(), config.getApiKey(), body, chunk -> {
                // 用户主动停止：抛出以中断上游流读取（此处不因单个客户端断开而中止）
                if (generation.isStopRequested()) {
                    throw new IOException("用户已停止生成，中止上游流");
                }

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
                    generation.appendThinking(thinkingDelta);
                }

                JsonNode contentNode = delta.get("content");
                if (contentNode != null && !contentNode.isNull()) {
                    String contentDelta = contentNode.asString("");
                    if (!contentDelta.isEmpty()) {
                        generation.appendContent(contentDelta);
                    }
                }
            });

            AiUsageDetail usage = usageHolder[0];
            Long durationMs = System.currentTimeMillis() - startTime;

            aiMessageService.markCompleted(assistantMsgId,
                    generation.currentContent(),
                    generation.currentThinking().isEmpty() ? null : generation.currentThinking(),
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
            generation.finish(AiSseEvent.DONE, doneData);

        } catch (Exception e) {
            // 用户主动停止：已消费的内容落库（标记完成），并向订阅者发 done（携带已有统计）
            if (generation.isStopRequested()) {
                log.debug("AI 流式被用户停止: sessionId={}, 已中止上游流", sessionId);
                AiUsageDetail usage = usageHolder[0];
                aiMessageService.markCompleted(assistantMsgId,
                        generation.currentContent(),
                        generation.currentThinking().isEmpty() ? null : generation.currentThinking(),
                        usage.promptTokens(), usage.completionTokens(),
                        usage.cacheHitTokens(), usage.cacheWriteTokens(), usage.reasoningTokens(),
                        config.getId(), System.currentTimeMillis() - startTime);
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
                generation.finish(AiSseEvent.DONE, doneData);
                return;
            }
            log.error("AI 流式调用出错: sessionId={}", sessionId, e);
            aiMessageService.markError(assistantMsgId, e.getMessage());
            generation.finish(AiSseEvent.ERROR, Map.of("message", "AI 调用出错: " + e.getMessage()));
        } finally {
            // 无论成功/出错/停止，都从注册表移除（终态已通过 finish 广播）
            aiStreamRegistry.remove(sessionId);
        }
    }

    /**
     * 重新挂接到会话进行中的生成（重连续播）。应在虚拟线程中调用。
     * <p>
     * 命中进行中的生成时：先回放已生成的全量快照（snapshot 事件），emitter 加入订阅列表继续接收增量；
     * 未命中（生成已完成 / 出错 / 从未开始）时：直接 complete，客户端应走历史消息接口兜底获取最终结果。
     *
     * @param sessionId 会话 ID
     * @param emitter   新的 SSE 连接
     * @param managerId 当前管理员 ID
     */
    public void attachStream(String sessionId, SseEmitter emitter, String managerId) {
        AiSession session = aiSessionService.getById(sessionId);
        if (session == null || !session.getManagerId().equals(managerId)) {
            sendSseError(emitter, "会话不存在或无权限");
            return;
        }

        AiStreamRegistry.ActiveGeneration generation = aiStreamRegistry.get(sessionId);
        if (generation == null) {
            // 无进行中的生成：直接结束，客户端走历史消息兜底
            emitter.complete();
            return;
        }
        // 断开时主动从订阅列表移除该 emitter（否则仅在下次广播时惰性清理）
        emitter.onError(e -> generation.unsubscribe(emitter));
        emitter.onTimeout(() -> generation.unsubscribe(emitter));
        emitter.onCompletion(() -> generation.unsubscribe(emitter));
        if (!generation.subscribe(emitter)) {
            // 恰好在挂接瞬间生成已结束：直接结束，客户端走历史消息兜底
            emitter.complete();
        }
        // 挂接成功后无需在此循环等待：后台生成线程会持续向订阅者广播，并在终态时 finish/complete
    }

    /**
     * 用户主动停止会话进行中的生成。
     * <p>
     * 置位停止标记，后台生成线程会在下一个上游 chunk 处中止上游流，把已生成部分落库并向订阅者广播 done。
     *
     * @param sessionId 会话 ID
     * @param managerId 当前管理员 ID
     * @return true=已请求停止；false=该会话当前无进行中的生成
     */
    public boolean stopGeneration(String sessionId, String managerId) {
        AiSession session = aiSessionService.getById(sessionId);
        if (session == null || !session.getManagerId().equals(managerId)) {
            return false;
        }
        AiStreamRegistry.ActiveGeneration generation = aiStreamRegistry.get(sessionId);
        if (generation == null) {
            return false;
        }
        generation.requestStop();
        return true;
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
            return response.at("/choices/0/message/content").asString("");
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
        messages.add(Map.of("role", "user", "content", contentParts));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);
        adapter.applyNonStreamingParams(body, config);

        try {
            JsonNode response = aiChatClient.chat(config.getBaseUrl(), config.getApiKey(), body);
            return response.at("/choices/0/message/content").asString("");
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
                // 重建历史 user 消息的多模态附件：多轮对话中后续追问需保留此前上传的图片/视频/音频，
                // 否则模型上下文丢失前几轮的媒体，追问必然答非所问。
                List<AiAttachmentResolver.ResolvedAttachment> historyAtts =
                        aiAttachmentResolver.resolve(parseAiAttachmentsJson(msg.getAttachments()));
                messages.add(buildUserMessage(msg.getContent(), historyAtts));
            } else if ("assistant".equals(msg.getRole())) {
                Map<String, Object> assistantMsg = new LinkedHashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", msg.getContent() != null ? msg.getContent() : "");
                messages.add(assistantMsg);
            }
        }

        messages.add(buildUserMessage(currentContent, resolvedAttachments));

        return messages;
    }

    /**
     * 构造单条 user 消息：无附件时为纯文本 content，有附件时为多模态 parts 数组。
     * 历史轮与当前轮共用，保证多轮多模态上下文一致。
     */
    private Map<String, Object> buildUserMessage(String content,
                                                 List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments) {
        if (resolvedAttachments == null || resolvedAttachments.isEmpty()) {
            return Map.of("role", "user", "content", content != null ? content : "");
        }
        List<Map<String, Object>> parts = new ArrayList<>();
        if (StringUtils.hasText(content)) {
            parts.add(Map.of("type", "text", "text", content));
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
        return Map.of("role", "user", "content", parts);
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
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
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
            resp.setText(response.at("/choices/0/text").asString(""));
            String finishReason = response.at("/choices/0/finish_reason").asString(null);
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
                String text = textNode.asString("");
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
                String text = contentNode.asString("");
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