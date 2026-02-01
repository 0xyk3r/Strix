package cn.projectan.strix.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 新消息事件
 * <p>
 * 当新消息发送成功后发布此事件，用于触发异步通知等后续操作
 *
 * @author ProjectAn
 * @since 2026/2/2 12:00
 */
@Getter
public class ChatNewMessageEvent extends ApplicationEvent {

    /**
     * 会话 ID
     */
    private final String sessionId;

    /**
     * 消息 ID
     */
    private final String messageId;

    public ChatNewMessageEvent(Object source, String sessionId, String messageId) {
        super(source);
        this.sessionId = sessionId;
        this.messageId = messageId;
    }

}
