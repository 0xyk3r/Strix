package cn.projectan.strix.model.annotation;

import cn.projectan.strix.model.dict.SysLogOperType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ProjectAn
 * @date 2023/6/17 14:11
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrixLog {

    String operationGroup() default "";

    String operationName() default "";

    String operationType() default SysLogOperType.QUERY;

    boolean saveRequestParam() default true;

    boolean saveResponseData() default false;

}
