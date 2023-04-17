package cn.projectan.strix.core.advice;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.core.security.ApiSecurity;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * 请求体统一处理 用于请求体解密
 *
 * @author 安炯奕
 * @date 2021/5/2 18:41
 */
@Slf4j
@RestControllerAdvice
public class DecodeRequestBodyAdvice implements RequestBodyAdvice {

    @Autowired
    private ApiSecurity apiSecurity;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.profiles.active}")
    private String profiles;

    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        String className = methodParameter.getContainingClass().getName();
        // 排除swagger3的接口
        if (className.startsWith("springfox") || className.startsWith("io.swagger")) {
            return false;
        }

        boolean apiSecurityCheckAspect = className.equals("cn.projectan.strix.core.aop.ApiSecurityCheckAspect");
        boolean ignoreDataEncryptionByException = className.equals("cn.projectan.strix.core.advice.GlobalExceptionHandler");
        boolean ignoreDataEncryptionByClass = methodParameter.getContainingClass().isAnnotationPresent(IgnoreDataEncryption.class);
        IgnoreDataEncryption methodAnnotation = methodParameter.getMethodAnnotation(IgnoreDataEncryption.class);
        return !apiSecurityCheckAspect && !ignoreDataEncryptionByException && !ignoreDataEncryptionByClass && methodAnnotation == null;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        try {
            return new HttpInputMessageHandler(inputMessage);
        } catch (Exception e) {
            Method method = methodParameter.getMethod();
            if (method != null) {
                log.error("对方法: 【" + method.getDeclaringClass().getName() + "." + method.getName() + "】请求数据进行解密时异常", e);
            } else {
                log.error("对unknown方法请求数据进行解密时异常", e);
            }
            return inputMessage;
        }
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage httpInputMessage, MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage httpInputMessage, MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return body;
    }

    private class HttpInputMessageHandler implements HttpInputMessage {
        private HttpHeaders headers;
        private InputStream body;

        public HttpInputMessageHandler(HttpInputMessage inputMessage) throws Exception {
            this.headers = inputMessage.getHeaders();
            this.body = inputMessage.getBody();
            String originalBody = StrUtil.str(IoUtil.readBytes(this.body, false), StandardCharsets.UTF_8);

            String handlingData = handleSecurity(originalBody);
            if (handlingData != null) {
                this.body = IOUtils.toInputStream(handlingData, StandardCharsets.UTF_8);
            } else {
                this.body = IOUtils.toInputStream("{\"security\": false}", StandardCharsets.UTF_8);
            }
        }

        @Override
        public InputStream getBody() {
            return this.body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }

        public String handleSecurity(String requestData) {
            if (StringUtils.hasText(requestData)) {
                return apiSecurity.decrypt(requestData);
            }
            return null;
        }
    }
}
