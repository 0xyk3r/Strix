package cn.projectan.strix.model.request.srv.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送消息请求
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@Schema(description = "聊天 - 发送消息请求")
public class SendMessageReq {

    @NotBlank(message = "会话 ID 不能为空")
    @Schema(description = "会话 ID", example = "1234567890")
    private String sessionId;

    @NotBlank(message = "消息类型不能为空")
    @Schema(description = "消息类型（TEXT/IMAGE/CARD）", example = "TEXT")
    private String msgType;

    @NotBlank(message = "客户端消息 ID 不能为空")
    @Schema(description = "客户端消息 ID（用于幂等）", example = "client_msg_12345")
    private String clientMsgId;

    @Schema(description = "文本内容（msgType=TEXT 时使用）", example = "你好")
    private String content;

    @Schema(description = "图片文件 ID（msgType=IMAGE 时使用）", example = "file_123")
    private String imageFileId;

    @Schema(description = "卡片类型（msgType=CARD 时使用）", example = "ORDER_CARD")
    private String cardType;

    @Schema(description = "卡片数据 ID（msgType=CARD 时使用）", example = "order_123")
    private String cardDataId;

}
