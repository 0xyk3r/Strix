package cn.projectan.strix.core.validation.annotation;

import cn.projectan.strix.core.validation.validator.DynamicDictValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 动态字典值校验器
 *
 * @author ProjectAn
 * @date 2023/9/6 21:51
 */
@Documented
@Constraint(validatedBy = {DynamicDictValueValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface DynamicDictValue {

    String message() default "{error.validation.dict}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String dictName();

}
