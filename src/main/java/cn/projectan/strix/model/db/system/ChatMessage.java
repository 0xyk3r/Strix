package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 * Strix Chat 消息
 * </p>
 *
 * @author ProjectAn
 * @since 2026-02-01
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_chat_message")
public class ChatMessage extends BaseModel<ChatMessage> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 聊天会话 ID
     */
    private String sessionId;

    /**
     * 聊天成员用户 ID
     */
    private String formUserId;

    /**
     * 聊天消息类型 (TEXT / IMAGE / CARD)
     */
    private String msgType;

    /**
     * 聊天消息卡片类型 (仅 msg_type = CARD)
     */
    private String cardType;

    /**
     * 聊天消息卡片数据 ID (仅 msg_type = CARD)
     */
    private String cardDataId;

    /**
     * 聊天消息图片文件 ID
     */
    private String imageFileId;

    /**
     * 聊天消息文本内容
     */
    private String content;

    /**
     * 聊天消息发送时间
     */
    private LocalDateTime sendTime;

}
