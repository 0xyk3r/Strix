package cn.projectan.strix.core.aop.aspect;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.constant.system.StrixPasswordConst;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.http.ServletUtils;
import cn.projectan.strix.util.system.ApiSignUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * API 安全校验切面
 * <p>
 * POST 请求：基于解密后的原始请求体字符串验证签名，消除 DTO 序列化差异。
 * GET 请求：基于排序后的查询参数验证签名。
 *
 * @author ProjectAn
 * @since 2026/3/20 23:50
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class ApiSecurityCheckAspect {

    private final ApiSignUtil apiSignUtil;

    @SuppressWarnings("EmptyMethod")
    @Pointcut("execution(public * cn.projectan..controller..*(..))")
    public void controller() {
    }

    @Around("controller()")
    public Object handle(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attributes = ServletUtils.getRequestAttributes();
        if (attributes == null) {
            return pjp.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        MethodSignature signature = (MethodSignature) pjp.getSignature();

        // 非加密接口直接放行
        if (signature.getMethod().isAnnotationPresent(IgnoreEncryption.class) ||
                signature.getMethod().getDeclaringClass().isAnnotationPresent(IgnoreEncryption.class)) {
            return pjp.proceed();
        }

        // 填写了密码直接放行
        if (StrixPasswordConst.IGNORE_ENCRYPTION.equals(request.getHeader("ss-pwd"))) {
            return pjp.proceed();
        }

        // 判断请求是否已经过 DecodeRequestBodyAdvice 处理
        boolean security = Optional.ofNullable(request.getAttribute("Strix-Security"))
                .map(String::valueOf)
                .map(Boolean::parseBoolean)
                .orElse(true);
        if (!security) {
            return RetBuilder.error(RetCode.BAD_REQUEST, I18nUtil.get("error.badRequest") + "1");
        }

        String url = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
        );
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        if (!StringUtils.hasText(sign) || !StringUtils.hasText(timestamp)) {
            return RetBuilder.error(RetCode.BAD_REQUEST, I18nUtil.get("error.badRequest") + "2");
        }

        // 校验时间戳 60s 内有效
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return RetBuilder.error(RetCode.BAD_REQUEST, I18nUtil.get("error.badRequest") + "3");
        }
        if (System.currentTimeMillis() - ts > 1000 * 60) {
            return RetBuilder.error(RetCode.BAD_REQUEST, I18nUtil.get("error.badRequest") + "3");
        }

        // 校验签名
        boolean signValid;
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            // GET 请求：排序后的查询参数签名
            final Map<String, Object> paramsMap = new TreeMap<>(ServletUtils.getRequestParams(request));
            // 过滤参数中的空字符串
            paramsMap.entrySet().removeIf(entry -> entry.getValue() == null || (entry.getValue() instanceof String && !StringUtils.hasText((String) entry.getValue())));
            signValid = apiSignUtil.verifySignFromParams(paramsMap, url, timestamp, sign);
        } else {
            // POST 请求：基于原始请求体字符串签名
            String rawBody = (String) request.getAttribute("Strix-Decrypted-Body");
            signValid = apiSignUtil.verifySign(rawBody, url, timestamp, sign);
        }

        if (!signValid) {
            return RetBuilder.error(RetCode.BAD_REQUEST, I18nUtil.get("error.badRequest") + "4");
        }

        return pjp.proceed();
    }

}
