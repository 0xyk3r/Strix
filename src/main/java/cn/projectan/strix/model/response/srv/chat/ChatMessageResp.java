package cn.projectan.strix.model.response.srv.chat;

import cn.projectan.strix.model.db.system.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息响应
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@Schema(description = "聊天 - 消息响应")
public class ChatMessageResp {

    @Schema(description = "消息 ID")
    private String messageId;

    @Schema(description = "会话 ID")
    private String sessionId;

    @Schema(description = "发送者用户 ID")
    private String fromUserId;

    @Schema(description = "发送者用户名称")
    private String fromUserName;

    @Schema(description = "发送者用户头像")
    private String fromUserAvatar;

    @Schema(description = "消息类型（TEXT/IMAGE/CARD）")
    private String msgType;

    @Schema(description = "文本内容")
    private String content;

    @Schema(description = "图片文件 ID")
    private String imageFileId;

    @Schema(description = "图片 URL")
    private String imageUrl;

    @Schema(description = "卡片类型")
    private String cardType;

    @Schema(description = "卡片数据 ID")
    private String cardDataId;

    @Schema(description = "卡片数据")
    private Object cardData;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    public ChatMessageResp() {
    }

    public ChatMessageResp(ChatMessage message) {
        this.messageId = message.getId();
        this.sessionId = message.getSessionId();
        this.fromUserId = message.getFormUserId();
        this.msgType = message.getMsgType();
        this.content = message.getContent();
        this.imageFileId = message.getImageFileId();
        this.cardType = message.getCardType();
        this.cardDataId = message.getCardDataId();
        this.sendTime = message.getSendTime();
    }

}
