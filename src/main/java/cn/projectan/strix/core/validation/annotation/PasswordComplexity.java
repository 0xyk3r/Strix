package cn.projectan.strix.core.validation.annotation;

import cn.projectan.strix.core.validation.validator.PasswordComplexityValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 密码复杂度校验注解
 * <p>
 * 等保三级要求: 口令长度不少于 8 位, 必须包含大写字母、小写字母、数字、特殊字符中的至少 3 类。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
@Documented
@Constraint(validatedBy = {PasswordComplexityValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface PasswordComplexity {

    String message() default "密码不符合复杂度要求: 长度不少于8位, 必须包含大写字母、小写字母、数字、特殊字符中的至少3类";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
