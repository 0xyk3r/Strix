package cn.projectan.strix.core.ss.filter;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.core.ss.token.SystemManagerAuthenticationToken;
import cn.projectan.strix.utils.RedisUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author ProjectAn
 * @date 2023/2/25 0:27
 */
@Component
@RequiredArgsConstructor
public class SystemManagerAuthenticationTokenFilter extends OncePerRequestFilter {

    private final RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("token");

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从redis中获取用户信息
        Object loginInfo = redisUtil.get("strix:system:manager:login_token:token:" + token);
        if (!(loginInfo instanceof LoginSystemManager loginSystemManager)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 存入SecurityContextHolder
        SystemManagerAuthenticationToken authentication =
                new SystemManagerAuthenticationToken(loginSystemManager, null, loginSystemManager.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

}
