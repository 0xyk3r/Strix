package cn.projectan.strix.core.xss;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * XSS 过滤器
 * <p>
 * 将 HttpServletRequest 包装为 {@link XssHttpServletRequestWrapper}，
 * 对 Query Parameter 进行 XSS 清理。
 * </p>
 *
 * @author ProjectAn
 * @since 2025-03-21
 */
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            chain.doFilter(new XssHttpServletRequestWrapper(httpRequest), response);
        } else {
            chain.doFilter(request, response);
        }
    }

}
