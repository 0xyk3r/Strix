package cn.projectan.strix.core.aop.aspect;

import cn.projectan.strix.core.ramcache.SystemConfigCache;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.model.annotation.NeedSystemPermission;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemRegionService;
import cn.projectan.strix.utils.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统管理用户权限统一校验处理
 *
 * @author 安炯奕
 * @date 2021/5/13 13:30
 * @deprecated 已使用 Spring Security 替代
 */
@Slf4j
//@Aspect
//@Order(2)
//@Component
@Deprecated
public class SystemPermissionAspect {

    //    @Autowired
    private SystemManagerService systemManagerService;
    //    @Autowired
    private SystemRegionService systemRegionService;

    //    @Autowired
    private RedisUtil redisUtil;
    //    @Autowired
    private SystemConfigCache systemConfigCache;

    @Pointcut("execution(public * cn.projectan.strix..controller.system..*.*(..))")
    public void controller() {
    }

    @Around("controller()")
    public Object handle(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return pjp.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        NeedSystemPermission needSystemPermission = signature.getMethod().getAnnotation(NeedSystemPermission.class);
        if (needSystemPermission == null) {
            return pjp.proceed();
        }
        // 获取请求头中的token
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) {
            // 请求头中没有附带token 直接拒绝请求
            return RetMarker.makeErrRsp(RetCode.NOT_LOGIN, "无权访问");
        }
        List<String> systemManagerRegionIdList = new ArrayList<>();
        if ("StrixDevTestToken00195342366901".equals(token)) {
            // 开发专用token
            SystemManager systemManager = systemManagerService.getById("anjiongyi");
            request.setAttribute("_LoginSystemManager", systemManager);
            if (StringUtils.hasText(systemManager.getRegionId())) {
                systemManagerRegionIdList = systemRegionService.getChildrenIdList(systemManager.getRegionId());
            }
            request.setAttribute("_LoginSystemManagerRegionIdList", systemManagerRegionIdList);
            return pjp.proceed();
        }

        // 检查Redis中是否包含此token 并读取出缓存的信息
        Object loginInfo = redisUtil.get("strix:system:manager:login_token:token:" + token);
        if (loginInfo == null) {
            return RetMarker.makeErrRsp(RetCode.NOT_LOGIN, "Token不存在或已失效，请重新登录");
        }
        SystemManager systemManager = (SystemManager) loginInfo;
//        // 更新登录用户信息
//        systemManager = systemManagerService.getById(systemManager.getId());
        // 保存至request对象中 方便BaseController层读取登录用户信息
        request.setAttribute("_LoginSystemManager", systemManager);
        if (StringUtils.hasText(systemManager.getRegionId())) {
            systemManagerRegionIdList = systemRegionService.getChildrenIdList(systemManager.getRegionId());
        }
        request.setAttribute("_LoginSystemManagerRegionIdList", systemManagerRegionIdList);
//        // token续期并更新用户信息至缓存中
//        long effectiveTime = 1440L;
//        String et = systemSettingCache.get("SYSTEM_MANAGER_LOGIN_EFFECTIVE_TIME");
//        if (StringUtils.hasText(et)) {
//            effectiveTime = Long.parseLong(et);
//        }
//        redisUtil.set("strix:system:manager:login_token:login:id_" + systemManager.getId(), token, effectiveTime, TimeUnit.MINUTES);
//        redisUtil.set("strix:system:manager:login_token:token:" + token, systemManager, effectiveTime, TimeUnit.MINUTES);

        if (!StringUtils.hasText(needSystemPermission.value())) {
            // 如果没有需要的权限key 那么代表访问该api仅需要登录状态即可 故直接放行
            return pjp.proceed();
        }

        // 查询用户权限
        List<SystemPermission> permissionList = systemManagerService.getAllSystemPermissionByManager(systemManager.getId());
        for (SystemPermission p : permissionList) {
            if (p.getPermissionKey().equalsIgnoreCase(needSystemPermission.value())) {
                if (p.getPermissionType() == 2) {
                    // 有读写权 直接放行
                    return pjp.proceed();
                } else if (p.getPermissionType() == 1) {
                    // 仅有只读权 判断此接口是否需要写入权限
                    if (needSystemPermission.isEdit()) {
                        return RetMarker.makeErrRsp(RetCode.NOT_PERMISSION, "您的账号仅拥有只读权限");
                    }
                    return pjp.proceed();
                }
            }
        }

        return RetMarker.makeErrRsp(RetCode.NOT_PERMISSION, "您的账号无访问权限");
    }
}
