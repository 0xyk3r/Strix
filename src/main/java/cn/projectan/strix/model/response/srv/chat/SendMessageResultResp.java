package cn.projectan.strix.model.response.srv.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发送消息结果响应
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@Schema(description = "聊天 - 发送消息结果响应")
public class SendMessageResultResp {

    @Schema(description = "消息 ID")
    private String messageId;

    @Schema(description = "会话 ID")
    private String sessionId;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

}
