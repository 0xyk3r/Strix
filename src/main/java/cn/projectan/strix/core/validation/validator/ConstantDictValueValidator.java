package cn.projectan.strix.core.validation.validator;

import cn.projectan.strix.core.validation.annotation.ConstantDictValue;
import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.util.common.I18nUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 常量字典值校验器
 * <p>注意：本校验器通过反射获取字典值，仅适用于字典值不可被后台修改的情况
 *
 * @author ProjectAn
 * @since 2023/9/6 16:43
 */
@Slf4j
public class ConstantDictValueValidator implements ConstraintValidator<ConstantDictValue, Object> {

    private final List<String> validValues = new ArrayList<>();

    @Override
    public void initialize(ConstantDictValue constraintAnnotation) {
        Class<?> clazz = constraintAnnotation.dict();
        if (!clazz.isAnnotationPresent(Dict.class)) {
            throw new RuntimeException("ConstantDictValueValidator: " + I18nUtil.get("error.dict.missingAnnotation"));
        }

        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(DictData.class)) {
                    continue;
                }
                validValues.add(field.get(null).toString());
            }
        } catch (Exception e) {
            log.error("ConstantDictValueValidator: failed to initialize dict values", e);
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 如果值为空, 则不校验
        if (value == null) {
            return true;
        }
        return validValues.contains(value.toString());
    }

}
