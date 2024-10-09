package cn.projectan.strix.controller.system;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.SystemConfigCache;
import cn.projectan.strix.core.captcha.CaptchaService;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.dict.SystemManagerStatus;
import cn.projectan.strix.model.other.captcha.CaptchaInfoVO;
import cn.projectan.strix.model.request.system.SystemLoginReq;
import cn.projectan.strix.model.response.module.captcha.StrixCaptchaResp;
import cn.projectan.strix.model.response.system.SystemLoginResp;
import cn.projectan.strix.model.response.system.SystemMenuResp;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemMenuService;
import cn.projectan.strix.util.RedisUtil;
import cn.projectan.strix.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统基础接口
 *
 * @author ProjectAn
 * @since 2021/5/12 18:39
 */
@Slf4j
@RestController
@RequestMapping("system")
@RequiredArgsConstructor
public class SystemController extends BaseSystemController {

    private final SystemMenuService systemMenusService;
    private final SystemManagerService systemManagerService;
    private final CaptchaService captchaService;
    private final SystemConfigCache systemConfigCache;
    private final RedisUtil redisUtil;

    /**
     * 系统登录
     */
    @Anonymous
    @PostMapping("login")
    @StrixLog(operationGroup = "系统登录", operationName = "系统登录", operationType = SysLogOperType.LOGIN)
    public RetResult<Object> login(@RequestBody SystemLoginReq req) {
        // 验证码校验
        Assert.hasText(req.getCaptchaVerification(), "行为验证不通过，请重新验证");
        CaptchaInfoVO captchaInfoVO = new CaptchaInfoVO();
        captchaInfoVO.setCaptchaVerification(req.getCaptchaVerification());
        StrixCaptchaResp strixCaptchaResp = captchaService.verification(captchaInfoVO);
        Assert.isTrue(strixCaptchaResp.isSuccess(), "行为验证不通过，请重新验证");

        SystemManager systemManager = systemManagerService.lambdaQuery()
                .eq(SystemManager::getLoginName, req.getLoginName())
                .one();
        Assert.notNull(systemManager, "账号或密码错误");
        Assert.isTrue(systemManager.getLoginPassword().equals(req.getLoginPassword()), "账号或密码错误");
        Assert.isTrue(systemManager.getStatus() == SystemManagerStatus.NORMAL, "该管理用户已停用");

        // 检查是否支持多点登录
        Boolean supportMultipleLogin = systemConfigCache.getBoolean("SYSTEM_MANAGER_SUPPORT_MULTIPLE_LOGIN", false);
        if (!supportMultipleLogin) {
            // 使上次登录生成的Token失效
            Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + systemManager.getId());
            if (existToken != null) {
                redisUtil.del("strix:system:manager:login_token:token:" + existToken);
                redisUtil.del("strix:system:manager:login_token:login:id_" + systemManager.getId());
            }
        }

        LoginSystemManager loginSystemManager = systemManagerService.getLoginInfo(systemManager.getId());

        String token = IdUtil.fastSimpleUUID();
        long tokenTTL = systemConfigCache.getLong("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME", 1440L);
        redisUtil.set("strix:system:manager:login_token:login:id_" + systemManager.getId(), token, tokenTTL, TimeUnit.MINUTES);
        redisUtil.set("strix:system:manager:login_token:token:" + token, loginSystemManager, tokenTTL, TimeUnit.MINUTES);

        // 合并菜单权限
        List<String> permissionKeys = new ArrayList<>();
        permissionKeys.addAll(loginSystemManager.getMenusKeys());
        permissionKeys.addAll(loginSystemManager.getPermissionKeys());

        return RetBuilder.success(
                new SystemLoginResp(
                        new SystemLoginResp.LoginManagerInfo(
                                systemManager.getId(), systemManager.getNickname(), systemManager.getType(), permissionKeys
                        ),
                        token,
                        LocalDateTime.now().plusMinutes(tokenTTL)
                ));
    }

    /**
     * 重新获取Token
     */
    @PostMapping("renewToken")
    public RetResult<SystemLoginResp> renewToken() {
        String loginSystemManagerId = loginManagerId();
        Assert.hasText(loginSystemManagerId, "请重新登陆");
        SystemManager systemManager = systemManagerService.getById(loginSystemManagerId);

        Object oldTokenObj = redisUtil.get("strix:system:manager:login_token:login:id_" + systemManager.getId());
        Assert.notNull(oldTokenObj, "旧token已失效，请重新登陆");
        Object oldTokenInfoObj = redisUtil.get("strix:system:manager:login_token:token:" + oldTokenObj);
        Assert.notNull(oldTokenInfoObj, "旧token已失效，请重新登陆");
        LoginSystemManager loginSystemManager = (LoginSystemManager) oldTokenInfoObj;
        Assert.notNull(loginSystemManager, "旧token已失效，请重新登陆");

        long effectiveTime = systemConfigCache.getLong("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME", 525600L);
        redisUtil.setExpire("strix:system:manager:login_token:login:id_" + systemManager.getId(), effectiveTime, TimeUnit.MINUTES);
        redisUtil.setExpire("strix:system:manager:login_token:token:" + oldTokenObj, effectiveTime, TimeUnit.MINUTES);

        // 合并菜单权限
        List<String> permissionKeys = new ArrayList<>();
        permissionKeys.addAll(loginSystemManager.getMenusKeys());
        permissionKeys.addAll(loginSystemManager.getPermissionKeys());

        return RetBuilder.success(
                new SystemLoginResp(
                        new SystemLoginResp.LoginManagerInfo(
                                systemManager.getId(), systemManager.getNickname(), systemManager.getType(), permissionKeys
                        ),
                        oldTokenObj.toString(),
                        LocalDateTime.now().plusMinutes(effectiveTime)
                ));
    }

    /**
     * 获取系统菜单
     */
    @GetMapping("menus")
    public RetResult<SystemMenuResp> getMenuList() {
        List<String> systemMenuKeys = SecurityUtils.getManagerMenuKeys();
        Assert.notEmpty(systemMenuKeys, "当前账号无菜单权限");

        List<SystemMenu> systemMenus = systemMenusService.lambdaQuery()
                .in(SystemMenu::getKey, systemMenuKeys)
                .orderByAsc(SystemMenu::getSortValue)
                .list();
        Assert.notEmpty(systemMenus, "当前账号无可用菜单权限");

        return RetBuilder.success(new SystemMenuResp(systemMenus));
    }

}
