package cn.projectan.strix.websocket.handler;

import cn.projectan.strix.service.system.UserOnlineStatusService;
import cn.projectan.strix.websocket.manager.ChatWebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 消息处理器
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatWebSocketSessionManager sessionManager;
    private final UserOnlineStatusService userOnlineStatusService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从 attributes 中获取 userId（由 HandshakeInterceptor 设置）
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.addSession(userId, session);
            // 添加到 Redis 在线状态管理
            userOnlineStatusService.addConnection(userId, session.getId());
            log.info("WebSocket 连接已建立: userId={}, sessionId={}", userId, session.getId());
        } else {
            log.error("WebSocket 连接建立失败: userId 为空, sessionId={}", session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = (String) session.getAttributes().get("userId");
        String payload = message.getPayload();

        // 处理心跳消息
        if ("PING".equalsIgnoreCase(payload) || "heartbeat".equalsIgnoreCase(payload)) {
            // 刷新 Redis 中的在线状态过期时间
            userOnlineStatusService.refreshConnection(userId, session.getId());
            log.debug("收到心跳消息: userId={}, sessionId={}", userId, session.getId());

            // 回复 PONG
            try {
                session.sendMessage(new TextMessage("PONG"));
            } catch (Exception e) {
                log.error("发送 PONG 失败: userId={}, sessionId={}", userId, session.getId(), e);
            }
            return;
        }

        // 其他消息（暂不处理）
        log.debug("收到 WebSocket 消息: userId={}, message={}", userId, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.removeSession(userId);
            // 从 Redis 在线状态管理中移除连接
            userOnlineStatusService.removeConnection(userId, session.getId());
            log.info("WebSocket 连接已关闭: userId={}, sessionId={}, status={}", userId, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, @NonNull Throwable exception) {
        String userId = (String) session.getAttributes().get("userId");
        log.error("WebSocket 传输错误: userId={}, sessionId={}", userId, session.getId(), exception);

        // 发生错误时关闭连接
        if (userId != null) {
            sessionManager.removeSession(userId);
            // 从 Redis 在线状态管理中移除连接
            userOnlineStatusService.removeConnection(userId, session.getId());
        }
    }

}
