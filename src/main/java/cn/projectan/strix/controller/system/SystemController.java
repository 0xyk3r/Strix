package cn.projectan.strix.controller.system;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.system.SystemConfigCache;
import cn.projectan.strix.core.captcha.CaptchaService;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import cn.projectan.strix.model.constant.system.StrixRedisKeyConst;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.db.system.SystemMenu;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.dict.system.SystemManagerStatus;
import cn.projectan.strix.model.other.system.captcha.CaptchaDataVO;
import cn.projectan.strix.model.request.system.login.SystemLoginReq;
import cn.projectan.strix.model.response.system.SystemMenuResp;
import cn.projectan.strix.model.response.system.login.SystemManagerLoginResp;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.service.system.SystemMenuService;
import cn.projectan.strix.util.common.RedisUtil;
import cn.projectan.strix.util.crypto.StrixSM3Util;
import cn.projectan.strix.util.http.ServletUtils;
import cn.projectan.strix.util.ip.IpUtils;
import cn.projectan.strix.util.system.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
     * 登录失败最大尝试次数
     */
    private static final int MAX_LOGIN_FAILURES = 5;

    /**
     * 登录锁定时间（分钟）
     */
    private static final long LOGIN_LOCK_MINUTES = 30;

    /**
     * 系统登录
     */
    @Anonymous
    @PostMapping("login")
    @StrixLog(operationGroup = "系统登录", operationName = "系统登录", operationType = SystemLogOperType.LOGIN)
    public RetResult<SystemManagerLoginResp> login(@RequestBody SystemLoginReq req) {
        // 验证码校验
        Assert.hasText(req.getCaptchaVerification(), "行为验证不通过，请重新验证");
        CaptchaDataVO captchaDataVO = new CaptchaDataVO();
        captchaDataVO.setCaptchaVerification(req.getCaptchaVerification());
        RetResult<Void> captchaResult = captchaService.verification(captchaDataVO);
        Assert.isTrue(captchaResult.getCode() == RetCode.SUCCESS, "行为验证不通过，请重新验证");

        // 基于客户端 IP 的登录失败次数限制
        String clientIp = IpUtils.getIpAddr(ServletUtils.getRequest());
        String failureKey = StrixRedisKeyConst.STR_LOGIN_FAILURE_IP_PREFIX + clientIp;
        Object failureCountObj = redisUtil.get(failureKey);
        if (failureCountObj instanceof Number failureCount && failureCount.intValue() >= MAX_LOGIN_FAILURES) {
            long remainSeconds = redisUtil.getExpire(failureKey);
            long remainMinutes = Math.max(1, remainSeconds / 60);
            return RetBuilder.build(RetCode.BAD_REQUEST, "登录失败次数过多，请 " + remainMinutes + " 分钟后再试");
        }

        SystemManager systemManager = systemManagerService.lambdaQuery()
                .eq(SystemManager::getLoginName, req.getLoginName())
                .one();

        if (systemManager == null || !StrixSM3Util.matches(req.getLoginPassword(), systemManager.getLoginPassword())) {
            recordLoginFailure(failureKey);
            return RetBuilder.build(RetCode.BAD_REQUEST, "账号或密码错误");
        }

        Assert.isTrue(systemManager.getStatus() == SystemManagerStatus.NORMAL, "该管理用户已停用");

        // 登录成功，清除失败计数
        redisUtil.del(failureKey);

        // 检查是否支持多点登录
        Boolean supportMultipleLogin = systemConfigCache.getBoolean("SYSTEM_MANAGER_SUPPORT_MULTIPLE_LOGIN", false);
        if (!supportMultipleLogin) {
            // 使上次登录生成的Token失效
            Object existToken = redisUtil.get(LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + systemManager.getId());
            if (existToken != null) {
                redisUtil.del(LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX + existToken);
                redisUtil.del(LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + systemManager.getId());
            }
        }

        LoginSystemManager loginSystemManager = systemManagerService.getLoginInfo(systemManager.getId());

        String token = IdUtil.fastSimpleUUID();
        long tokenTTL = systemConfigCache.getLong("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME", 1440L);
        redisUtil.set(LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + systemManager.getId(), token, tokenTTL, TimeUnit.MINUTES);
        redisUtil.set(LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX + token, loginSystemManager, tokenTTL, TimeUnit.MINUTES);

        // 合并菜单权限
        List<String> permissionKeys = new ArrayList<>();
        permissionKeys.addAll(loginSystemManager.getMenusKeys());
        permissionKeys.addAll(loginSystemManager.getPermissionKeys());

        return RetBuilder.success(
                new SystemManagerLoginResp(
                        new SystemManagerLoginResp.LoginManagerInfo(
                                systemManager.getId(), systemManager.getNickname(), systemManager.getType(), systemManager.getRegionId(), permissionKeys
                        ),
                        token,
                        LocalDateTime.now().plusMinutes(tokenTTL)
                ));
    }

    /**
     * 记录登录失败次数
     */
    private void recordLoginFailure(String failureKey) {
        Object current = redisUtil.get(failureKey);
        int count = (current instanceof Number n) ? n.intValue() + 1 : 1;
        redisUtil.set(failureKey, count, LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 重新获取Token
     */
    @PostMapping("renewToken")
    public RetResult<SystemManagerLoginResp> renewToken() {
        String loginSystemManagerId = loginManagerId();
        Assert.hasText(loginSystemManagerId, "请重新登陆");
        SystemManager systemManager = systemManagerService.getById(loginSystemManagerId);

        Object oldTokenObj = redisUtil.get(LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + systemManager.getId());
        Assert.notNull(oldTokenObj, "旧token已失效，请重新登陆");
        Object oldTokenInfoObj = redisUtil.get(LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX + oldTokenObj);
        Assert.notNull(oldTokenInfoObj, "旧token已失效，请重新登陆");
        LoginSystemManager loginSystemManager = (LoginSystemManager) oldTokenInfoObj;
        Assert.notNull(loginSystemManager, "旧token已失效，请重新登陆");

        long effectiveTime = systemConfigCache.getLong("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME", 1440L);
        redisUtil.setExpire(LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + systemManager.getId(), effectiveTime, TimeUnit.MINUTES);
        redisUtil.setExpire(LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX + oldTokenObj, effectiveTime, TimeUnit.MINUTES);

        // 合并菜单权限
        List<String> permissionKeys = new ArrayList<>();
        permissionKeys.addAll(loginSystemManager.getMenusKeys());
        permissionKeys.addAll(loginSystemManager.getPermissionKeys());

        return RetBuilder.success(
                new SystemManagerLoginResp(
                        new SystemManagerLoginResp.LoginManagerInfo(
                                systemManager.getId(), systemManager.getNickname(), systemManager.getType(), systemManager.getRegionId(), permissionKeys
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
        List<String> systemMenuKeys = Optional.ofNullable(SecurityUtils.getSystemManagerLoginInfo()).map(LoginSystemManager::getMenusKeys).orElse(null);
        Assert.notEmpty(systemMenuKeys, "当前账号无菜单权限");

        List<SystemMenu> systemMenus = systemMenusService.lambdaQuery()
                .in(SystemMenu::getKey, systemMenuKeys)
                .orderByAsc(SystemMenu::getSortValue)
                .list();
        Assert.notEmpty(systemMenus, "当前账号无可用菜单权限");

        return RetBuilder.success(new SystemMenuResp(systemMenus));
    }

}
