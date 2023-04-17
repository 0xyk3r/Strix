package cn.projectan.strix.core.ss.filter;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.core.ss.token.SystemManagerAuthenticationToken;
import cn.projectan.strix.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author 安炯奕
 * @date 2023/2/25 0:27
 */
@Component
public class SystemManagerAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("token");

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从redis中获取用户信息
        Object loginInfo = redisUtil.get("strix:system:manager:login_token:token:" + token);
        if (!(loginInfo instanceof LoginSystemManager)) {
            filterChain.doFilter(request, response);
            return;
        }
        LoginSystemManager loginSystemManager = (LoginSystemManager) loginInfo;

        // 存入SecurityContextHolder
        SystemManagerAuthenticationToken authentication =
                new SystemManagerAuthenticationToken(loginSystemManager, null, loginSystemManager.getAuthorities());

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

}
