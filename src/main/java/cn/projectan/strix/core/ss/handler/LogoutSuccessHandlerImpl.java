package cn.projectan.strix.core.ss.handler;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.utils.RedisUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author 安炯奕
 * @date 2023/5/26 17:58
 */
@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
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
