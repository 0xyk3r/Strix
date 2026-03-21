package cn.projectan.strix.controller.srv.debug;

import cn.projectan.strix.controller.srv.base.BaseSrvController;
import cn.projectan.strix.core.cache.system.SystemConfigCache;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.response.system.login.SystemUserLoginResp;
import cn.projectan.strix.service.system.SystemUserService;
import cn.projectan.strix.service.system.TokenSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2026/2/1 22:16
 */
@Slf4j
@RestController("SrvDebugController")
@RequestMapping("/srv/debug")
@Tag(name = "调试服务", description = "调试用接口，仅在开发环境启用")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.profiles", name = "active", havingValue = "dev")
public class DebugController extends BaseSrvController {

    private final SystemUserService systemUserService;
    private final SystemConfigCache systemConfigCache;
    private final TokenSessionService tokenSessionService;

    @GetMapping("loginAnyUser/{userId}")
    @Operation(summary = "登录任意用户", description = "调试用，登录任意用户，返回该用户的登录信息和 token")
    public RetResult<SystemUserLoginResp> loginAnyUser(@PathVariable String userId) {
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, "用户不存在: " + userId);
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
