package cn.projectan.strix.core.xss;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Set;

/**
 * XSS 过滤器
 * <p>
 * 将 HttpServletRequest 包装为 {@link XssHttpServletRequestWrapper}，
 * 对 Query Parameter 进行 XSS 清理。
 * <p>
 * 自动排除静态资源和 API 文档路径。
 *
 * @author ProjectAn
 * @since 2025-03-21
 */
public class XssFilter implements Filter {

    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            "/webjars/", "/static/", "/v3/api-docs", "/doc.html",
            "/favicon.ico", "/swagger-ui"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            if (isExcluded(httpRequest.getRequestURI())) {
                chain.doFilter(request, response);
            } else {
                chain.doFilter(new XssHttpServletRequestWrapper(httpRequest), response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isExcluded(String uri) {
        for (String prefix : EXCLUDED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
