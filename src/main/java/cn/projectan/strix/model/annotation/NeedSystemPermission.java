package cn.projectan.strix.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 后台功能标记所需权限注解
 *
 * @author ProjectAn
 * @since 2021/5/13 13:23
 * @deprecated 已使用 Spring Security 替代
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface NeedSystemPermission {

    /**
     * 需要的权限
     */
    String value() default "";

    /**
     * 是否需要写入权限
     */
    boolean isEdit() default false;

}
