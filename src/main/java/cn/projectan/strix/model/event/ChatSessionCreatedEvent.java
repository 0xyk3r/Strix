package cn.projectan.strix.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 会话创建事件
 * <p>
 * 当新会话创建完成后发布此事件，用于触发异步通知等后续操作
 *
 * @author ProjectAn
 * @since 2026/2/2 12:00
 */
@Getter
public class ChatSessionCreatedEvent extends ApplicationEvent {

    /**
     * 会话 ID
     */
    private final String sessionId;

    public ChatSessionCreatedEvent(Object source, String sessionId) {
        super(source);
        this.sessionId = sessionId;
    }

}
