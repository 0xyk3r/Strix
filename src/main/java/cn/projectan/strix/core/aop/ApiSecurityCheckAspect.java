package cn.projectan.strix.core.aop;

import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.request.base.BaseReq;
import cn.projectan.strix.utils.ApiSignUtil;
import cn.projectan.strix.utils.I18nUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * api统一安全校验
 *
 * @author 安炯奕
 * @date 2021/5/7 18:20
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class ApiSecurityCheckAspect {

    private final ObjectMapper objectMapper;

    public ApiSecurityCheckAspect() {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }

    @Pointcut("execution(public * cn.projectan..controller..*(..)) && !execution(public * cn.projectan.captcha.controller..*(..))")
    public void controller() {
    }

    @Around("controller()")
    public Object handle(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Object bodyObj = null;
        for (Object obj : args) {
            if (obj instanceof BaseReq) {
                bodyObj = obj;
                BaseReq bd = (BaseReq) obj;
                if (!bd.getSecurity()) {
                    return RetMarker.makeErrRsp(RetCode.BAT_REQUEST, I18nUtil.getMessage("error.bad_request") + "1");
                }
            }
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return pjp.proceed();
        }
        HttpServletRequest request = attributes.getRequest();

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        boolean ignoreDataEncryptionByException = signature.getMethod().getDeclaringClass().getName().equals("cn.projectan.strix.core.advice.GlobalExceptionHandler");
        boolean ignoreDataEncryptionByClass = signature.getMethod().getDeclaringClass().isAnnotationPresent(IgnoreDataEncryption.class);
        IgnoreDataEncryption ignoreDataEncryption = signature.getMethod().getAnnotation(IgnoreDataEncryption.class);
        if (ignoreDataEncryptionByException || ignoreDataEncryptionByClass || ignoreDataEncryption != null) {
            return pjp.proceed();
        }

        String url = request.getRequestURI();
        String queryString = request.getQueryString();
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");

        if (!StringUtils.hasText(sign) || !StringUtils.hasText(timestamp)) {
            return RetMarker.makeErrRsp(RetCode.BAT_REQUEST, I18nUtil.getMessage("error.bad_request") + "3");
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            Map<String, Object> paramsMap = new TreeMap<>();
            // 将queryString转换为map
            if (StringUtils.hasText(queryString)) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        paramsMap.put(kv[0], kv[1]);
                    }
                }
            }
//            paramsMap.put("_requestUrl", url + (StringUtils.hasText(queryString) ? "?" + queryString : ""));
            paramsMap.put("_requestUrl", url);
            if (ApiSignUtil.verifySign(paramsMap, timestamp, sign)) {
                return pjp.proceed();
            } else {
                return RetMarker.makeErrRsp(RetCode.BAT_REQUEST, I18nUtil.getMessage("error.bad_request") + "2");
            }
        } else {
            Map<String, Object> paramsMap = new TreeMap<>();
            // 处理请求体为空报空指针的问题
            if (bodyObj != null) {
                paramsMap = objectMapper.readValue(objectMapper.writeValueAsString(bodyObj), new TypeReference<SortedMap<String, Object>>() {
                });
            }
            paramsMap.put("_requestUrl", url);
            paramsMap.remove("security");
            if (ApiSignUtil.verifySign(paramsMap, timestamp, sign)) {
                return pjp.proceed();
            } else {
                return RetMarker.makeErrRsp(RetCode.BAT_REQUEST, I18nUtil.getMessage("error.bad_request") + "2");
            }
        }

    }
}
