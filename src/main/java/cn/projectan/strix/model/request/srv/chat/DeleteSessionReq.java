package cn.projectan.strix.model.request.srv.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 删除/退出会话请求
 *
 * @author ProjectAn
 * @since 2026/2/2 12:00
 */
@Data
@Schema(description = "聊天 - 删除/退出会话请求")
public class DeleteSessionReq {

    @NotBlank(message = "{validation.required:field.chat.sessionId}")
    @Schema(description = "会话 ID", example = "1234567890")
    private String sessionId;

}
