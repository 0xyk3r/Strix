package cn.projectan.strix.websocket.config;

import cn.projectan.strix.websocket.handler.AiAsrWebSocketHandler;
import cn.projectan.strix.websocket.handler.AiTtsWebSocketHandler;
import cn.projectan.strix.websocket.handler.ChatWebSocketHandler;
import cn.projectan.strix.websocket.interceptor.AiAsrHandshakeInterceptor;
import cn.projectan.strix.websocket.interceptor.AiTtsHandshakeInterceptor;
import cn.projectan.strix.websocket.interceptor.ChatWebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatWebSocketHandshakeInterceptor chatWebSocketHandshakeInterceptor;
    private final AiAsrWebSocketHandler aiAsrWebSocketHandler;
    private final AiAsrHandshakeInterceptor aiAsrHandshakeInterceptor;
    private final AiTtsWebSocketHandler aiTtsWebSocketHandler;
    private final AiTtsHandshakeInterceptor aiTtsHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(chatWebSocketHandshakeInterceptor)
                .setAllowedOrigins("*");

        registry.addHandler(aiAsrWebSocketHandler, "/ws/ai/asr")
                .addInterceptors(aiAsrHandshakeInterceptor)
                .setAllowedOrigins("*");

        registry.addHandler(aiTtsWebSocketHandler, "/ws/ai/tts")
                .addInterceptors(aiTtsHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

}
