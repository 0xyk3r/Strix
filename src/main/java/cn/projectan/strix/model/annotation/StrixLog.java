package cn.projectan.strix.model.annotation;

import cn.projectan.strix.model.dict.system.SystemLogOperType;

import java.lang.annotation.*;

/**
 * 系统审计日志注解
 * <p>
 * 标记此注解的方法将由 {@code SystemLogAspect} 切面自动记录审计日志，
 * 包括请求参数、响应数据、操作用户、客户端信息和耗时等。
 *
 * @author ProjectAn
 * @since 2023/6/17 14:11
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrixLog {

    String operationGroup() default "";

    String operationName() default "";

    String operationType() default SystemLogOperType.QUERY;

    boolean saveRequestParam() default true;

    boolean saveResponseData() default false;

}
