package cn.projectan.strix.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 DTO 类可通过 FormSchema API 暴露其校验规则
 * <p>
 * 后端启动时扫描此注解, 注册到白名单
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FormSchema {

    /**
     * 自定义名称, 默认使用类的 simpleName
     */
    String value() default "";
}
