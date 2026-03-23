package cn.projectan.strix.model.request.srv.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 标记已读请求
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@Schema(description = "聊天 - 标记已读请求")
public class MarkReadReq {

    @NotBlank(message = "{validation.required:field.chat.sessionId}")
    @Schema(description = "会话 ID", example = "1234567890")
    private String sessionId;

    @NotBlank(message = "{validation.required:field.chat.lastReadMsgId}")
    @Schema(description = "最后已读消息 ID", example = "100")
    private String lastReadId;

}
