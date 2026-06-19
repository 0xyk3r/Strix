package cn.projectan.strix.core.ss.filter;

import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.core.ss.token.SystemUserAuthenticationToken;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import cn.projectan.strix.service.system.TokenSessionService;
import cn.projectan.strix.util.common.RedisUtil;
import cn.projectan.strix.util.http.TokenUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemUserAuthenticationTokenFilter extends OncePerRequestFilter {

    private final RedisUtil redisUtil;
    private final TokenSessionService tokenSessionService;
    private final RequestAttributeSecurityContextRepository requestAttributeSecurityContextRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        String token = TokenUtil.resolveToken(request);

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从redis中获取用户信息
        Object loginInfo = redisUtil.get(LoginRedisKeys.USER_TOKEN_PREFIX + token);
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

        // 更新最后活跃时间
        updateLastActiveTime(loginSystemUser, token);

        filterChain.doFilter(request, response);
    }

    private void updateLastActiveTime(LoginSystemUser loginSystemUser, String token) {
        try {
            if (loginSystemUser.getSystemUser() == null) {
                return;
            }
            String userId = loginSystemUser.getSystemUser().getId();
            if (userId == null) {
                return;
            }
            tokenSessionService.updateUserLastActiveTime(userId, token);
        } catch (Exception e) {
            log.debug("更新用户最后活跃时间失败: {}", e.getMessage());
        }
    }
}
