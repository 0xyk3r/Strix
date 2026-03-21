package cn.projectan.strix.core.ss.handler;

import cn.projectan.strix.service.system.TokenSessionService;
import cn.projectan.strix.util.http.TokenUtil;
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

    private final TokenSessionService tokenSessionService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String token = TokenUtil.resolveToken(request);

        if (StringUtils.hasText(token)) {
            tokenSessionService.logoutManager(token);
        }
    }

}
