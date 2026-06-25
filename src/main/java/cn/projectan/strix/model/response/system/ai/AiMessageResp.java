package cn.projectan.strix.model.response.system.ai;

import cn.projectan.strix.model.db.system.AiMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 对话消息响应
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Schema(description = "AI 对话消息响应")
@Data
public class AiMessageResp {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Schema(description = "消息 ID")
    private String id;

    @Schema(description = "会话 ID")
    private String sessionId;

    @Schema(description = "消息角色：user/assistant/system")
    private String role;

    @Schema(description = "消息正文内容")
    private String content;

    @Schema(description = "思考内容（qwen3 thinking 模式）")
    private String thinkingContent;

    @Schema(description = "附件列表")
    private List<AttachmentResp> attachments;

    @Schema(description = "输入 Token 消耗")
    private Integer promptTokens;

    @Schema(description = "输出 Token 消耗")
    private Integer completionTokens;

    @Schema(description = "缓存命中 Token 数（实际计费输入 = promptTokens - cacheHitTokens）")
    private Integer cacheHitTokens;

    @Schema(description = "缓存写入 Token 数（DashScope 特有，有独立计费单价）")
    private Integer cacheWriteTokens;

    @Schema(description = "思考链 Token 数（含于 completionTokens 内，思考模式专用）")
    private Integer reasoningTokens;

    @Schema(description = "模型配置 ID")
    private String modelConfigId;

    @Schema(description = "模型配置名称")
    private String modelConfigName;

    @Schema(description = "生成耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "消息状态：0=生成中 1=完成 2=错误")
    private Short status;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Data
    public static class AttachmentResp {
        private String fileId;
        private String type;
        private String mimeType;
        private String name;
        private String previewUrl;
    }

    public static AiMessageResp from(AiMessage message) {
        if (message == null) return null;
        AiMessageResp resp = new AiMessageResp();
        resp.setId(message.getId());
        resp.setSessionId(message.getSessionId());
        resp.setRole(message.getRole());
        resp.setContent(message.getContent());
        resp.setThinkingContent(message.getThinkingContent());
        resp.setAttachments(parseAttachments(message.getAttachments()));
        resp.setPromptTokens(message.getPromptTokens());
        resp.setCompletionTokens(message.getCompletionTokens());
        resp.setCacheHitTokens(message.getCacheHitTokens());
        resp.setCacheWriteTokens(message.getCacheWriteTokens());
        resp.setReasoningTokens(message.getReasoningTokens());
        resp.setModelConfigId(message.getModelConfigId());
        resp.setDurationMs(message.getDurationMs());
        resp.setStatus(message.getStatus());
        resp.setErrorMsg(message.getErrorMsg());
        resp.setCreatedTime(message.getCreatedTime());
        return resp;
    }

    private static List<AttachmentResp> parseAttachments(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return null;
        }
    }
}
