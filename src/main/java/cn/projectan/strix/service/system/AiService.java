package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.ai.AiAttachmentResolver;
import cn.projectan.strix.core.module.ai.AiModelStore;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiSession;
import cn.projectan.strix.model.dict.system.AiMessageStatus;
import cn.projectan.strix.model.request.system.module.ai.AiAttachment;
import cn.projectan.strix.model.response.system.ai.AiSseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * AI 核心服务
 * <p>
 * 提供两种调用方式：
 * <ul>
 *   <li>在线对话（SSE 流式）：{@link #streamChat}</li>
 *   <li>程序化调用（同步阻塞）：{@link #chat}, {@link #analyzeMedia}</li>
 * </ul>
 * <p>
 * 在线对话相关能力在本类实现；TTS/STT/图片生成等 DashScope 原生 API（非 OpenAI 兼容端点）
 * 由 {@link DashScopeAiService} 实现。
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * 专用于多模态流式调用的 OkHttp 客户端（绕过 OpenAI SDK 序列化限制）
     * <p>连接超时 30s，读取超时 5min（长流式响应）
     */
    private static final OkHttpClient RAW_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(5))
            .writeTimeout(Duration.ofSeconds(30))
            .build();

    private final AiAttachmentResolver aiAttachmentResolver;
    private final AiModelStore aiModelStore;
    private final AiModelConfigService aiModelConfigService;
    private final AiSessionService aiSessionService;
    private final AiMessageService aiMessageService;

    // ============================================================
    //  流式在线对话（SSE）
    // ============================================================

    /**
     * 流式 AI 对话，将结果推送到 {@link SseEmitter}
     * <p>
     * 应在虚拟线程中调用。
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
        List<Message> messages = buildMessages(config, history, content, resolvedAttachments);

        // 6. 执行流式调用：多模态时使用 OkHttp 直连（绕过 SDK 序列化），否则走 SDK
        if (resolvedAttachments != null && !resolvedAttachments.isEmpty()) {
            runStreamingRaw(config, messages, resolvedAttachments, assistantMsg.getId(), userMsg.getId(), emitter, startTime, sessionId);
        } else {
            runStreaming(config, messages, assistantMsg.getId(), userMsg.getId(), emitter, startTime, sessionId);
        }
    }

    /**
     * 重新生成最后一条 AI 回复（SSE 流式）
     * <p>
     * 删除当前会话最后一条 assistant 消息，找到最后一条 user 消息内容，重新触发流式生成。
     * 应在虚拟线程中调用。
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

        // 6. 加载上下文 → 构建 messages → 流式调用：多模态时走 OkHttp，否则走 SDK
        List<AiMessage> history = aiMessageService.listContextMessages(sessionId);
        List<Message> messages = buildMessages(config, history, lastUser.getContent(), resolvedAttachments);
        if (resolvedAttachments != null && !resolvedAttachments.isEmpty()) {
            runStreamingRaw(config, messages, resolvedAttachments, assistantMsg.getId(), null, emitter, startTime, sessionId);
        } else {
            runStreaming(config, messages, assistantMsg.getId(), null, emitter, startTime, sessionId);
        }
    }

    /**
     * 执行底层流式调用（SDK 方式，纯文本）：消费 OpenAI SDK 流、推送 thinking/content 事件、落库并发送 DONE/ERROR 事件。
     * <p>由 {@link #streamChat} 与 {@link #streamRegenerate} 在无多模态附件时调用。应在虚拟线程中调用。
     *
     * @param userMsgId 用户消息 ID，仅 streamChat 需要在 DONE 事件回传；regenerate 传 {@code null}
     */
    private void runStreaming(AiModelConfig config, List<Message> messages,
                              String assistantMsgId, String userMsgId,
                              SseEmitter emitter, long startTime, String sessionId) {
        // 构建 SDK 请求参数（纯文本，无附件）
        ChatCompletionCreateParams params = buildSdkParams(config, messages, true, null);
        try {
            OpenAIClient syncClient = aiModelStore.getSyncClient(config);

            StringBuilder fullContent = new StringBuilder();
            StringBuilder thinkingContent = new StringBuilder();
            int[] promptTokens = {-1};
            int[] completionTokens = {-1};
            int[] cacheHitTokens = {-1};
            int[] cacheWriteTokens = {-1};
            int[] reasoningTokens = {-1};

            try (var stream = syncClient.chat().completions().createStreaming(params)) {
                stream.stream().forEach(chunk -> {
                    // 提取 token 使用情况（通常在最后一个 chunk）
                    chunk.usage().ifPresent(usage -> {
                        promptTokens[0] = (int) usage.promptTokens();
                        completionTokens[0] = (int) usage.completionTokens();

                        // 缓存命中 Token（prompt_tokens_details.cached_tokens）
                        // 缓存写入 Token（cache_write_tokens，DashScope 顶层字段）
                        // 思考链 Token（completion_tokens_details.reasoning_tokens）
                        // 通过 Jackson 序列化 _additionalProperties() Map 提取，避免 JsonValue API 版本差异
                        extractSdkUsageTokens(usage._additionalProperties(),
                                cacheHitTokens, cacheWriteTokens, reasoningTokens);
                    });

                    if (chunk.choices().isEmpty()) return;

                    ChatCompletionChunk.Choice choice = chunk.choices().get(0);
                    String delta = choice.delta().content().orElse(null);

                    // 提取思考内容 reasoning_content（百炼 qwen3 思考模式非标准字段）
                    String thinkingDelta = null;
                    JsonValue reasoningValue = choice.delta()._additionalProperties().get("reasoning_content");
                    if (reasoningValue != null) {
                        Object val = reasoningValue.asString().orElse(null);
                        thinkingDelta = val instanceof String s ? s : null;
                    }

                    if (StringUtils.hasText(thinkingDelta)) {
                        thinkingContent.append(thinkingDelta);
                        sendSseEvent(emitter, AiSseEvent.THINKING, Map.of("content", thinkingDelta));
                    }
                    if (StringUtils.hasText(delta)) {
                        fullContent.append(delta);
                        sendSseEvent(emitter, AiSseEvent.CONTENT, Map.of("content", delta));
                    }
                });
            }

            Integer finalPromptTokens = promptTokens[0] >= 0 ? promptTokens[0] : null;
            Integer finalCompletionTokens = completionTokens[0] >= 0 ? completionTokens[0] : null;
            Integer finalCacheHitTokens = cacheHitTokens[0] >= 0 ? cacheHitTokens[0] : null;
            Integer finalCacheWriteTokens = cacheWriteTokens[0] >= 0 ? cacheWriteTokens[0] : null;
            Integer finalReasoningTokens = reasoningTokens[0] >= 0 ? reasoningTokens[0] : null;
            Long durationMs = System.currentTimeMillis() - startTime;

            // 更新 assistant 消息为完成状态
            aiMessageService.markCompleted(assistantMsgId,
                    fullContent.toString(),
                    thinkingContent.isEmpty() ? null : thinkingContent.toString(),
                    finalPromptTokens, finalCompletionTokens,
                    finalCacheHitTokens, finalCacheWriteTokens, finalReasoningTokens,
                    config.getId(), durationMs);

            // 发送完成事件
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("messageId", assistantMsgId);
            if (userMsgId != null) doneData.put("userMessageId", userMsgId);
            doneData.put("modelConfigId", config.getId());
            doneData.put("modelConfigName", config.getName());
            if (finalPromptTokens != null) doneData.put("promptTokens", finalPromptTokens);
            if (finalCompletionTokens != null) doneData.put("completionTokens", finalCompletionTokens);
            if (finalCacheHitTokens != null) doneData.put("cacheHitTokens", finalCacheHitTokens);
            if (finalCacheWriteTokens != null) doneData.put("cacheWriteTokens", finalCacheWriteTokens);
            if (finalReasoningTokens != null) doneData.put("reasoningTokens", finalReasoningTokens);
            sendSseEvent(emitter, AiSseEvent.DONE, doneData);
            emitter.complete();

        } catch (Exception e) {
            log.error("AI 流式调用出错: sessionId={}", sessionId, e);
            aiMessageService.markError(assistantMsgId, e.getMessage());
            sendSseError(emitter, "AI 调用出错: " + e.getMessage());
        }
    }

    /**
     * 执行底层流式调用（OkHttp 直连方式，多模态）：手动构建 JSON body、解析 SSE 响应、推送事件。
     * <p>绕过 OpenAI Java SDK 的 {@code ChatCompletionContentPart} 序列化限制，
     * 直接以 DashScope 接受的原始 JSON 格式发送多模态 messages。
     * <p>由 {@link #streamChat} 与 {@link #streamRegenerate} 在有附件时调用。应在虚拟线程中调用。
     *
     * @param userMsgId 用户消息 ID，仅 streamChat 需要在 DONE 事件回传；regenerate 传 {@code null}
     */
    private void runStreamingRaw(AiModelConfig config, List<Message> messages,
                                 List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments,
                                 String assistantMsgId, String userMsgId,
                                 SseEmitter emitter, long startTime, String sessionId) {
        try {
            // 构建请求体
            Map<String, Object> body = buildRawRequestBody(config, messages, resolvedAttachments);
            String jsonBody = OBJECT_MAPPER.writeValueAsString(body);

            // 构建 HTTP 请求
            String url = config.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();

            StringBuilder fullContent = new StringBuilder();
            StringBuilder thinkingContent = new StringBuilder();
            int[] promptTokens = {-1};
            int[] completionTokens = {-1};
            int[] cacheHitTokens = {-1};
            int[] cacheWriteTokens = {-1};
            int[] reasoningTokens = {-1};

            try (Response response = RAW_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    String errorMsg = response.body() != null ? response.body().string() : "HTTP " + response.code();
                    throw new IOException("DashScope API 返回错误: " + response.code() + " - " + errorMsg);
                }

                // 逐行读取 SSE 流
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;
                        if (!line.startsWith("data: ")) continue;

                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) break;

                        JsonNode chunk = OBJECT_MAPPER.readTree(data);
                        processRawSseChunk(chunk, fullContent, thinkingContent,
                                promptTokens, completionTokens,
                                cacheHitTokens, cacheWriteTokens, reasoningTokens, emitter);
                    }
                }
            }

            Integer finalPromptTokens = promptTokens[0] >= 0 ? promptTokens[0] : null;
            Integer finalCompletionTokens = completionTokens[0] >= 0 ? completionTokens[0] : null;
            Integer finalCacheHitTokens = cacheHitTokens[0] >= 0 ? cacheHitTokens[0] : null;
            Integer finalCacheWriteTokens = cacheWriteTokens[0] >= 0 ? cacheWriteTokens[0] : null;
            Integer finalReasoningTokens = reasoningTokens[0] >= 0 ? reasoningTokens[0] : null;
            Long durationMs = System.currentTimeMillis() - startTime;

            // 更新 assistant 消息为完成状态
            aiMessageService.markCompleted(assistantMsgId,
                    fullContent.toString(),
                    thinkingContent.isEmpty() ? null : thinkingContent.toString(),
                    finalPromptTokens, finalCompletionTokens,
                    finalCacheHitTokens, finalCacheWriteTokens, finalReasoningTokens,
                    config.getId(), durationMs);

            // 发送完成事件
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("messageId", assistantMsgId);
            if (userMsgId != null) doneData.put("userMessageId", userMsgId);
            doneData.put("modelConfigId", config.getId());
            doneData.put("modelConfigName", config.getName());
            if (finalPromptTokens != null) doneData.put("promptTokens", finalPromptTokens);
            if (finalCompletionTokens != null) doneData.put("completionTokens", finalCompletionTokens);
            if (finalCacheHitTokens != null) doneData.put("cacheHitTokens", finalCacheHitTokens);
            if (finalCacheWriteTokens != null) doneData.put("cacheWriteTokens", finalCacheWriteTokens);
            if (finalReasoningTokens != null) doneData.put("reasoningTokens", finalReasoningTokens);
            sendSseEvent(emitter, AiSseEvent.DONE, doneData);
            emitter.complete();

        } catch (Exception e) {
            log.error("AI 多模态流式调用出错: sessionId={}", sessionId, e);
            aiMessageService.markError(assistantMsgId, e.getMessage());
            sendSseError(emitter, "AI 调用出错: " + e.getMessage());
        }
    }

    /**
     * 处理 OkHttp 直连 SSE 流的单个 chunk JSON
     */
    private void processRawSseChunk(JsonNode chunk, StringBuilder fullContent, StringBuilder thinkingContent,
                                    int[] promptTokens, int[] completionTokens,
                                    int[] cacheHitTokens, int[] cacheWriteTokens, int[] reasoningTokens,
                                    SseEmitter emitter) {
        // 提取 usage（通常在最后一个 chunk）
        JsonNode usageNode = chunk.get("usage");
        if (usageNode != null && !usageNode.isNull()) {
            if (usageNode.has("prompt_tokens")) {
                promptTokens[0] = usageNode.get("prompt_tokens").asInt();
            }
            if (usageNode.has("completion_tokens")) {
                completionTokens[0] = usageNode.get("completion_tokens").asInt();
            }
            // 缓存命中 Token（prompt_tokens_details.cached_tokens）
            JsonNode promptDetails = usageNode.get("prompt_tokens_details");
            if (promptDetails != null && promptDetails.has("cached_tokens")) {
                cacheHitTokens[0] = promptDetails.get("cached_tokens").asInt();
            }
            // 缓存写入 Token（prompt_tokens_details.cache_creation_input_tokens）
            if (promptDetails != null && promptDetails.has("cache_creation_input_tokens")) {
                cacheWriteTokens[0] = promptDetails.get("cache_creation_input_tokens").asInt();
            }
            // 思考链 Token（completion_tokens_details.reasoning_tokens）
            JsonNode completionDetails = usageNode.get("completion_tokens_details");
            if (completionDetails != null && completionDetails.has("reasoning_tokens")) {
                reasoningTokens[0] = completionDetails.get("reasoning_tokens").asInt();
            }
        }

        JsonNode choices = chunk.get("choices");
        if (choices == null || choices.isEmpty()) return;

        JsonNode choice = choices.get(0);
        JsonNode delta = choice.get("delta");
        if (delta == null) return;

        // 提取思考内容 reasoning_content
        JsonNode reasoningNode = delta.get("reasoning_content");
        if (reasoningNode != null && !reasoningNode.isNull()) {
            String thinkingDelta = reasoningNode.asText("");
            if (!thinkingDelta.isEmpty()) {
                thinkingContent.append(thinkingDelta);
                sendSseEvent(emitter, AiSseEvent.THINKING, Map.of("content", thinkingDelta));
            }
        }

        // 提取正文内容 content
        JsonNode contentNode = delta.get("content");
        if (contentNode != null && !contentNode.isNull()) {
            String contentDelta = contentNode.asText("");
            if (!contentDelta.isEmpty()) {
                fullContent.append(contentDelta);
                sendSseEvent(emitter, AiSseEvent.CONTENT, Map.of("content", contentDelta));
            }
        }
    }

    /**
     * 构建 OkHttp 直连调用的原始请求体（包含 DashScope 标准 + 非标准参数）
     */
    private Map<String, Object> buildRawRequestBody(AiModelConfig config, List<Message> messages,
                                                    List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", toRawMessages(messages, resolvedAttachments));
        body.put("stream", true);
        body.put("stream_options", Map.of("include_usage", true));

        // === 标准 OpenAI 参数 ===
        if (config.getTemperature() != null) body.put("temperature", config.getTemperature().doubleValue());
        if (config.getTopP() != null) body.put("top_p", config.getTopP().doubleValue());
        if (config.getMaxCompletionTokens() != null) {
            body.put("max_completion_tokens", config.getMaxCompletionTokens());
        } else if (config.getMaxTokens() != null) {
            body.put("max_completion_tokens", config.getMaxTokens());
        }
        if (config.getPresencePenalty() != null)
            body.put("presence_penalty", config.getPresencePenalty().doubleValue());
        if (config.getFrequencyPenalty() != null)
            body.put("frequency_penalty", config.getFrequencyPenalty().doubleValue());
        if (config.getSeed() != null) body.put("seed", config.getSeed());
        if (config.getN() != null) body.put("n", config.getN().intValue());
        // stop sequences
        if (StringUtils.hasText(config.getStopSequences())) {
            try {
                List<String> stops = OBJECT_MAPPER.readValue(config.getStopSequences(), new TypeReference<List<String>>() {
                });
                if (!stops.isEmpty()) body.put("stop", stops);
            } catch (Exception e) {
                log.warn("AI: 解析 stopSequences 失败: {}", config.getStopSequences(), e);
            }
        }
        // response_format
        if ("json_object".equals(config.getResponseFormat())) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        // === DashScope 非标准参数 ===

        // 思考模式
        if (config.getEnableThinking() != null && config.getEnableThinking() == 1) {
            body.put("enable_thinking", true);
            if (config.getThinkingBudget() != null) body.put("thinking_budget", config.getThinkingBudget());
            if (config.getPreserveThinking() != null && config.getPreserveThinking() == 1) {
                body.put("preserve_thinking", true);
            }
            // 代码解释器（流式模式 + 思考模式）
            if (config.getEnableCodeInterpreter() != null && config.getEnableCodeInterpreter() == 1) {
                body.put("enable_code_interpreter", true);
            }
        }

        // 推理力度
        if (StringUtils.hasText(config.getReasoningEffort())) {
            body.put("reasoning_effort", config.getReasoningEffort());
        }

        // 图文混合输出
        if (config.getEnableTextImageMixed() != null && config.getEnableTextImageMixed() == 1) {
            body.put("enable_text_image_mixed", true);
        }

        // 高分辨率图像
        if (config.getVlHighResolutionImages() != null && config.getVlHighResolutionImages() == 1) {
            body.put("vl_high_resolution_images", true);
        } else {
            // 未启用高分辨率时，像素阈值和视频帧率生效
            if (config.getMinPixels() != null) body.put("min_pixels", config.getMinPixels());
            if (config.getMaxPixels() != null) body.put("max_pixels", config.getMaxPixels());
        }
        if (config.getVideoFps() != null) body.put("fps", config.getVideoFps().doubleValue());

        // top_k
        if (config.getTopK() != null) body.put("top_k", config.getTopK());

        // repetition_penalty
        if (config.getRepetitionPenalty() != null) {
            body.put("repetition_penalty", config.getRepetitionPenalty().doubleValue());
        }

        // 联网搜索
        if (config.getEnableSearch() != null && config.getEnableSearch() == 1) {
            body.put("enable_search", true);
            Map<String, Object> searchOptions = new HashMap<>();
            if (StringUtils.hasText(config.getSearchStrategy())) {
                searchOptions.put("search_strategy", config.getSearchStrategy());
            }
            if (config.getForcedSearch() != null && config.getForcedSearch() == 1) {
                searchOptions.put("forced_search", true);
            }
            if (config.getEnableSource() != null && config.getEnableSource() == 1) {
                searchOptions.put("enable_source", true);
            }
            if (config.getSearchFreshness() != null) {
                searchOptions.put("freshness", config.getSearchFreshness());
            }
            if (config.getEnableSearchExtension() != null && config.getEnableSearchExtension() == 1) {
                searchOptions.put("enable_search_extension", true);
            }
            if (!searchOptions.isEmpty()) body.put("search_options", searchOptions);
        }

        return body;
    }

    // ============================================================
    //  程序化调用 - 文本/视觉对话（同步阻塞）
    // ============================================================

    /**
     * 同步文本对话（程序化调用）
     *
     * @param configKey 模型配置 key
     * @param messages  消息列表
     * @return AI 响应文本
     */
    public String chat(String configKey, List<Message> messages) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        OpenAiChatModel chatModel = aiModelStore.getChatModel(config);
        OpenAiChatOptions options = buildChatOptions(config, false);
        ChatResponse response = chatModel.call(new Prompt(messages, options));
        return response.getResult().getOutput().getText();
    }

    /**
     * 同步文本对话（简单单轮，程序化调用）
     *
     * @param configKey 模型配置 key
     * @param userInput 用户输入
     * @return AI 响应文本
     */
    public String chat(String configKey, String userInput) {
        List<Message> messages = new ArrayList<>();
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(new SystemMessage(config.getSystemPrompt()));
        }
        messages.add(new UserMessage(userInput));
        return chat(configKey, messages);
    }

    /**
     * 同步流式对话（程序化调用，返回 Flux）
     *
     * @param configKey 模型配置 key
     * @param messages  消息列表
     * @return 响应流
     */
    public Flux<ChatResponse> chatStream(String configKey, List<Message> messages) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        OpenAiChatModel chatModel = aiModelStore.getChatModel(config);
        OpenAiChatOptions options = buildChatOptions(config, true);
        return chatModel.stream(new Prompt(messages, options));
    }

    /**
     * 视觉模型分析图片/视频（程序化调用）
     *
     * @param configKey 模型配置 key
     * @param prompt    文本提示
     * @param mediaUrls 媒体 URL 列表（图片或视频）
     * @param mimeTypes 对应的 MIME 类型（如 "image/jpeg", "video/mp4"），与 mediaUrls 一一对应
     * @return AI 分析结果文本
     */
    public String analyzeMedia(String configKey, String prompt, List<String> mediaUrls, List<String> mimeTypes) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        List<Media> mediaList = new ArrayList<>();
        for (int i = 0; i < mediaUrls.size(); i++) {
            String mime = (mimeTypes != null && i < mimeTypes.size()) ? mimeTypes.get(i) : "image/jpeg";
            mediaList.add(new Media(MimeTypeUtils.parseMimeType(mime), URI.create(mediaUrls.get(i))));
        }
        UserMessage userMessage = UserMessage.builder().text(prompt).media(mediaList).build();
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(new SystemMessage(config.getSystemPrompt()));
        }
        messages.add(userMessage);
        return chat(configKey, messages);
    }

    // ============================================================
    //  内部辅助方法
    // ============================================================

    /**
     * 根据历史消息和当前输入构建 Spring AI Message 列表
     * <p>包级可见以便单元测试（{@code AiServiceContextTest}）直接验证上下文截断逻辑。
     */
    List<Message> buildMessages(AiModelConfig config, List<AiMessage> history,
                                String currentContent, List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments) {
        List<Message> messages = new ArrayList<>();

        // 添加 system prompt
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(new SystemMessage(config.getSystemPrompt()));
        }

        // 添加历史消息（排除当前刚保存的用户消息——它会作为本轮输入单独追加）
        // 注意：占位的 assistant 消息（GENERATING）已被 listContextMessages 过滤，因此这里只需跳过最后 1 条（user）
        int skipLast = 1;
        List<AiMessage> contextHistory = history.size() > skipLast
                ? history.subList(0, history.size() - skipLast)
                : List.of();

        for (AiMessage msg : contextHistory) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent() != null ? msg.getContent() : ""));
            } else if ("assistant".equals(msg.getRole())) {
                Map<String, Object> metadata = new HashMap<>();
                if (StringUtils.hasText(msg.getThinkingContent())) {
                    metadata.put("reasoning_content", msg.getThinkingContent());
                }
                messages.add(AssistantMessage.builder()
                        .content(msg.getContent() != null ? msg.getContent() : "")
                        .properties(metadata)
                        .build());
            }
        }

        // 添加当前用户消息（纯文本，多模态附件在 toSdkMessages 中处理）
        messages.add(new UserMessage(currentContent));

        return messages;
    }

    /**
     * 构建 OpenAI Chat Options
     *
     * @param config    模型配置
     * @param streaming 是否为流式调用（code_interpreter 仅在流式模式下生效）
     */
    private OpenAiChatOptions buildChatOptions(AiModelConfig config, boolean streaming) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .model(config.getModelName());

        if (config.getTemperature() != null) builder.temperature(config.getTemperature().doubleValue());
        if (config.getTopP() != null) builder.topP(config.getTopP().doubleValue());
        // 优先 maxCompletionTokens，降级到 maxTokens（与流式调用路径对齐）
        if (config.getMaxCompletionTokens() != null) {
            builder.maxTokens(config.getMaxCompletionTokens());
        } else if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }

        // 思考模式 + 代码解释器（代码解释器要求流式模式且同时开启思考）
        if ((config.getEnableThinking() != null && config.getEnableThinking() == 1)
                || (config.getEnableSearch() != null && config.getEnableSearch() == 1)) {
            Map<String, Object> extra = new HashMap<>();
            if (config.getEnableThinking() != null && config.getEnableThinking() == 1) {
                extra.put("enable_thinking", true);
                if (config.getThinkingBudget() != null) {
                    extra.put("thinking_budget", config.getThinkingBudget());
                }
                if (streaming && config.getEnableCodeInterpreter() != null && config.getEnableCodeInterpreter() == 1) {
                    extra.put("enable_code_interpreter", true);
                }
            }
            if (config.getEnableSearch() != null && config.getEnableSearch() == 1) {
                extra.put("enable_search", true);
                Map<String, Object> searchOptions = new HashMap<>();
                if (config.getSearchStrategy() != null && !config.getSearchStrategy().isBlank()) {
                    searchOptions.put("search_strategy", config.getSearchStrategy());
                }
                if (config.getEnableSource() != null && config.getEnableSource() == 1) {
                    searchOptions.put("enable_source", true);
                }
                if (!searchOptions.isEmpty()) {
                    extra.put("search_options", searchOptions);
                }
            }
            builder.extraBody(extra);
        }

        return builder.build();
    }

    /**
     * 将 Spring AI Message 列表转换为原始 Map 结构（用于多模态场景绕过 SDK 类型限制）
     */
    private List<Map<String, Object>> toRawMessages(List<Message> messages,
                                                    List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            boolean isLastUserMsg = (i == messages.size() - 1) && (msg instanceof UserMessage);

            if (msg instanceof SystemMessage) {
                result.add(Map.of("role", "system", "content", msg.getText() != null ? msg.getText() : ""));
            } else if (msg instanceof UserMessage um) {
                List<AiAttachmentResolver.ResolvedAttachment> currentAttachments =
                        isLastUserMsg ? resolvedAttachments : null;

                if (currentAttachments != null && !currentAttachments.isEmpty()) {
                    List<Map<String, Object>> contentParts = new ArrayList<>();
                    if (StringUtils.hasText(um.getText())) {
                        contentParts.add(Map.of("type", "text", "text", um.getText()));
                    }
                    for (AiAttachmentResolver.ResolvedAttachment att : currentAttachments) {
                        if ("image".equals(att.getType())) {
                            contentParts.add(Map.of("type", "image_url",
                                    "image_url", Map.of("url", att.getDataUrl())));
                        } else if ("video".equals(att.getType())) {
                            contentParts.add(Map.of("type", "video_url",
                                    "video_url", Map.of("url", att.getDataUrl())));
                        } else if ("audio".equals(att.getType())) {
                            Map<String, Object> audioData = new LinkedHashMap<>();
                            audioData.put("data", att.getDataUrl());
                            if (att.getFormat() != null) audioData.put("format", att.getFormat());
                            contentParts.add(Map.of("type", "input_audio",
                                    "input_audio", audioData));
                        }
                    }
                    result.add(Map.of("role", "user", "content", (Object) contentParts));
                } else {
                    result.add(Map.of("role", "user", "content", um.getText() != null ? um.getText() : ""));
                }
            } else if (msg instanceof AssistantMessage) {
                result.add(Map.of("role", "assistant", "content", msg.getText() != null ? msg.getText() : ""));
            }
        }
        return result;
    }

    /**
     * 将 Spring AI Message 列表转换为 OpenAI SDK 的 ChatCompletionMessageParam 列表（纯文本场景）
     */
    private List<ChatCompletionMessageParam> toSdkMessages(List<Message> messages) {
        List<ChatCompletionMessageParam> result = new ArrayList<>();
        for (Message msg : messages) {
            if (msg instanceof SystemMessage) {
                result.add(ChatCompletionMessageParam.ofSystem(
                        ChatCompletionSystemMessageParam.builder()
                                .content(msg.getText() != null ? msg.getText() : "")
                                .build()));
            } else if (msg instanceof UserMessage um) {
                result.add(ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                                .content(um.getText() != null ? um.getText() : "")
                                .build()));
            } else if (msg instanceof AssistantMessage) {
                result.add(ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder()
                                .content(msg.getText() != null ? msg.getText() : "")
                                .build()));
            }
        }
        return result;
    }

    /**
     * 构建 OpenAI SDK {@link ChatCompletionCreateParams}，包含所有标准/非标准扩展参数
     */
    private ChatCompletionCreateParams buildSdkParams(AiModelConfig config, List<Message> messages,
                                                      boolean streaming,
                                                      List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments) {
        boolean hasMultimodal = resolvedAttachments != null && !resolvedAttachments.isEmpty();

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(config.getModelName());

        if (hasMultimodal) {
            // 多模态场景：用原始 Map 构建 messages，通过 additionalProperty 覆盖 SDK 的 messages 字段
            // 先设一个占位 message 满足 builder 校验
            builder.messages(List.of(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content("placeholder").build())));
            // 用 additionalProperty 覆盖为原始 JSON 格式
            builder.putAdditionalBodyProperty("messages", JsonValue.from(toRawMessages(messages, resolvedAttachments)));
        } else {
            builder.messages(toSdkMessages(messages));
        }

        // === 标准 OpenAI 参数 ===
        if (config.getTemperature() != null) builder.temperature(config.getTemperature().doubleValue());
        if (config.getTopP() != null) builder.topP(config.getTopP().doubleValue());
        if (config.getMaxCompletionTokens() != null) {
            builder.maxCompletionTokens(config.getMaxCompletionTokens().longValue());
        } else if (config.getMaxTokens() != null) {
            builder.maxCompletionTokens(config.getMaxTokens().longValue());
        }
        if (config.getPresencePenalty() != null) builder.presencePenalty(config.getPresencePenalty().doubleValue());
        if (config.getFrequencyPenalty() != null) builder.frequencyPenalty(config.getFrequencyPenalty().doubleValue());
        if (config.getSeed() != null) builder.seed(config.getSeed());
        if (config.getN() != null) builder.n(config.getN().longValue());
        if (config.getLogprobs() != null && config.getLogprobs() == 1) {
            builder.logprobs(true);
            if (config.getTopLogprobs() != null) builder.topLogprobs(config.getTopLogprobs().longValue());
        }
        // stop sequences
        if (StringUtils.hasText(config.getStopSequences())) {
            try {
                List<String> stops = OBJECT_MAPPER.readValue(config.getStopSequences(), new TypeReference<List<String>>() {
                });
                if (!stops.isEmpty()) {
                    builder.stop(ChatCompletionCreateParams.Stop.ofStrings(stops));
                }
            } catch (Exception e) {
                log.warn("AI: 解析 stopSequences 失败: {}", config.getStopSequences(), e);
            }
        }
        // response_format (json_object)
        if ("json_object".equals(config.getResponseFormat())) {
            builder.putAdditionalBodyProperty("response_format", JsonValue.from(Map.of("type", "json_object")));
        }

        // === DashScope 非标准参数（via putAdditionalBodyProperty） ===

        // 思考模式
        if (config.getEnableThinking() != null && config.getEnableThinking() == 1) {
            builder.putAdditionalBodyProperty("enable_thinking", JsonValue.from(true));
            if (config.getThinkingBudget() != null) {
                builder.putAdditionalBodyProperty("thinking_budget", JsonValue.from(config.getThinkingBudget()));
            }
            if (config.getPreserveThinking() != null && config.getPreserveThinking() == 1) {
                builder.putAdditionalBodyProperty("preserve_thinking", JsonValue.from(true));
            }
            // 代码解释器（要求流式模式 + 思考模式）
            if (streaming && config.getEnableCodeInterpreter() != null && config.getEnableCodeInterpreter() == 1) {
                builder.putAdditionalBodyProperty("enable_code_interpreter", JsonValue.from(true));
            }
        }

        // 推理力度（DeepSeek-V4 等模型）
        if (StringUtils.hasText(config.getReasoningEffort())) {
            builder.putAdditionalBodyProperty("reasoning_effort", JsonValue.from(config.getReasoningEffort()));
        }

        // 图文混合输出
        if (config.getEnableTextImageMixed() != null && config.getEnableTextImageMixed() == 1) {
            builder.putAdditionalBodyProperty("enable_text_image_mixed", JsonValue.from(true));
        }

        // 高分辨率图像
        if (config.getVlHighResolutionImages() != null && config.getVlHighResolutionImages() == 1) {
            builder.putAdditionalBodyProperty("vl_high_resolution_images", JsonValue.from(true));
        } else {
            // 未启用高分辨率时，像素阈值和视频帧率生效
            if (config.getMinPixels() != null) {
                builder.putAdditionalBodyProperty("min_pixels", JsonValue.from(config.getMinPixels()));
            }
            if (config.getMaxPixels() != null) {
                builder.putAdditionalBodyProperty("max_pixels", JsonValue.from(config.getMaxPixels()));
            }
        }
        if (config.getVideoFps() != null) {
            builder.putAdditionalBodyProperty("fps", JsonValue.from(config.getVideoFps().doubleValue()));
        }

        // top_k
        if (config.getTopK() != null) {
            builder.putAdditionalBodyProperty("top_k", JsonValue.from(config.getTopK()));
        }

        // repetition_penalty
        if (config.getRepetitionPenalty() != null) {
            builder.putAdditionalBodyProperty("repetition_penalty", JsonValue.from(config.getRepetitionPenalty().doubleValue()));
        }

        // 联网搜索
        if (config.getEnableSearch() != null && config.getEnableSearch() == 1) {
            builder.putAdditionalBodyProperty("enable_search", JsonValue.from(true));
            Map<String, Object> searchOptions = new HashMap<>();
            if (StringUtils.hasText(config.getSearchStrategy())) {
                searchOptions.put("search_strategy", config.getSearchStrategy());
            }
            if (config.getForcedSearch() != null && config.getForcedSearch() == 1) {
                searchOptions.put("forced_search", true);
            }
            if (config.getEnableSource() != null && config.getEnableSource() == 1) {
                searchOptions.put("enable_source", true);
            }
            if (config.getSearchFreshness() != null) {
                searchOptions.put("freshness", config.getSearchFreshness());
            }
            if (config.getEnableSearchExtension() != null && config.getEnableSearchExtension() == 1) {
                searchOptions.put("enable_search_extension", true);
            }
            if (!searchOptions.isEmpty()) {
                builder.putAdditionalBodyProperty("search_options", JsonValue.from(searchOptions));
            }
        }

        return builder.build();
    }

    /**
     * 将已解析附件转换为 Spring AI Media 对象（仅处理 image 类型）
     */
    private List<Media> resolvedAttachmentsToMedia(List<AiAttachmentResolver.ResolvedAttachment> attachments) {
        List<Media> mediaList = new ArrayList<>();
        for (AiAttachmentResolver.ResolvedAttachment att : attachments) {
            if ("image".equals(att.getType()) && StringUtils.hasText(att.getDataUrl())) {
                try {
                    String mimeType = att.getMimeType() != null ? att.getMimeType() : "image/jpeg";
                    mediaList.add(new Media(MimeTypeUtils.parseMimeType(mimeType), URI.create(att.getDataUrl())));
                } catch (Exception e) {
                    log.warn("AI: 解析附件 URL 失败: {}", att.getDataUrl(), e);
                }
            }
        }
        return mediaList;
    }

    /**
     * 将 {@link AiAttachment} 列表序列化为 JSON 字符串存储
     * <p>存储原始附件引用（fileId/type/mimeType/name），不含解析后的 URL。
     */
    private String attachmentsToJson(List<AiAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(attachments);
        } catch (Exception e) {
            log.warn("AI: 序列化附件失败", e);
            return null;
        }
    }

    /**
     * 将附件 JSON 字符串解析为 {@code List<AiAttachment>}
     */
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

    /**
     * 从 OpenAI SDK usage._additionalProperties() 中提取缓存相关 Token 数（best-effort）
     * <p>通过 Jackson 序列化 Map<String, JsonValue> 再解析，避免依赖 JsonValue 的版本特定 API。
     *
     * @param extra            usage._additionalProperties() 返回值
     * @param cacheHitTokens   写入目标：缓存命中 Token（prompt_tokens_details.cached_tokens）
     * @param cacheWriteTokens 写入目标：缓存写入 Token（cache_write_tokens）
     * @param reasoningTokens  写入目标：思考链 Token（completion_tokens_details.reasoning_tokens）
     */
    private static void extractSdkUsageTokens(Map<String, JsonValue> extra,
                                              int[] cacheHitTokens, int[] cacheWriteTokens, int[] reasoningTokens) {
        if (extra == null || extra.isEmpty()) return;
        try {
            String json = OBJECT_MAPPER.writeValueAsString(extra);
            JsonNode node = OBJECT_MAPPER.readTree(json);

            JsonNode ptd = node.get("prompt_tokens_details");
            if (ptd != null && ptd.has("cached_tokens")) cacheHitTokens[0] = ptd.get("cached_tokens").asInt();

            JsonNode cwt = node.get("cache_write_tokens");
            if (cwt != null && cwt.isNumber()) cacheWriteTokens[0] = cwt.asInt();

            JsonNode ctd = node.get("completion_tokens_details");
            if (ctd != null && ctd.has("reasoning_tokens")) reasoningTokens[0] = ctd.get("reasoning_tokens").asInt();
        } catch (Exception ignored) {
            // 缓存 Token 提取为 best-effort，失败不影响主流程
        }
    }

    /**
     * 向 SSE 发送指定类型事件
     */
    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.debug("AI SSE 发送事件失败: event={}", eventName);
        }
    }

    /**
     * 向 SSE 发送错误事件并完成
     */
    private void sendSseError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name(AiSseEvent.ERROR).data(Map.of("message", message)));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }

}
