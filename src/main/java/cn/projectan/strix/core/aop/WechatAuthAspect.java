package cn.projectan.strix.core.aop;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.model.annotation.NeedWechatAuth;
import cn.projectan.strix.model.db.WechatUser;
import cn.projectan.strix.service.WechatUserService;
import cn.projectan.strix.utils.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * @author 安炯奕
 * @date 2021/8/26 11:55
 * @deprecated 已使用 Spring Security 替代
 */
@Slf4j
//@Aspect
//@Order(3)
//@Component
@Deprecated
public class WechatAuthAspect {

    @Autowired
    private WechatUserService wechatUserService;
    @Autowired
    private RedisUtil redisUtil;

    @Pointcut("execution(public * cn.projectan.strix..controller.wechat.*.*(..))")
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
        NeedWechatAuth needWechatAuth = signature.getMethod().getAnnotation(NeedWechatAuth.class);
        if (needWechatAuth == null) {
            return pjp.proceed();
        }
        // 获取请求中的PathVariable参数
        Map<String, Object> uriAttribute = (Map<String, Object>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String wechatConfigId = MapUtil.getStr(uriAttribute, "wechatConfigId");

        // 获取请求头中的token
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) {
            // 请求头中没有附带token 直接拒绝请求
            return RetMarker.makeErrRsp(RetCode.NOT_LOGIN, "您还没有授权微信登录，请授权后再使用");
        }
        if ("StrixDevTestToken00195342366901".equals(token)) {
            // 开发专用token
            WechatUser wechatUser = wechatUserService.getById("1");
            request.setAttribute("_LoginWechatUser", wechatUser);
            return pjp.proceed();
        }

        // 检查Redis中是否包含此token 并读取出缓存的信息
        Object loginInfo = redisUtil.get("strix:wxUserLogin:" + wechatConfigId + ":tokenToWxUser:" + token);
        if (loginInfo == null) {
            return RetMarker.makeErrRsp(RetCode.NOT_LOGIN, "微信授权状态失效，请重新授权后再使用");
        }
        WechatUser wechatUser = (WechatUser) loginInfo;
        // 保存至request对象中 方便BaseController层读取登录用户信息
        request.setAttribute("_LoginWechatUser", wechatUser);
        return pjp.proceed();
    }

}
