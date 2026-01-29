package cn.projectan.strix.core.ss.handler;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.util.common.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author ProjectAn
 * @since 2023/5/26 17:58
 */
@Component
@RequiredArgsConstructor
public class SystemManagerLogoutSuccessHandler implements LogoutSuccessHandler {

    private final RedisUtil redisUtil;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String token = request.getHeader("token");

        if (StringUtils.hasText(token)) {
            // 从redis中获取用户信息
            Object loginInfo = redisUtil.get(LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX + token);
            if (loginInfo instanceof LoginSystemManager loginSystemManager) {
                SystemManager systemManager = loginSystemManager.getSystemManager();
                redisUtil.del(LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX + token);
                if (systemManager != null && StringUtils.hasText(systemManager.getId())) {
                    redisUtil.del(LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + systemManager.getId());
                }
            }
        }
    }

}
