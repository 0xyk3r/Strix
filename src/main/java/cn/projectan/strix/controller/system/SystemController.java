package cn.projectan.strix.controller.system;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ramcache.SystemSettingCache;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.NeedSystemPermission;
import cn.projectan.strix.model.constant.SystemManagerStatus;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.request.system.SystemLoginReq;
import cn.projectan.strix.model.response.system.SystemLoginResp;
import cn.projectan.strix.model.response.system.SystemMenuResp;
import cn.projectan.strix.service.SystemManagerService;
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
    private SystemSettingCache systemSettingCache;
    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("login")
    public RetResult<SystemLoginResp> login(@RequestBody SystemLoginReq systemLoginReq) {
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

        String token = IdUtil.fastSimpleUUID();
        redisUtil.set("strix:system:manager:login_token:login:id_" + systemManager.getId(), token, effectiveTime, TimeUnit.MINUTES);
        redisUtil.set("strix:system:manager:login_token:token:" + token, systemManager, effectiveTime, TimeUnit.MINUTES);

        return RetMarker.makeSuccessRsp(new SystemLoginResp(
                new SystemLoginResp.LoginManagerInfo(systemManager.getId(), systemManager.getNickname(), systemManager.getManagerType()),
                token, LocalDateTime.now().plusMinutes(effectiveTime)));
    }

    @NeedSystemPermission
    @PostMapping("checkToken")
    public RetResult<SystemLoginResp> checkToken() {
        SystemManager systemManager = getLoginManager();
        LocalDateTime tokenExpire = redisUtil.getExpireDateTime("strix:system:manager:login_token:login:id_" + systemManager.getId());

        return RetMarker.makeSuccessRsp(new SystemLoginResp(
                new SystemLoginResp.LoginManagerInfo(systemManager.getId(), systemManager.getNickname(), systemManager.getManagerType()),
                "original token", tokenExpire));
    }

    @NeedSystemPermission
    @PostMapping("renewToken")
    public RetResult<SystemLoginResp> renewToken() {
        SystemManager systemManager = getLoginManager();
        systemManager = systemManagerService.getById(systemManager.getId());
        Object oldTokenObj = redisUtil.get("strix:system:manager:login_token:login:id_" + systemManager.getId());
        Assert.notNull(oldTokenObj, "旧token已失效，请重新登陆");
        long effectiveTime = 1440L;
        String et = systemSettingCache.get("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME");
        if (StringUtils.hasText(et)) {
            effectiveTime = Long.parseLong(et);
        }
        redisUtil.set("strix:system:manager:login_token:login:id_" + systemManager.getId(), oldTokenObj.toString(), effectiveTime, TimeUnit.MINUTES);
        redisUtil.set("strix:system:manager:login_token:token:" + oldTokenObj.toString(), systemManager, effectiveTime, TimeUnit.MINUTES);

        return RetMarker.makeSuccessRsp(new SystemLoginResp(
                new SystemLoginResp.LoginManagerInfo(systemManager.getId(), systemManager.getNickname(), systemManager.getManagerType()),
                oldTokenObj.toString(), LocalDateTime.now().plusMinutes(effectiveTime)));
    }

    @NeedSystemPermission
    @GetMapping("menus")
    public RetResult<SystemMenuResp> getMenuList() {
        List<SystemMenu> systemMenuList = systemManagerService.getAllSystemMenuByManager(getLoginManagerId());
        Assert.notEmpty(systemMenuList, "当前账号无菜单权限");
        SystemMenuResp resp = new SystemMenuResp(systemMenuList);
        return RetMarker.makeSuccessRsp(resp);
    }

}
