package cn.projectan.strix.core.aop.aspect;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.utils.ApiSignUtil;
import cn.projectan.strix.utils.I18nUtil;
import cn.projectan.strix.utils.ServletUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Api 统一安全校验
 *
 * @author ProjectAn
 * @date 2021/5/7 18:20
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ApiSecurityCheckAspect {

    private final ObjectMapper objectMapper;

    public ApiSecurityCheckAspect() {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }

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
        if (signature.getMethod().isAnnotationPresent(IgnoreDataEncryption.class) || signature.getMethod().getDeclaringClass().isAnnotationPresent(IgnoreDataEncryption.class)) {
            return pjp.proceed();
        }

        // 获取被 @RequestBody 注解的参数
        Object bodyObj = null;
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] pjpArgs = pjp.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                bodyObj = pjpArgs[i];
            }
        }

        // 判断请求是否已经过 DecodeRequestBodyAdvice 处理
        // 这里由于不是所有方法都经过 DecodeRequestBodyAdvice 处理，所以默认为 True
        boolean security = Optional.ofNullable(request.getAttribute("Strix-Security")).map(String::valueOf).map(Boolean::parseBoolean).orElse(true);
        if (!security) {
            return RetBuilder.error(RetCode.BAT_REQUEST, I18nUtil.getMessage("error.bad_request") + "1");
        }

        String url = request.getRequestURI();
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        if (!StringUtils.hasText(sign) || !StringUtils.hasText(timestamp)) {
            return RetBuilder.error(RetCode.BAT_REQUEST, I18nUtil.getMessage("error.bad_request") + "2");
        }
        // 校验时间戳 30s 内有效
        if (System.currentTimeMillis() - Long.parseLong(timestamp) > 1000 * 30) {
            return RetBuilder.error(RetCode.BAT_REQUEST, I18nUtil.getMessage("error.bad_request") + "3");
        }

        final Map<String, Object> paramsMap = new TreeMap<>();
        paramsMap.put("_requestUrl", url);
        paramsMap.put("_timestamp", timestamp);

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            // 处理 GET 请求
            // 将 QueryString 中的参数放入 paramsMap
            paramsMap.putAll(ServletUtils.getRequestParams(request));
        } else {
            // 处理 POST 请求
            // 将 RequestBody 中的参数放入 paramsMap
            Optional.ofNullable(bodyObj).ifPresent(o -> paramsMap.putAll(objectMapper.convertValue(o, new TypeReference<SortedMap<String, Object>>() {
            })));
        }

        // 校验签名
        if (!ApiSignUtil.verifySign(paramsMap, sign)) {
            return RetBuilder.error(RetCode.BAT_REQUEST, I18nUtil.getMessage("error.bad_request") + "4");
        }

        return pjp.proceed();
    }

}
