package cn.projectan.strix.core.ss.filter;

import cn.projectan.strix.core.ss.token.SystemUserAuthenticationToken;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import java.util.Collections;

/**
 * @author 安炯奕
 * @date 2023/2/25 15:09
 */
@Component
public class SystemUserAuthenticationTokenFilter extends OncePerRequestFilter {

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
        Object loginInfo = redisUtil.get("strix:system:user:login_token:token:" + token);
        if (loginInfo == null || !(loginInfo instanceof SystemUser)) {
            filterChain.doFilter(request, response);
            return;
        }
        SystemUser systemUser = (SystemUser) loginInfo;

        // 存入SecurityContextHolder
        SystemUserAuthenticationToken authentication =
                new SystemUserAuthenticationToken(systemUser, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM_USER")));

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

}
