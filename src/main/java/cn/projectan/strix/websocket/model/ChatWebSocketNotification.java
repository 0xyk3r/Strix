package cn.projectan.strix.websocket.model;

import cn.projectan.strix.model.enums.system.ChatWebSocketNotifyTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 聊天通知消息
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatWebSocketNotification {

    /**
     * 通知类型（NEW_MSG / SESSION_UPDATE）
     */
    private String type;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 最大消息 ID（type=NEW_MSG 时使用）
     */
    private String maxMsgId;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 创建新消息通知
     */
    public static ChatWebSocketNotification newMessage(String sessionId, String maxMsgId) {
        return new ChatWebSocketNotification(ChatWebSocketNotifyTypeEnum.NEW_MSG.getCodeValue(), sessionId, maxMsgId, LocalDateTime.now());
    }

    /**
     * 创建会话更新通知
     */
    public static ChatWebSocketNotification sessionUpdate(String sessionId) {
        return new ChatWebSocketNotification(ChatWebSocketNotifyTypeEnum.SESSION_UPDATE.getCodeValue(), sessionId, null, LocalDateTime.now());
    }

}
