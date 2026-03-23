package cn.projectan.strix.model.annotation;

import java.lang.annotation.*;

/**
 * 忽略加密/解密和签名校验
 * <p>
 * 标记此注解的 Controller 类或方法将跳过以下处理：
 * <ul>
 *   <li>{@code DecodeRequestBodyAdvice} — 跳过请求体解密</li>
 *   <li>{@code EncodeResponseBodyAdvice} — 跳过响应体加密</li>
 *   <li>{@code ApiSecurityCheckAspect} — 跳过 API 签名校验</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2021/8/25 14:25
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreEncryption {
}
