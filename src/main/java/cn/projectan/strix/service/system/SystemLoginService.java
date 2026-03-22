package cn.projectan.strix.service.system;

import cn.projectan.strix.core.cache.system.SystemConfigCache;
import cn.projectan.strix.core.captcha.CaptchaService;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.constant.system.StrixRedisKeyConst;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.dict.system.SystemManagerStatus;
import cn.projectan.strix.model.other.system.captcha.CaptchaData;
import cn.projectan.strix.model.request.system.login.SystemLoginReq;
import cn.projectan.strix.model.response.system.login.SystemManagerLoginResp;
import cn.projectan.strix.util.common.RedisUtil;
import cn.projectan.strix.util.crypto.StrixSM3Util;
import cn.projectan.strix.util.http.ServletUtil;
import cn.projectan.strix.util.http.TokenUtil;
import cn.projectan.strix.util.ip.IpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统登录服务
 *
 * @author ProjectAn
 */
@Service
@RequiredArgsConstructor
public class SystemLoginService {

    private final SystemManagerService systemManagerService;
    private final CaptchaService captchaService;
    private final SystemConfigCache systemConfigCache;
    private final TokenSessionService tokenSessionService;
    private final RedisUtil redisUtil;

    private static final int DEFAULT_MAX_LOGIN_FAILURES = 5;
    private static final long DEFAULT_LOGIN_LOCK_MINUTES = 30;

    /**
     * 管理员登录
     */
    public RetResult<SystemManagerLoginResp> login(SystemLoginReq req) {
        // 验证码校验
        Assert.hasText(req.getCaptchaVerification(), "行为验证不通过，请重新验证");
        CaptchaData captchaDataVO = new CaptchaData();
        captchaDataVO.setCaptchaVerification(req.getCaptchaVerification());
        RetResult<Void> captchaResult = captchaService.verification(captchaDataVO);
        Assert.isTrue(captchaResult.getCode() == RetCode.SUCCESS, "行为验证不通过，请重新验证");

        // 基于客户端 IP 的登录失败次数限制
        String clientIp = IpUtils.getIpAddr(ServletUtil.getRequest());
        String failureKey = StrixRedisKeyConst.STR_LOGIN_FAILURE_IP_PREFIX + clientIp;
        int maxLoginFailures = systemConfigCache.getLong("SYSTEM_MANAGER_MAX_LOGIN_FAILURES", (long) DEFAULT_MAX_LOGIN_FAILURES).intValue();
        Object failureCountObj = redisUtil.get(failureKey);
        if (failureCountObj instanceof Number failureCount && failureCount.intValue() >= maxLoginFailures) {
            long remainSeconds = redisUtil.getExpire(failureKey);
            long remainMinutes = Math.max(1, remainSeconds / 60);
            return RetBuilder.build(RetCode.BAD_REQUEST, "登录失败次数过多，请 " + remainMinutes + " 分钟后再试");
        }

        SystemManager systemManager = systemManagerService.getByLoginName(req.getLoginName());

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
            tokenSessionService.invalidateManagerSession(systemManager.getId());
        }

        LoginSystemManager loginSystemManager = systemManagerService.getLoginInfo(systemManager.getId());

        long tokenTTL = systemConfigCache.getLong("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME", 1440L);
        String token = tokenSessionService.createManagerSession(systemManager.getId(), loginSystemManager, tokenTTL);

        return RetBuilder.success(buildLoginResp(systemManager, loginSystemManager, token, tokenTTL));
    }

    /**
     * 续期 Token
     */
    public RetResult<SystemManagerLoginResp> renewToken(String managerId) {
        Assert.hasText(managerId, "请重新登陆");
        SystemManager systemManager = systemManagerService.getById(managerId);

        LoginSystemManager loginSystemManager = tokenSessionService.getManagerLoginInfoById(managerId);
        Assert.notNull(loginSystemManager, "旧token已失效，请重新登陆");

        long effectiveTime = systemConfigCache.getLong("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME", 1440L);
        tokenSessionService.refreshManagerSessionTTL(systemManager.getId(), effectiveTime);

        String currentToken = TokenUtil.resolveToken(ServletUtil.getRequest());

        return RetBuilder.success(buildLoginResp(systemManager, loginSystemManager, currentToken, effectiveTime));
    }

    /**
     * 构建登录响应
     */
    private SystemManagerLoginResp buildLoginResp(SystemManager systemManager, LoginSystemManager loginSystemManager,
                                                  String token, long tokenTTLMinutes) {
        List<String> permissionKeys = new ArrayList<>();
        permissionKeys.addAll(loginSystemManager.getMenusKeys());
        permissionKeys.addAll(loginSystemManager.getPermissionKeys());

        return new SystemManagerLoginResp(
                new SystemManagerLoginResp.LoginManagerInfo(
                        systemManager.getId(), systemManager.getNickname(), systemManager.getType(), systemManager.getRegionId(), permissionKeys
                ),
                token,
                LocalDateTime.now().plusMinutes(tokenTTLMinutes)
        );
    }

    /**
     * 记录登录失败次数（使用原子递增避免竞态条件）
     */
    private void recordLoginFailure(String failureKey) {
        long loginLockMinutes = systemConfigCache.getLong("SYSTEM_MANAGER_LOGIN_LOCK_MINUTES", DEFAULT_LOGIN_LOCK_MINUTES);
        long count = redisUtil.incr(failureKey);
        if (count == 1) {
            redisUtil.setExpire(failureKey, loginLockMinutes, TimeUnit.MINUTES);
        }
    }

}
