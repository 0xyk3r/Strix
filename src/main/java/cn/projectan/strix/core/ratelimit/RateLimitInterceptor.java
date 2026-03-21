package cn.projectan.strix.core.ratelimit;

import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.RateLimit;
import cn.projectan.strix.model.constant.system.StrixRedisKeyConst;
import cn.projectan.strix.model.properties.system.StrixRateLimitProperties;
import cn.projectan.strix.util.common.RedisUtil;
import cn.projectan.strix.util.ip.IpUtils;
import cn.projectan.strix.util.system.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * API 速率限制拦截器
 * <p>
 * 基于 Redis 固定窗口计数器实现。
 * 已认证用户按用户 ID 限流，未认证请求按客户端 IP 限流。
 * 支持通过 {@link RateLimit} 注解自定义单接口限额。
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;
    private final StrixRateLimitProperties properties;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_MESSAGE = "请求过于频繁，请稍后再试";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isEnabled()) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 获取 @RateLimit 注解（方法级 > 类级）
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        int limit = resolveLimit(rateLimit);
        int window = resolveWindow(rateLimit);

        // 构建 Redis key
        String identifier = resolveIdentifier(request);
        String path = resolvePath(request, rateLimit);
        String redisKey = StrixRedisKeyConst.STR_RATE_LIMIT_PREFIX + identifier + ":" + path;

        // Redis 计数
        long count = redisUtil.incr(redisKey);
        if (count == 1) {
            redisUtil.setExpire(redisKey, window, TimeUnit.SECONDS);
        }

        if (count > limit) {
            long retryAfter = redisUtil.getExpire(redisKey);
            String message = (rateLimit != null && StringUtils.hasText(rateLimit.message()))
                    ? rateLimit.message() : DEFAULT_MESSAGE;
            writeErrorResponse(response, retryAfter, message);
            log.warn("Rate limit exceeded: identifier={}, path={}, count={}, limit={}", identifier, path, count, limit);
            return false;
        }

        // 添加限流信息响应头
        response.setIntHeader("X-RateLimit-Limit", limit);
        response.setIntHeader("X-RateLimit-Remaining", (int) (limit - count));

        return true;
    }

    private int resolveLimit(RateLimit rateLimit) {
        if (rateLimit != null && rateLimit.limit() > 0) {
            return rateLimit.limit();
        }
        return properties.getDefaultLimit();
    }

    private int resolveWindow(RateLimit rateLimit) {
        if (rateLimit != null && rateLimit.window() > 0) {
            return rateLimit.window();
        }
        return properties.getDefaultWindow();
    }

    /**
     * 确定限流标识：已认证用户用 userId，未认证用 IP
     */
    private String resolveIdentifier(HttpServletRequest request) {
        String operatorId = SecurityUtil.getOperatorId();
        if (StringUtils.hasText(operatorId)) {
            return "u:" + operatorId;
        }
        return "ip:" + IpUtils.getIpAddr(request);
    }

    /**
     * 确定限流路径 key
     */
    private String resolvePath(HttpServletRequest request, RateLimit rateLimit) {
        if (rateLimit != null && StringUtils.hasText(rateLimit.key())) {
            return rateLimit.key();
        }
        return request.getRequestURI();
    }

    private void writeErrorResponse(HttpServletResponse response, long retryAfter, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
        RetResult<Object> result = new RetResult<>(RetCode.TOO_MANY_REQUESTS, message, null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

}
