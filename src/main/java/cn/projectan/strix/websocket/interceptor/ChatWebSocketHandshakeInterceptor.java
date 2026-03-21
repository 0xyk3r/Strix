package cn.projectan.strix.websocket.interceptor;

import cn.projectan.strix.core.ss.details.LoginSystemUser;
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
 * WebSocket 握手拦截器 - Token 认证
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {
        // 从 query 参数或 header 获取 token
        String token = getToken(request);

        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket 握手失败: Token 为空");
            return false;
        }

        // 从 Redis 验证 token
        String redisKey = LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX + token;
        LoginSystemUser user = (LoginSystemUser) redisTemplate.opsForValue().get(redisKey);

        if (user == null || user.getSystemUser() == null) {
            log.warn("WebSocket 握手失败: Token 无效或已过期, token={}", token);
            return false;
        }

        // 将 userId 存入 attributes，后续 Handler 可以使用
        attributes.put("userId", user.getSystemUser().getId());
        log.info("WebSocket 握手成功: userId={}, token={}", user.getSystemUser().getId(), token);
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后处理，暂无需处理
    }

    /**
     * 从请求中获取 token
     * 优先从 Authorization header 获取，其次从 query 参数获取（WebSocket 兼容）
     */
    private String getToken(ServerHttpRequest request) {
        // 1. 从 header 获取
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. 从 query 参数获取（WebSocket 握手兼容）
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (StringUtils.hasText(token)) {
                return token;
            }
        }

        return null;
    }

}
