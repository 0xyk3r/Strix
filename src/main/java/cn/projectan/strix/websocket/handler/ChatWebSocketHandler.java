package cn.projectan.strix.websocket.handler;

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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从 attributes 中获取 userId（由 HandshakeInterceptor 设置）
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.addSession(userId, session);
            log.info("WebSocket 连接已建立: userId={}, sessionId={}", userId, session.getId());
        } else {
            log.error("WebSocket 连接建立失败: userId 为空, sessionId={}", session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 当前设计中，客户端不发送消息给服务端（只接收通知）
        // 如果未来需要处理客户端消息（如心跳），可以在此实现
        String userId = (String) session.getAttributes().get("userId");
        log.debug("收到 WebSocket 消息: userId={}, message={}", userId, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.removeSession(userId);
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
        }
    }

}
