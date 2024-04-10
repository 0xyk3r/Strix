package cn.projectan.strix.utils.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求上下文拦截器
 *
 * @author ProjectAn
 * @date 2023/12/9 16:25
 */
public class ContextInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) throws Exception {
        // 在请求结束时，清理数据
        ContextHolder.clear();
    }

}
