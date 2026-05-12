package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.ai.AiModelStore;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiSession;
import cn.projectan.strix.model.dict.system.AiMessageStatus;
import cn.projectan.strix.model.dict.system.AiModelType;
import cn.projectan.strix.model.response.system.ai.AiSseEvent;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionChunk;
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

        // 5. 构建 options
        OpenAiChatOptions options = buildChatOptions(config, true);

        // 6. 执行流式调用
        try {
            OpenAiChatModel chatModel = aiModelStore.getChatModel(config);
            Flux<ChatResponse> flux = chatModel.stream(new Prompt(messages, options));

            StringBuilder fullContent = new StringBuilder();
            StringBuilder thinkingContent = new StringBuilder();
            Integer promptTokens = null;
            Integer completionTokens = null;

            for (ChatResponse response : flux.toIterable()) {
                if (response.getResult() == null) continue;

                String delta = response.getResult().getOutput().getText();
                String thinkingDelta = extractThinkingDelta(response);

                // 发送思考内容块
                if (StringUtils.hasText(thinkingDelta)) {
                    thinkingContent.append(thinkingDelta);
                    sendSseEvent(emitter, AiSseEvent.THINKING, Map.of("content", thinkingDelta));
                }

                // 发送正文内容块
                if (StringUtils.hasText(delta)) {
                    fullContent.append(delta);
                    sendSseEvent(emitter, AiSseEvent.CONTENT, Map.of("content", delta));
                }

                // 提取 Token 使用情况
                if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                    var usage = response.getMetadata().getUsage();
                    if (usage.getPromptTokens() != null) promptTokens = usage.getPromptTokens().intValue();
                    if (usage.getCompletionTokens() != null) completionTokens = usage.getCompletionTokens().intValue();
                }
            }

            // 7. 更新 assistant 消息为完成状态
            aiMessageService.markCompleted(assistantMsg.getId(),
                    fullContent.toString(),
                    thinkingContent.isEmpty() ? null : thinkingContent.toString(),
                    promptTokens, completionTokens);

            // 8. 发送完成事件
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("messageId", assistantMsg.getId());
            if (promptTokens != null) doneData.put("promptTokens", promptTokens);
            if (completionTokens != null) doneData.put("completionTokens", completionTokens);
            sendSseEvent(emitter, AiSseEvent.DONE, doneData);
            emitter.complete();

        } catch (Exception e) {
            log.error("AI 流式对话出错: sessionId={}", sessionId, e);
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
     * 从流式响应中提取思考内容（reasoning_content）
     * <p>
     * 百炼 qwen3 思考模式将 {@code reasoning_content} 作为非标准字段返回，
     * Spring AI 2.0 将原始 {@link ChatCompletionChunk.Choice} 存入 metadata["chunkChoice"]，
     * 该字段通过 openai-java SDK 的 {@code _additionalProperties()} 获取。
     */
    private String extractThinkingDelta(ChatResponse response) {
        if (response.getResult() == null || response.getResult().getOutput() == null) return null;
        Map<String, Object> metadata = response.getResult().getOutput().getMetadata();
        if (metadata == null) return null;
        Object chunkChoiceObj = metadata.get("chunkChoice");
        if (!(chunkChoiceObj instanceof ChatCompletionChunk.Choice chunkChoice)) return null;
        try {
            JsonValue reasoningValue = chunkChoice.delta()._additionalProperties().get("reasoning_content");
            if (reasoningValue == null) return null;
            // asString() 在 Java 端受 Kotlin 泛型擦除影响，orElse 返回 Object，需用 instanceof 提取
            Object val = reasoningValue.asString().orElse(null);
            return val instanceof String s ? s : null;
        } catch (Exception e) {
            return null;
        }
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
