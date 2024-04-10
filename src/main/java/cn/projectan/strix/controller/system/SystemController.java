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
import cn.projectan.strix.utils.RedisUtil;
import cn.projectan.strix.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ProjectAn
 * @date 2021/5/12 18:39
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

    @Anonymous
    @PostMapping("login")
    @StrixLog(operationGroup = "系统登录", operationName = "系统登录", operationType = SysLogOperType.LOGIN)
    public RetResult<Object> login(@RequestBody SystemLoginReq req) {
        // 验证码校验
        Assert.hasText(req.getCaptchaVerification(), "行为验证不通过，请重新验证");
        CaptchaInfoVO captchaInfoVO = new CaptchaInfoVO();
        captchaInfoVO.setCaptchaVerification(req.getCaptchaVerification());
        StrixCaptchaResp strixCaptchaResp = captchaService.verification(captchaInfoVO);
        if (!strixCaptchaResp.isSuccess()) {
            return RetBuilder.error("行为验证不通过，请重新验证");
        }

        QueryWrapper<SystemManager> loginQueryWrapper = new QueryWrapper<>();
        loginQueryWrapper.eq("login_name", req.getLoginName());
        SystemManager systemManager = systemManagerService.getOne(loginQueryWrapper);
        Assert.notNull(systemManager, "账号或密码错误");
        Assert.isTrue(systemManager.getStatus() == SystemManagerStatus.NORMAL, "该管理用户已被禁止使用");
        Assert.isTrue(systemManager.getLoginPassword().equals(req.getLoginPassword()), "账号或密码错误");

        if ("0".equals(systemConfigCache.get("SYSTEM_MANAGER_SUPPORT_MULTIPLE_LOGIN"))) {
            // 检查该账号上次登录是否还没有超时
            Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + systemManager.getId());
            if (existToken != null) {
                // 使上次登录生成的Token失效
                redisUtil.del("strix:system:manager:login_token:token:" + existToken);
                redisUtil.del("strix:system:manager:login_token:login:id_" + systemManager.getId());
            }
        }
        // 获取存储时间并存储Token
        long effectiveTime = 1440L;
        String et = systemConfigCache.get("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME");
        if (StringUtils.hasText(et)) {
            effectiveTime = Long.parseLong(et);
        }

        LoginSystemManager loginSystemManager = systemManagerService.getLoginInfo(systemManager.getId());

        String token = IdUtil.fastSimpleUUID();
        redisUtil.set("strix:system:manager:login_token:login:id_" + systemManager.getId(), token, effectiveTime, TimeUnit.MINUTES);
        redisUtil.set("strix:system:manager:login_token:token:" + token, loginSystemManager, effectiveTime, TimeUnit.MINUTES);

        return RetBuilder.success(new SystemLoginResp(
                new SystemLoginResp.LoginManagerInfo(systemManager.getId(), systemManager.getNickname(), systemManager.getType()),
                token, LocalDateTime.now().plusMinutes(effectiveTime)));
    }

    @PostMapping("checkToken")
    public RetResult<SystemLoginResp> checkToken() {
        SystemManager systemManager = loginManager();
        long tokenTTL = redisUtil.getExpire("strix:system:manager:login_token:login:id_" + systemManager.getId());

        return RetBuilder.success(new SystemLoginResp(
                new SystemLoginResp.LoginManagerInfo(systemManager.getId(), systemManager.getNickname(), systemManager.getType()),
                "original token", LocalDateTime.now().plusMinutes(tokenTTL)));
    }

    @PostMapping("renewToken")
    public RetResult<SystemLoginResp> renewToken() {
        SystemManager systemManager = loginManager();
        systemManager = systemManagerService.getById(systemManager.getId());
        Object oldTokenObj = redisUtil.get("strix:system:manager:login_token:login:id_" + systemManager.getId());
        Assert.notNull(oldTokenObj, "旧token已失效，请重新登陆");
        long effectiveTime = 1440L;
        String et = systemConfigCache.get("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME");
        if (StringUtils.hasText(et)) {
            effectiveTime = Long.parseLong(et);
        }
        redisUtil.setExpire("strix:system:manager:login_token:login:id_" + systemManager.getId(), effectiveTime, TimeUnit.MINUTES);
        redisUtil.setExpire("strix:system:manager:login_token:token:" + oldTokenObj, effectiveTime, TimeUnit.MINUTES);

        return RetBuilder.success(new SystemLoginResp(
                new SystemLoginResp.LoginManagerInfo(systemManager.getId(), systemManager.getNickname(), systemManager.getType()),
                oldTokenObj.toString(), LocalDateTime.now().plusMinutes(effectiveTime)));
    }

    @GetMapping("menus")
    public RetResult<SystemMenuResp> getMenuList() {
        List<String> systemMenuKeys = SecurityUtils.getManagerMenuKeys();
        Assert.notEmpty(systemMenuKeys, "当前账号无菜单权限");

        QueryWrapper<SystemMenu> qw = new QueryWrapper<>();
        qw.in("`key`", systemMenuKeys);
        qw.orderByAsc("sort_value");
        List<SystemMenu> systemMenus = systemMenusService.list(qw);
        Assert.notEmpty(systemMenus, "当前账号无可用菜单权限");

        return RetBuilder.success(new SystemMenuResp(systemMenus));
    }

}
