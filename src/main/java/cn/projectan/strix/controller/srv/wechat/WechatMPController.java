package cn.projectan.strix.controller.srv.wechat;

import cn.projectan.strix.controller.srv.wechat.base.BaseWechatController;
import cn.projectan.strix.core.cache.system.SystemConfigCache;
import cn.projectan.strix.core.module.oauth.impl.WechatMPOAuthClient;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.dict.system.OAuthPlatform;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.model.request.api.wechat.WechatMPAuthReq;
import cn.projectan.strix.model.response.system.login.SystemUserLoginResp;
import cn.projectan.strix.service.system.OauthConfigService;
import cn.projectan.strix.service.system.OauthUserService;
import cn.projectan.strix.service.system.SystemUserService;
import cn.projectan.strix.service.system.TokenSessionService;
import cn.projectan.strix.util.common.I18nUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2026/1/29 09:25
 */
@Slf4j
@RestController
@RequestMapping("srv/wechat/mp/{configKey}")
@RequiredArgsConstructor
@Tag(name = "服务 - 微信小程序")
public class WechatMPController extends BaseWechatController {

    private final SystemUserService systemUserService;
    private final OauthUserService oauthUserService;
    private final OauthConfigService oauthConfigService;
    private final SystemConfigCache systemConfigCache;
    private final TokenSessionService tokenSessionService;

    @Operation(summary = "小程序测试接口")
    @GetMapping("test")
    public RetResult<Void> test(@Parameter(description = "小程序配置 Key") @PathVariable String configKey) {
        log.info("test{}", configKey);
        log.info("test user: {}", getLoginSystemUserId());

        return RetBuilder.success();
    }

    @Operation(summary = "小程序登录授权")
    @Anonymous
    @PostMapping("auth")
    public RetResult<SystemUserLoginResp> auth(@Parameter(description = "小程序配置 Key") @PathVariable String configKey, @Validated @RequestBody WechatMPAuthReq req) {
        WechatMPOAuthClient instance = (WechatMPOAuthClient) oauthConfigService.getInstance(configKey, OAuthPlatform.WECHAT_MP);
        BaseOAuthUserInfo userInfo = instance.auth(req.getCode());
        Assert.notNull(userInfo, I18nUtil.failed("field.userInfo"));

        SystemUser systemUser = oauthUserService.loginOrCreateSystemUser(userInfo, OAuthPlatform.WECHAT_MP);
        LoginSystemUser loginInfo = systemUserService.getLoginInfo(systemUser.getId());

        long tokenTTL = systemConfigCache.getLong("SYSTEM_USER_LOGIN_EFFECTIVE_TIME", 1440L);

        // 优先返回已存在的 token
        String existingToken = tokenSessionService.getOrRefreshUserSession(systemUser.getId(), loginInfo, tokenTTL);
        if (existingToken != null) {
            return RetBuilder.success(
                    new SystemUserLoginResp(
                            new SystemUserLoginResp.LoginUserInfo(loginInfo),
                            existingToken,
                            LocalDateTime.now().plusMinutes(tokenTTL)
                    ));
        }

        // 创建新 token
        String token = tokenSessionService.createUserSession(systemUser.getId(), loginInfo, tokenTTL);

        return RetBuilder.success(
                new SystemUserLoginResp(
                        new SystemUserLoginResp.LoginUserInfo(loginInfo),
                        token,
                        LocalDateTime.now().plusMinutes(tokenTTL)
                ));
    }

}
