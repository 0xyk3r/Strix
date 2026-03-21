package cn.projectan.strix.util.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * HTTP 认证 Token 解析工具类
 * <p>
 * 从标准 Authorization: Bearer xxx 头中提取 Token
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/21
 */
public final class TokenUtil {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private TokenUtil() {
    }

    /**
     * 从 HttpServletRequest 的 Authorization 头中解析 Bearer Token
     *
     * @param request HTTP 请求
     * @return Token 字符串，无有效 Token 时返回 null
     */
    public static String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }

}
