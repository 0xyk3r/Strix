package cn.projectan.strix.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 是否允许该字段被更新
 *
 * @author 安炯奕
 * @date 2021/6/17 17:27
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateField {

    /**
     * 是否允许将有内容字段更新为空值
     */
    boolean allowEmpty() default false;

}
