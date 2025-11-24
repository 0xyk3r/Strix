package cn.projectan.strix.core.aop.aspect;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.util.ApiSignUtil;
import cn.projectan.strix.util.I18nUtil;
import cn.projectan.strix.util.ServletUtils;
import cn.projectan.strix.util.SpringUtil;
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
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * API 安全校验切面
 *
 * @author ProjectAn
 * @since 2021/5/7 18:20
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ApiSecurityCheckAspect {

    private final ObjectMapper objectMapper;

    public ApiSecurityCheckAspect() {
        // 深拷贝一份 ObjectMapper ，避免修改全局配置
        ObjectMapper globalObjectMapper = SpringUtil.getBean(ObjectMapper.class);
        this.objectMapper = globalObjectMapper.copy();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
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
        if (signature.getMethod().isAnnotationPresent(IgnoreDataEncryption.class) ||
                signature.getMethod().getDeclaringClass().isAnnotationPresent(IgnoreDataEncryption.class)) {
            return pjp.proceed();
        }

        // 获取被 @RequestBody 注解的参数
        Object bodyObj = null;
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] pjpArgs = pjp.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                bodyObj = pjpArgs[i];
                break;
            }
        }

        // 判断请求是否已经过 DecodeRequestBodyAdvice 处理
        // 这里由于不是所有方法都经过 DecodeRequestBodyAdvice 处理，所以默认为 True
        boolean security = Optional.ofNullable(request.getAttribute("Strix-Security"))
                .map(String::valueOf)
                .map(Boolean::parseBoolean)
                .orElse(true);
        if (!security) {
            return RetBuilder.error(RetCode.BAT_REQUEST, I18nUtil.get("error.badRequest") + "1");
        }

        String url = request.getRequestURI();
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        if (!StringUtils.hasText(sign) || !StringUtils.hasText(timestamp)) {
            return RetBuilder.error(RetCode.BAT_REQUEST, I18nUtil.get("error.badRequest") + "2");
        }
        // 校验时间戳 600s 内有效
        if (System.currentTimeMillis() - Long.parseLong(timestamp) > 1000 * 600) {
            return RetBuilder.error(RetCode.BAT_REQUEST, I18nUtil.get("error.badRequest") + "3");
        }

        final Map<String, Object> paramsMap = new TreeMap<>();
        paramsMap.put("_requestUrl", url);
        paramsMap.put("_timestamp", timestamp);

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            // GET请求 将QueryString中的参数放入paramsMap
            paramsMap.putAll(ServletUtils.getRequestParams(request));
        } else {
            // POST请求 将RequestBody中的参数放入paramsMap
            Optional.ofNullable(bodyObj).ifPresent(o -> paramsMap.putAll(objectMapper.convertValue(o, new TypeReference<SortedMap<String, Object>>() {
            })));
        }

        // 校验签名
        if (!ApiSignUtil.verifySign(paramsMap, sign)) {
            return RetBuilder.error(RetCode.BAT_REQUEST, I18nUtil.get("error.badRequest") + "4");
        }

        return pjp.proceed();
    }

}
