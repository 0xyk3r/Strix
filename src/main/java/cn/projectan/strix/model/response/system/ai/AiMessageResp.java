package cn.projectan.strix.model.response.system.ai;

import cn.projectan.strix.model.db.system.AiMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话消息响应
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Schema(description = "AI 对话消息响应")
@Data
public class AiMessageResp {

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

    @Schema(description = "附件（JSON 数组字符串）")
    private String attachments;

    @Schema(description = "输入 Token 消耗")
    private Integer promptTokens;

    @Schema(description = "输出 Token 消耗")
    private Integer completionTokens;

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

    public static AiMessageResp from(AiMessage message) {
        if (message == null) return null;
        AiMessageResp resp = new AiMessageResp();
        resp.setId(message.getId());
        resp.setSessionId(message.getSessionId());
        resp.setRole(message.getRole());
        resp.setContent(message.getContent());
        resp.setThinkingContent(message.getThinkingContent());
        resp.setAttachments(message.getAttachments());
        resp.setPromptTokens(message.getPromptTokens());
        resp.setCompletionTokens(message.getCompletionTokens());
        resp.setModelConfigId(message.getModelConfigId());
        resp.setDurationMs(message.getDurationMs());
        resp.setStatus(message.getStatus());
        resp.setErrorMsg(message.getErrorMsg());
        resp.setCreatedTime(message.getCreatedTime());
        return resp;
    }
}
