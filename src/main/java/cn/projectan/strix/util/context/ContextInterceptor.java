package cn.projectan.strix.util.context;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求上下文拦截器
 *
 * @author ProjectAn
 * @since 2023/12/9 16:25
 */
public class ContextInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler, Exception ex) throws Exception {
        // 在请求结束时，清理数据
        ContextHolder.clear();
    }

}
