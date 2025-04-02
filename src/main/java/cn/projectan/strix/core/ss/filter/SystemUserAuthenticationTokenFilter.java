package cn.projectan.strix.core.ss.filter;

import cn.projectan.strix.core.ss.token.SystemUserAuthenticationToken;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.util.RedisUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * @author ProjectAn
 * @since 2023/2/25 15:09
 */
@Component
@RequiredArgsConstructor
public class SystemUserAuthenticationTokenFilter extends OncePerRequestFilter {

    private final RedisUtil redisUtil;
    private final RequestAttributeSecurityContextRepository requestAttributeSecurityContextRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("token");

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从redis中获取用户信息
        Object loginInfo = redisUtil.get("strix:system:user:login_token:token:" + token);
        if (!(loginInfo instanceof SystemUser systemUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 存入SecurityContextHolder
        SystemUserAuthenticationToken authentication =
                new SystemUserAuthenticationToken(systemUser, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM_USER")));

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        requestAttributeSecurityContextRepository.saveContext(context, request, response);

        filterChain.doFilter(request, response);
    }

}
