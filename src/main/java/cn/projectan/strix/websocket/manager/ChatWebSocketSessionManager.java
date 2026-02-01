package cn.projectan.strix.websocket.manager;

import cn.hutool.json.JSONUtil;
import cn.projectan.strix.websocket.model.ChatWebSocketNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Slf4j
@Component
public class ChatWebSocketSessionManager {

    /**
     * 存储 userId -> WebSocketSession 映射
     */
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(String userId, WebSocketSession session) {
        sessions.put(userId, session);
        log.info("WebSocket 会话已添加: userId={}, sessionId={}", userId, session.getId());
    }

    /**
     * 移除会话
     */
    public void removeSession(String userId) {
        WebSocketSession removed = sessions.remove(userId);
        if (removed != null) {
            log.info("WebSocket 会话已移除: userId={}, sessionId={}", userId, removed.getId());
        }
    }

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(String userId, ChatWebSocketNotification notification) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = JSONUtil.toJsonStr(notification);
                session.sendMessage(new TextMessage(json));
                log.debug("WebSocket 消息已发送: userId={}, notification={}", userId, json);
            } catch (IOException e) {
                log.error("WebSocket 消息发送失败: userId={}", userId, e);
            }
        } else {
            log.debug("用户不在线或连接已关闭，跳过发送: userId={}", userId);
        }
    }

    /**
     * 发送消息给多个用户
     */
    public void sendToUsers(List<String> userIds, ChatWebSocketNotification notification) {
        for (String userId : userIds) {
            sendToUser(userId, notification);
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isOnline(String userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return (int) sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }

}
