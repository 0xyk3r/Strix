package cn.projectan.strix.core.ss.filter;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.core.ss.token.SystemManagerAuthenticationToken;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 管理员 Token 认证过滤器
 *
 * @author ProjectAn
 * @since 2023/2/25 0:27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemManagerAuthenticationTokenFilter extends OncePerRequestFilter {

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

        // 从 Redis 中获取用户信息 (新 key 前缀)
        Object loginInfo = redisUtil.get(LoginRedisKeys.MANAGER_TOKEN_PREFIX + token);
        if (!(loginInfo instanceof LoginSystemManager loginSystemManager)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 存入 SecurityContextHolder
        SystemManagerAuthenticationToken authentication =
                new SystemManagerAuthenticationToken(loginSystemManager, null, loginSystemManager.getAuthorities());

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        requestAttributeSecurityContextRepository.saveContext(context, request, response);

        // 更新最后活跃时间
        updateLastActiveTime(loginSystemManager, token);

        filterChain.doFilter(request, response);
    }

    private void updateLastActiveTime(LoginSystemManager loginSystemManager, String token) {
        try {
            if (loginSystemManager.getSystemManager() == null) {
                return;
            }
            String managerId = loginSystemManager.getSystemManager().getId();
            if (managerId != null) {
                tokenSessionService.updateManagerLastActiveTime(managerId, token);
            }
        } catch (Exception e) {
            log.debug("更新管理员最后活跃时间失败: {}", e.getMessage());
        }
    }
}
