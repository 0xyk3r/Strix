package cn.projectan.strix.core.aop.advice;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.security.ApiSecurity;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.constant.system.StrixPasswordConst;
import cn.projectan.strix.model.properties.system.StrixProperties;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.http.ServletUtil;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.ObjectMapper;

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
    private final StrixProperties strixProperties;

    @SneakyThrows
    @Override
    public boolean supports(@NotNull MethodParameter methodParameter, @Nonnull Class aClass) {
        if (StrixPasswordConst.IGNORE_ENCRYPTION.equals(ServletUtil.getRequest().getHeader("ss-pwd"))) {
            return false;
        }
        String className = methodParameter.getContainingClass().getName();
        return className.startsWith("cn.projectan.strix.controller") &&
                !methodParameter.getContainingClass().isAnnotationPresent(IgnoreEncryption.class) &&
                !methodParameter.hasMethodAnnotation(IgnoreEncryption.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, @Nonnull MethodParameter methodParameter, @Nonnull MediaType mediaType, @Nonnull Class aClass, @Nonnull ServerHttpRequest serverHttpRequest, @Nonnull ServerHttpResponse serverHttpResponse) {
        try {
            if (strixProperties.isShowResponse() && methodParameter.getMethod() != null) {
                String fullMethodName = methodParameter.getContainingClass().getName() + "." + methodParameter.getMethod().getName();
                log.info("""
                                
                                ============================================================
                                响应函数: {}
                                响应数据:
                                {}
                                ============================================================""",
                        fullMethodName, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));
            }
            return apiSecurity.encrypt(body);
        } catch (Exception e) {
            log.error("响应数据加密失败", e);
            try {
                RetResult<Object> errorResponse = RetBuilder.error(RetCode.SERVER_ERROR, I18nUtil.get("error.response.encodeFailed"));
                return apiSecurity.encrypt(errorResponse);
            } catch (Exception encryptException) {
                log.error("加密错误响应也失败，返回未加密错误", encryptException);
                return RetBuilder.error(RetCode.SERVER_ERROR, "Internal server error");
            }
        }
    }

}
