package cn.projectan.strix.model.annotation;

import java.lang.annotation.*;

/**
 * API 速率限制注解
 * <br>
 * 可标注在方法或类上，方法级优先于类级。
 * 值为 0 时使用全局默认配置。
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口内最大请求数，0 表示使用全局默认值
     */
    int limit() default 0;

    /**
     * 时间窗口（秒），0 表示使用全局默认值
     */
    int window() default 0;

    /**
     * 自定义限流 key 后缀（默认使用请求路径）
     */
    String key() default "";

    /**
     * 超限时的自定义提示消息
     */
    String message() default "";

}
