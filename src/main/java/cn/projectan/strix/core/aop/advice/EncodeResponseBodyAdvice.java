package cn.projectan.strix.core.aop.advice;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.security.ApiSecurity;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应数据加密处理
 *
 * @author ProjectAn
 * @since 2021/5/2 19:06
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class EncodeResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ApiSecurity apiSecurity;
    private final ObjectMapper objectMapper;

    @Value("${strix.show-response:false}")
    private Boolean showResponse;

    @SneakyThrows
    @Override
    public boolean supports(MethodParameter methodParameter, @NotNull Class aClass) {
        String className = methodParameter.getContainingClass().getName();
        return !className.equals("cn.projectan.strix.core.aop.aspect.ApiSecurityCheckAspect") &&
                !className.equals("cn.projectan.strix.core.aop.advice.GlobalExceptionHandler") &&
                !methodParameter.getContainingClass().isAnnotationPresent(IgnoreDataEncryption.class) &&
                !methodParameter.hasMethodAnnotation(IgnoreDataEncryption.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, @NotNull MethodParameter methodParameter, @NotNull MediaType mediaType, @NotNull Class aClass, @NotNull ServerHttpRequest serverHttpRequest, @NotNull ServerHttpResponse serverHttpResponse) {
        try {
            if (showResponse) {
                String fullMethodName = methodParameter.getContainingClass().getName() + "." + methodParameter.getMethod().getName();
                log.info("\n============================================================\n" +
                        "响应数据: " + fullMethodName + "\n" +
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body) +
                        "\n============================================================");
            }
            return apiSecurity.encrypt(body);
        } catch (Exception e) {
            try {
                RetResult<Object> errorResponse = RetBuilder.error(RetCode.BAT_REQUEST, "响应封装时发生异常");
                return apiSecurity.encrypt(errorResponse);
            } catch (Exception exception) {
                return "An exception occurred in the API server !";
            }
        }
    }

}
