package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.ai.AiModelStore;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiSession;
import cn.projectan.strix.model.dict.system.AiMessageStatus;
import cn.projectan.strix.model.dict.system.AiModelType;
import cn.projectan.strix.model.response.system.ai.AiSseEvent;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 核心服务
 * <p>
 * 提供两种调用方式：
 * <ul>
 *   <li>在线对话（SSE 流式）：{@link #streamChat}</li>
 *   <li>程序化调用（同步阻塞）：{@link #chat}, {@link #analyzeMedia}</li>
 * </ul>
 * <p>
 * TTS/STT/IMAGE_GEN 暂未实现（需 DashScope 原生 API，非 OpenAI 兼容端点）。
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

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
     * @param attachments 附件列表（视觉模型，每项格式 {type, url, mimeType}），可为 null
     * @param emitter     SSE emitter
     * @param managerId   当前管理员 ID
     */
    public void streamChat(String sessionId, String content, List<Map<String, String>> attachments,
                           SseEmitter emitter, String managerId) {
        // 1. 加载并校验会话
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

        // 2. 保存用户消息
        AiMessage userMsg = new AiMessage()
                .setSessionId(sessionId)
                .setRole("user")
                .setContent(content)
                .setAttachments(attachmentsToJson(attachments))
                .setStatus(AiMessageStatus.COMPLETED);
        aiMessageService.save(userMsg);

        // 3. 保存 assistant 占位消息（生成中）
        AiMessage assistantMsg = new AiMessage()
                .setSessionId(sessionId)
                .setRole("assistant")
                .setContent("")
                .setStatus(AiMessageStatus.GENERATING);
        aiMessageService.save(assistantMsg);

        // 4. 加载历史上下文并构建 messages
        List<AiMessage> history = aiMessageService.listContextMessages(sessionId);
        List<Message> messages = buildMessages(config, history, content, attachments);

        // 5. 构建 SDK 请求参数（直接使用同步 OkHttp 客户端，绕过 Spring AI Flux 的缓冲问题）
        ChatCompletionCreateParams params = buildSdkParams(config, messages, true);

        // 6. 执行流式调用
        try {
            OpenAIClient syncClient = aiModelStore.getSyncClient(config);

            StringBuilder fullContent = new StringBuilder();
            StringBuilder thinkingContent = new StringBuilder();
            int[] promptTokens = {-1};
            int[] completionTokens = {-1};

            try (var stream = syncClient.chat().completions().createStreaming(params)) {
                stream.stream().forEach(chunk -> {
                    // 提取 token 使用情况（通常在最后一个 chunk）
                    chunk.usage().ifPresent(usage -> {
                        promptTokens[0] = (int) usage.promptTokens();
                        completionTokens[0] = (int) usage.completionTokens();
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

            // 7. 更新 assistant 消息为完成状态
            aiMessageService.markCompleted(assistantMsg.getId(),
                    fullContent.toString(),
                    thinkingContent.isEmpty() ? null : thinkingContent.toString(),
                    finalPromptTokens, finalCompletionTokens);

            // 8. 发送完成事件
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("messageId", assistantMsg.getId());
            doneData.put("userMessageId", userMsg.getId());
            if (finalPromptTokens != null) doneData.put("promptTokens", finalPromptTokens);
            if (finalCompletionTokens != null) doneData.put("completionTokens", finalCompletionTokens);
            sendSseEvent(emitter, AiSseEvent.DONE, doneData);
            emitter.complete();

        } catch (Exception e) {
            log.error("AI 流式对话出错: sessionId={}", sessionId, e);
            aiMessageService.markError(assistantMsg.getId(), e.getMessage());
            sendSseError(emitter, "AI 调用出错: " + e.getMessage());
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

        // 2. 删除最后一条 assistant 消息
        AiMessage lastAssistant = aiMessageService.lambdaQuery()
                .eq(AiMessage::getSessionId, sessionId)
                .eq(AiMessage::getRole, "assistant")
                .orderByDesc(AiMessage::getCreatedTime)
                .last("LIMIT 1")
                .one();
        if (lastAssistant != null) {
            aiMessageService.removeById(lastAssistant.getId());
        }

        // 3. 找到最后一条 user 消息
        AiMessage lastUser = aiMessageService.lambdaQuery()
                .eq(AiMessage::getSessionId, sessionId)
                .eq(AiMessage::getRole, "user")
                .orderByDesc(AiMessage::getCreatedTime)
                .last("LIMIT 1")
                .one();
        if (lastUser == null) {
            sendSseError(emitter, "没有可以重新生成的用户消息");
            return;
        }

        // 4. 解析附件 JSON
        List<Map<String, String>> attachments = parseAttachmentsJson(lastUser.getAttachments());

        // 5. 保存新的 assistant 占位消息
        AiMessage assistantMsg = new AiMessage()
                .setSessionId(sessionId)
                .setRole("assistant")
                .setContent("")
                .setStatus(AiMessageStatus.GENERATING);
        aiMessageService.save(assistantMsg);

        // 6-8. 加载上下文 → 构建参数 → 流式调用（与 streamChat 相同逻辑）
        List<AiMessage> history = aiMessageService.listContextMessages(sessionId);
        List<Message> messages = buildMessages(config, history, lastUser.getContent(), attachments);
        ChatCompletionCreateParams params = buildSdkParams(config, messages, true);

        try {
            OpenAIClient syncClient = aiModelStore.getSyncClient(config);

            StringBuilder fullContent = new StringBuilder();
            StringBuilder thinkingContent = new StringBuilder();
            int[] promptTokens = {-1};
            int[] completionTokens = {-1};

            try (var stream = syncClient.chat().completions().createStreaming(params)) {
                stream.stream().forEach(chunk -> {
                    chunk.usage().ifPresent(usage -> {
                        promptTokens[0] = (int) usage.promptTokens();
                        completionTokens[0] = (int) usage.completionTokens();
                    });
                    if (chunk.choices().isEmpty()) return;

                    ChatCompletionChunk.Choice choice = chunk.choices().get(0);
                    String delta = choice.delta().content().orElse(null);

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

            aiMessageService.markCompleted(assistantMsg.getId(),
                    fullContent.toString(),
                    thinkingContent.isEmpty() ? null : thinkingContent.toString(),
                    finalPromptTokens, finalCompletionTokens);

            Map<String, Object> doneData = new HashMap<>();
            doneData.put("messageId", assistantMsg.getId());
            if (finalPromptTokens != null) doneData.put("promptTokens", finalPromptTokens);
            if (finalCompletionTokens != null) doneData.put("completionTokens", finalCompletionTokens);
            sendSseEvent(emitter, AiSseEvent.DONE, doneData);
            emitter.complete();

        } catch (Exception e) {
            log.error("AI 重新生成出错: sessionId={}", sessionId, e);
            aiMessageService.markError(assistantMsg.getId(), e.getMessage());
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
     */
    private List<Message> buildMessages(AiModelConfig config, List<AiMessage> history,
                                        String currentContent, List<Map<String, String>> attachments) {
        List<Message> messages = new ArrayList<>();

        // 添加 system prompt
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(new SystemMessage(config.getSystemPrompt()));
        }

        // 添加历史消息（排除当前最后两条：刚保存的用户消息和占位 assistant 消息）
        int skipLast = 2;
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

        // 添加当前用户消息
        if (config.getType() == AiModelType.VISION && attachments != null && !attachments.isEmpty()) {
            // 视觉模型：带附件
            List<Media> mediaList = parseAttachmentsToMedia(attachments);
            messages.add(UserMessage.builder().text(currentContent).media(mediaList).build());
        } else {
            messages.add(new UserMessage(currentContent));
        }

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
        if (config.getMaxTokens() != null) builder.maxTokens(config.getMaxTokens());

        // 思考模式 + 代码解释器（代码解释器要求流式模式且同时开启思考）
        if (Boolean.TRUE.equals(config.getEnableThinking()) || Boolean.TRUE.equals(config.getEnableSearch())) {
            Map<String, Object> extra = new HashMap<>();
            if (Boolean.TRUE.equals(config.getEnableThinking())) {
                extra.put("enable_thinking", true);
                if (config.getThinkingBudget() != null) {
                    extra.put("thinking_budget", config.getThinkingBudget());
                }
                if (streaming && Boolean.TRUE.equals(config.getEnableCodeInterpreter())) {
                    extra.put("enable_code_interpreter", true);
                }
            }
            if (Boolean.TRUE.equals(config.getEnableSearch())) {
                extra.put("enable_search", true);
                Map<String, Object> searchOptions = new HashMap<>();
                if (config.getSearchStrategy() != null && !config.getSearchStrategy().isBlank()) {
                    searchOptions.put("search_strategy", config.getSearchStrategy());
                }
                if (Boolean.TRUE.equals(config.getEnableSource())) {
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
     * 将 Spring AI Message 列表转换为 OpenAI SDK 的 ChatCompletionMessageParam 列表
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
                List<Media> media = um.getMedia();
                if (media != null && !media.isEmpty()) {
                    List<ChatCompletionContentPart> parts = new ArrayList<>();
                    if (StringUtils.hasText(um.getText())) {
                        parts.add(ChatCompletionContentPart.ofText(
                                ChatCompletionContentPartText.builder().text(um.getText()).build()));
                    }
                    for (Media m : media) {
                        parts.add(ChatCompletionContentPart.ofImageUrl(
                                ChatCompletionContentPartImage.builder()
                                        .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                                                .url(m.getData().toString())
                                                .build())
                                        .build()));
                    }
                    result.add(ChatCompletionMessageParam.ofUser(
                            ChatCompletionUserMessageParam.builder()
                                    .contentOfArrayOfContentParts(parts)
                                    .build()));
                } else {
                    result.add(ChatCompletionMessageParam.ofUser(
                            ChatCompletionUserMessageParam.builder()
                                    .content(um.getText() != null ? um.getText() : "")
                                    .build()));
                }
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
     * 构建 OpenAI SDK {@link ChatCompletionCreateParams}，包含思考/搜索扩展参数
     */
    private ChatCompletionCreateParams buildSdkParams(AiModelConfig config, List<Message> messages, boolean streaming) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(config.getModelName())
                .messages(toSdkMessages(messages));

        if (config.getTemperature() != null) builder.temperature(config.getTemperature().doubleValue());
        if (config.getTopP() != null) builder.topP(config.getTopP().doubleValue());
        if (config.getMaxTokens() != null) builder.maxCompletionTokens(config.getMaxTokens().longValue());

        // 思考模式 + 代码解释器（代码解释器要求流式模式且同时开启思考）
        if (Boolean.TRUE.equals(config.getEnableThinking())) {
            builder.putAdditionalBodyProperty("enable_thinking", JsonValue.from(true));
            if (config.getThinkingBudget() != null) {
                builder.putAdditionalBodyProperty("thinking_budget", JsonValue.from(config.getThinkingBudget()));
            }
            if (streaming && Boolean.TRUE.equals(config.getEnableCodeInterpreter())) {
                builder.putAdditionalBodyProperty("enable_code_interpreter", JsonValue.from(true));
            }
        }

        // 联网搜索
        if (Boolean.TRUE.equals(config.getEnableSearch())) {
            builder.putAdditionalBodyProperty("enable_search", JsonValue.from(true));
            Map<String, Object> searchOptions = new HashMap<>();
            if (StringUtils.hasText(config.getSearchStrategy())) {
                searchOptions.put("search_strategy", config.getSearchStrategy());
            }
            if (Boolean.TRUE.equals(config.getEnableSource())) {
                searchOptions.put("enable_source", true);
            }
            if (!searchOptions.isEmpty()) {
                builder.putAdditionalBodyProperty("search_options", JsonValue.from(searchOptions));
            }
        }

        return builder.build();
    }

    /**
     * 将附件列表（JSON 格式 map）转换为 Spring AI Media 对象
     */
    private List<Media> parseAttachmentsToMedia(List<Map<String, String>> attachments) {
        List<Media> mediaList = new ArrayList<>();
        for (Map<String, String> att : attachments) {
            String url = att.get("url");
            String mimeType = att.getOrDefault("mimeType", "image/jpeg");
            if (StringUtils.hasText(url)) {
                try {
                    mediaList.add(new Media(MimeTypeUtils.parseMimeType(mimeType), URI.create(url)));
                } catch (Exception e) {
                    log.warn("AI: 解析附件 URL 失败: {}", url, e);
                }
            }
        }
        return mediaList;
    }

    /**
     * 将附件列表序列化为 JSON 字符串存储
     */
    private String attachmentsToJson(List<Map<String, String>> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        // 简单 JSON 序列化（避免引入额外依赖）
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attachments.size(); i++) {
            Map<String, String> att = attachments.get(i);
            sb.append("{");
            att.forEach((k, v) -> sb.append("\"").append(k).append("\":\"").append(v.replace("\"", "\\\"")).append("\","));
            if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
            sb.append("}");
            if (i < attachments.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将附件 JSON 字符串解析为 List&lt;Map&lt;String, String&gt;&gt;
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseAttachmentsJson(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            return (List<Map<String, String>>) new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {
                    });
        } catch (Exception e) {
            log.warn("解析附件 JSON 失败: {}", json, e);
            return null;
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
