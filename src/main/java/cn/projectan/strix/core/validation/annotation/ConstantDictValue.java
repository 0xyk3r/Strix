package cn.projectan.strix.core.validation.annotation;

import cn.projectan.strix.core.validation.validator.ConstantDictValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 静态字典值校验器
 *
 * @author 安炯奕
 * @date 2023/9/6 16:45
 */
@Documented
@Constraint(validatedBy = {ConstantDictValueValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface ConstantDictValue {

    String message() default "{error.validation.dict}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<?> dict();

}
