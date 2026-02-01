package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.ChatSessionMemberMapper;
import cn.projectan.strix.model.db.system.ChatSessionMember;
import cn.projectan.strix.model.event.ChatNewMessageEvent;
import cn.projectan.strix.model.event.ChatSessionCreatedEvent;
import cn.projectan.strix.websocket.manager.ChatWebSocketSessionManager;
import cn.projectan.strix.websocket.model.ChatWebSocketNotification;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 聊天通知服务
 * <p>
 * 负责向会话成员发送 WebSocket 通知
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNotificationService {

    private final ChatWebSocketSessionManager sessionManager;
    private final ChatSessionMemberMapper chatSessionMemberMapper;

    // ========== 事件监听器（事务提交后触发） ==========

    /**
     * 监听会话创建事件，事务提交后发送会话更新通知
     *
     * @param event 会话创建事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSessionCreated(ChatSessionCreatedEvent event) {
        notifySessionUpdate(event.getSessionId());
    }

    /**
     * 监听新消息事件，事务提交后发送新消息通知
     *
     * @param event 新消息事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNewMessage(ChatNewMessageEvent event) {
        notifyNewMessage(event.getSessionId(), event.getMessageId());
    }

    // ========== 通知方法 ==========

    /**
     * 发送新消息通知
     *
     * @param sessionId 会话 ID
     * @param maxMsgId  最大消息 ID
     */
    public void notifyNewMessage(String sessionId, String maxMsgId) {
        // 查询会话所有成员
        List<String> memberUserIds = getMemberUserIds(sessionId);

        if (memberUserIds.isEmpty()) {
            log.warn("会话成员为空，跳过新消息通知: sessionId={}", sessionId);
            return;
        }

        // 构建通知消息
        ChatWebSocketNotification notification = ChatWebSocketNotification.newMessage(sessionId, maxMsgId);

        // 推送给所有在线成员
        sessionManager.sendToUsers(memberUserIds, notification);
        log.debug("新消息通知已发送: sessionId={}, maxMsgId={}, memberCount={}", sessionId, maxMsgId, memberUserIds.size());
    }

    /**
     * 发送会话更新通知
     *
     * @param sessionId 会话 ID
     */
    public void notifySessionUpdate(String sessionId) {
        // 查询会话所有成员
        List<String> memberUserIds = getMemberUserIds(sessionId);

        if (memberUserIds.isEmpty()) {
            log.warn("会话成员为空，跳过会话更新通知: sessionId={}", sessionId);
            return;
        }

        // 构建通知消息
        ChatWebSocketNotification notification = ChatWebSocketNotification.sessionUpdate(sessionId);

        // 推送给所有在线成员
        sessionManager.sendToUsers(memberUserIds, notification);
        log.debug("会话更新通知已发送: sessionId={}, memberCount={}", sessionId, memberUserIds.size());
    }

    /**
     * 获取会话所有成员的用户 ID
     *
     * @param sessionId 会话 ID
     * @return 用户 ID 列表
     */
    private List<String> getMemberUserIds(String sessionId) {
        LambdaQueryWrapper<ChatSessionMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getDeletedStatus, 0);

        List<ChatSessionMember> members = chatSessionMemberMapper.selectList(queryWrapper);
        return members.stream()
                .map(ChatSessionMember::getUserId)
                .toList();
    }

}
