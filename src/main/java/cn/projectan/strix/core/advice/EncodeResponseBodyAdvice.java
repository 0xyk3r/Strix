package cn.projectan.strix.core.advice;

import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.security.ApiSecurity;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应统一处理 用于响应加密
 *
 * @author 安炯奕
 * @date 2021/5/2 19:06
 */
@Slf4j
@RestControllerAdvice
public class EncodeResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private ApiSecurity apiSecurity;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.profiles.active}")
    private String profiles;
    @Value("${strix.show-response:false}")
    private Boolean showResponse;

    @SneakyThrows
    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        String className = methodParameter.getContainingClass().getName();
        boolean apiSecurityCheckAspect = className.equals("cn.projectan.strix.core.aop.ApiSecurityCheckAspect");
        boolean ignoreDataEncryptionByException = className.equals("cn.projectan.strix.core.advice.GlobalExceptionHandler");
        boolean ignoreDataEncryptionByClass = methodParameter.getContainingClass().isAnnotationPresent(IgnoreDataEncryption.class);
        IgnoreDataEncryption methodAnnotation = methodParameter.getMethodAnnotation(IgnoreDataEncryption.class);
        return !apiSecurityCheckAspect && !ignoreDataEncryptionByException && !ignoreDataEncryptionByClass && methodAnnotation == null;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        try {
            if ("dev".equals(profiles) && showResponse) {
                log.info("\n===============================================================\n" +
                        "返回数据原内容: ------" + methodParameter.getContainingClass().getName() + "------\n" +
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body) +
                        "\n===============================================================");
            }
            return apiSecurity.encrypt(body);
        } catch (Exception e) {
            try {
                RetResult<Object> errorResponse = RetMarker.makeErrRsp(RetCode.BAT_REQUEST, "响应封装时发生异常");
                return apiSecurity.encrypt(errorResponse);
            } catch (Exception exception) {
                return "An exception occurred in the API server !";
            }
        }
    }

}
