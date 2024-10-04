package cn.projectan.strix.core.ss.handler;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.utils.RedisUtil;
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
            Object loginInfo = redisUtil.get("strix:system:manager:login_token:token:" + token);
            if (loginInfo instanceof LoginSystemManager loginSystemManager) {
                SystemManager systemManager = loginSystemManager.getSystemManager();
                redisUtil.del("strix:system:manager:login_token:token:" + token);
                if (systemManager != null && StringUtils.hasText(systemManager.getId())) {
                    redisUtil.del("strix:system:manager:login_token:login:id_" + systemManager.getId());
                }
            }
        }
    }

}
