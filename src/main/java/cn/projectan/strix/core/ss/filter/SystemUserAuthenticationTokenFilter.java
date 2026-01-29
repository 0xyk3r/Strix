package cn.projectan.strix.core.ss.filter;

import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.core.ss.token.SystemUserAuthenticationToken;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import cn.projectan.strix.util.common.RedisUtil;
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
        Object loginInfo = redisUtil.get(LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX + token);
        if (!(loginInfo instanceof LoginSystemUser loginSystemUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 存入SecurityContextHolder
        SystemUserAuthenticationToken authentication =
                new SystemUserAuthenticationToken(loginSystemUser, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM_USER")));

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        requestAttributeSecurityContextRepository.saveContext(context, request, response);

        filterChain.doFilter(request, response);
    }

}
