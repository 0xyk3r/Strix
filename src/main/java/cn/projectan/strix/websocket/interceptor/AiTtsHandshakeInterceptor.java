package cn.projectan.strix.websocket.interceptor;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * AI TTS 双向流式语音合成 WebSocket 握手拦截器
 * <p>
 * 验证用户 Token 并提取 configKey 查询参数，存入 WebSocket Session 属性中，
 * 供 {@link cn.projectan.strix.websocket.handler.AiTtsWebSocketHandler} 使用。
 * </p>
 *
 * <p>连接示例：{@code ws://host/api/ws/ai/tts?token=<token>&configKey=<key>}</p>
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTtsHandshakeInterceptor implements HandshakeInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {
        String token = getQueryParam(request, "token");
        if (!StringUtils.hasText(token)) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (!StringUtils.hasText(token)) {
            log.warn("TTS WebSocket 握手失败: Token 为空");
            return false;
        }

        String redisKey = LoginRedisKeys.MANAGER_TOKEN_PREFIX + token;
        LoginSystemManager manager = (LoginSystemManager) redisTemplate.opsForValue().get(redisKey);
        if (manager == null || manager.getSystemManager() == null) {
            log.warn("TTS WebSocket 握手失败: Token 无效或已过期");
            return false;
        }

        String configKey = getQueryParam(request, "configKey");
        if (!StringUtils.hasText(configKey)) {
            log.warn("TTS WebSocket 握手失败: configKey 为空");
            return false;
        }

        attributes.put("userId", manager.getSystemManager().getId());
        attributes.put("configKey", configKey);
        log.info("TTS WebSocket 握手成功: managerId={}, configKey={}", manager.getSystemManager().getId(), configKey);
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后无需额外处理
    }

    private String getQueryParam(ServerHttpRequest request, String paramName) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getParameter(paramName);
        }
        return null;
    }
}
