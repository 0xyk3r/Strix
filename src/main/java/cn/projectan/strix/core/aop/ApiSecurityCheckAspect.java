package cn.projectan.strix.core.aop;

import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.request.base.BaseReq;
import cn.projectan.strix.utils.ApiSignUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Pointcut("execution(public * cn.projectan.strix..controller..*(..))")
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
                    return RetMarker.makeErrRsp(RetCode.BAT_REQUEST, "无效请求");
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
            return RetMarker.makeErrRsp(RetCode.BAT_REQUEST, "无效请求3");
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            HashMap<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("_requestUrl", url + (StringUtils.hasText(queryString) ? "?" + queryString : ""));
            if (ApiSignUtil.verifySign(paramsMap, timestamp, sign)) {
                return pjp.proceed();
            } else {
                return RetMarker.makeErrRsp(RetCode.BAT_REQUEST, "无效请求2");
            }
        } else {
            Map<String, Object> paramsMap = new HashMap<>();
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
                return RetMarker.makeErrRsp(RetCode.BAT_REQUEST, "无效请求2");
            }
        }

    }
}
