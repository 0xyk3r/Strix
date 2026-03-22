package cn.projectan.strix.controller.srv.wechat;

import cn.projectan.strix.controller.srv.wechat.base.BaseWechatController;
import cn.projectan.strix.core.cache.system.SystemConfigCache;
import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.service.system.SystemUserService;
import cn.projectan.strix.service.system.TokenSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 微信公众号本地开发调试控制器
 * <p>
 * 仅在 strix.debug.wechat-dev-mode=true 时注册。
 *
 * @author ProjectAn
 */
@Slf4j
@RestController
@RequestMapping("srv/wechat/oa/{configKey}")
@ConditionalOnProperty(prefix = "strix.debug", name = "wechat-dev-mode", havingValue = "true")
@RequiredArgsConstructor
@Tag(name = "服务 - 微信公众号开发调试")
public class WechatOADevController extends BaseWechatController {

    private final SystemUserService systemUserService;
    private final SystemConfigCache systemConfigCache;
    private final TokenSessionService tokenSessionService;

    @Operation(summary = "开发模式模拟授权")
    @Anonymous
    @IgnoreEncryption
    @RequestMapping("giveMeSessionTokenOnDevMode")
    public void devMode(@Parameter(description = "公众号配置 Key") @PathVariable String configKey, HttpServletResponse response) throws IOException {
        log.warn("通过api获取微信Token (开发模式)...");

        String devUserId = "1775599867535130625";
        SystemUser systemUser = systemUserService.getById(devUserId);
        LoginSystemUser loginInfo = systemUserService.getLoginInfo(devUserId);
        long tokenTTL = systemConfigCache.getLong("SYSTEM_USER_LOGIN_EFFECTIVE_TIME", 1440L);

        tokenSessionService.invalidateUserSession(systemUser.getId());
        String token = tokenSessionService.createUserSession(systemUser.getId(), loginInfo, tokenTTL);

        response.sendRedirect("http://localhost:8080/?token=" + token + "&cfid=" + configKey);
    }

}
