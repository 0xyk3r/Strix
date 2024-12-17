package cn.projectan.strix.core.aop.advice;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.core.security.ApiSecurity;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.util.ServletUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 请求数据解密处理
 *
 * @author ProjectAn
 * @since 2021/5/2 18:41
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class DecodeRequestBodyAdvice implements RequestBodyAdvice {

    private final ApiSecurity apiSecurity;
    private final ObjectMapper objectMapper;

    @Value("${strix.show-request:false}")
    private Boolean showRequest;

    @Override
    public boolean supports(MethodParameter methodParameter, @Nonnull Type type, @Nonnull Class<? extends HttpMessageConverter<?>> aClass) {
        String className = methodParameter.getContainingClass().getName();
        return !className.equals("cn.projectan.strix.core.aop.aspect.ApiSecurityCheckAspect") &&
                !className.equals("cn.projectan.strix.core.aop.advice.GlobalExceptionHandler") &&
                !methodParameter.getContainingClass().isAnnotationPresent(IgnoreDataEncryption.class) &&
                !methodParameter.hasMethodAnnotation(IgnoreDataEncryption.class);
    }

    @Nonnull
    @Override
    public HttpInputMessage beforeBodyRead(@Nonnull HttpInputMessage inputMessage, @Nonnull MethodParameter methodParameter, @Nonnull Type type, @Nonnull Class<? extends HttpMessageConverter<?>> aClass) {
        try {
            return new HttpInputMessageHandler(inputMessage, methodParameter);
        } catch (Exception e) {
            Method method = methodParameter.getMethod();
            String methodName = (method != null) ? method.getDeclaringClass().getName() + "." + method.getName() : "unknown";
            log.error("对方法: 【{}】请求数据进行解密时异常", methodName, e);
            return inputMessage;
        }
    }

    @Nonnull
    @Override
    public Object afterBodyRead(@Nonnull Object body, @Nonnull HttpInputMessage httpInputMessage, @Nonnull MethodParameter methodParameter, @Nonnull Type type, @Nonnull Class<? extends HttpMessageConverter<?>> aClass) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, @Nonnull HttpInputMessage httpInputMessage, @Nonnull MethodParameter methodParameter, @Nonnull Type type, @Nonnull Class<? extends HttpMessageConverter<?>> aClass) {
        return body;
    }

    private class HttpInputMessageHandler implements HttpInputMessage {
        private final HttpHeaders headers;
        private InputStream body;

        public HttpInputMessageHandler(HttpInputMessage inputMessage, MethodParameter methodParameter) throws Exception {
            HttpServletRequest request = ServletUtils.getRequest();

            this.headers = inputMessage.getHeaders();
            this.body = inputMessage.getBody();
            String originalBody = StrUtil.str(IoUtil.readBytes(this.body, false), StandardCharsets.UTF_8);

            String decryptBodyStr = handleSecurity(originalBody);

            Method method = methodParameter.getMethod();
            if (showRequest && method != null) {
                String fullMethodName = methodParameter.getContainingClass().getName() + "." + method.getName();
                log.info("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n" +
                        "请求数据: " + fullMethodName + "\n" +
                        decryptBodyStr +
                        "\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            }

            if (!StringUtils.hasText(decryptBodyStr)) {
                request.setAttribute("Strix-Security", false);
                this.body = InputStream.nullInputStream();
            } else {
                request.setAttribute("Strix-Security", true);
                this.body = IoUtil.toStream(decryptBodyStr, StandardCharsets.UTF_8);
            }
        }

        @Nonnull
        @Override
        public InputStream getBody() {
            return this.body;
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }

        public String handleSecurity(String requestData) {
            return Optional.ofNullable(requestData)
                    .map(apiSecurity::decrypt)
                    .orElse(null);
        }
    }
}
