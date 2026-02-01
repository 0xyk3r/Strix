package cn.projectan.strix.model.response.srv.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天会话列表项响应
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "聊天 - 会话列表项响应")
public class ChatSessionListItemResp extends ChatSessionResp {

    @Schema(description = "未读消息数")
    private Integer unreadCount;

    @Schema(description = "最后一条消息预览")
    private String lastMessagePreview;

    @Schema(description = "最后一条消息发送者名称")
    private String lastMessageSenderName;

}
