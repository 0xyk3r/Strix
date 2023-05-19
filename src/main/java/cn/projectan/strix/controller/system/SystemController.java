package cn.projectan.strix.controller.system;

import cn.hutool.core.util.IdUtil;
import cn.projectan.captcha.model.common.ResponseModel;
import cn.projectan.captcha.model.vo.CaptchaVO;
import cn.projectan.captcha.service.CaptchaService;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ramcache.SystemSettingCache;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.constant.SystemManagerStatus;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.request.system.SystemLoginReq;
import cn.projectan.strix.model.response.system.SystemLoginResp;
import cn.projectan.strix.model.response.system.SystemMenuResp;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemRegionService;
import cn.projectan.strix.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 安炯奕
 * @date 2021/5/12 18:39
 */
@Slf4j
@RestController
@RequestMapping("system")
public class SystemController extends BaseSystemController {

    @Autowired
    private SystemManagerService systemManagerService;

    @Autowired
    private SystemRegionService systemRegionService;

    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private SystemSettingCache systemSettingCache;
    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("login")
    public RetResult<Object> login(@RequestBody SystemLoginReq systemLoginReq) {
        // 验证码校验
        Assert.hasText(systemLoginReq.getCaptchaVerification(), "行为验证不通过，请重新验证");
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(systemLoginReq.getCaptchaVerification());
        ResponseModel response = captchaService.verification(captchaVO);
        if (!response.isSuccess()) {
            return RetMarker.makeErrRsp("行为验证不通过，请重新验证");
        }

        QueryWrapper<SystemManager> loginQueryWrapper = new QueryWrapper<>();
        loginQueryWrapper.eq("login_name", systemLoginReq.getLoginName());
        SystemManager systemManager = systemManagerService.getOne(loginQueryWrapper);
        Assert.notNull(systemManager, "账号或密码错误");
        Assert.isTrue(systemManager.getManagerStatus() == SystemManagerStatus.NORMAL, "该管理用户已被禁止使用");
        Assert.isTrue(systemManager.getLoginPassword().equals(systemLoginReq.getLoginPassword()), "账号或密码错误");

        if ("0".equals(systemSettingCache.get("SYSTEM_MANAGER_SUPPORT_MULTIPLE_LOGIN"))) {
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
        String et = systemSettingCache.get("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME");
        if (StringUtils.hasText(et)) {
            effectiveTime = Long.parseLong(et);
        }

        LoginSystemManager loginSystemManager = systemManagerService.getLoginInfo(systemManager.getId());

        String token = IdUtil.fastSimpleUUID();
        redisUtil.set("strix:system:manager:login_token:login:id_" + systemManager.getId(), token, effectiveTime, TimeUnit.MINUTES);
        redisUtil.set("strix:system:manager:login_token:token:" + token, loginSystemManager, effectiveTime, TimeUnit.MINUTES);

        return RetMarker.makeSuccessRsp(new SystemLoginResp(
                new SystemLoginResp.LoginManagerInfo(systemManager.getId(), systemManager.getNickname(), systemManager.getManagerType()),
                token, LocalDateTime.now().plusMinutes(effectiveTime)));
    }

    @PostMapping("checkToken")
    public RetResult<SystemLoginResp> checkToken() {
        SystemManager systemManager = getSystemManager();
        LocalDateTime tokenExpire = redisUtil.getExpireDateTime("strix:system:manager:login_token:login:id_" + systemManager.getId());

        return RetMarker.makeSuccessRsp(new SystemLoginResp(
                new SystemLoginResp.LoginManagerInfo(systemManager.getId(), systemManager.getNickname(), systemManager.getManagerType()),
                "original token", tokenExpire));
    }

    @PostMapping("renewToken")
    public RetResult<SystemLoginResp> renewToken() {
        SystemManager systemManager = getSystemManager();
        systemManager = systemManagerService.getById(systemManager.getId());
        Object oldTokenObj = redisUtil.get("strix:system:manager:login_token:login:id_" + systemManager.getId());
        Assert.notNull(oldTokenObj, "旧token已失效，请重新登陆");
        long effectiveTime = 1440L;
        String et = systemSettingCache.get("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME");
        if (StringUtils.hasText(et)) {
            effectiveTime = Long.parseLong(et);
        }
        redisUtil.setExpire("strix:system:manager:login_token:login:id_" + systemManager.getId(), effectiveTime, TimeUnit.MINUTES);
        redisUtil.setExpire("strix:system:manager:login_token:token:" + oldTokenObj, effectiveTime, TimeUnit.MINUTES);

        return RetMarker.makeSuccessRsp(new SystemLoginResp(
                new SystemLoginResp.LoginManagerInfo(systemManager.getId(), systemManager.getNickname(), systemManager.getManagerType()),
                oldTokenObj.toString(), LocalDateTime.now().plusMinutes(effectiveTime)));
    }

    @GetMapping("menus")
    public RetResult<SystemMenuResp> getMenuList() {
        List<SystemMenu> systemMenuList = systemManagerService.getAllSystemMenuByManager(getLoginManagerId());
        Assert.notEmpty(systemMenuList, "当前账号无菜单权限");
        SystemMenuResp resp = new SystemMenuResp(systemMenuList);
        return RetMarker.makeSuccessRsp(resp);
    }

}
