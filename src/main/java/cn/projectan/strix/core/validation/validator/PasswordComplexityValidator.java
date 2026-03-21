package cn.projectan.strix.core.validation.validator;

import cn.projectan.strix.core.validation.annotation.PasswordComplexity;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 密码复杂度校验器
 * <p>
 * 等保三级要求:
 * 1. 密码长度不少于 8 位
 * 2. 必须包含大写字母、小写字母、数字、特殊字符中的至少 3 类
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
public class PasswordComplexityValidator implements ConstraintValidator<PasswordComplexity, String> {

    private static final int MIN_LENGTH = 8;
    private static final int MIN_CATEGORIES = 3;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 允许为空（由 @NotEmpty 控制是否必填, 更新时允许不修改密码）
        if (value == null || value.isEmpty()) {
            return true;
        }

        if (value.length() < MIN_LENGTH) {
            return false;
        }

        int categoryCount = 0;
        if (value.chars().anyMatch(Character::isUpperCase)) {
            categoryCount++;
        }
        if (value.chars().anyMatch(Character::isLowerCase)) {
            categoryCount++;
        }
        if (value.chars().anyMatch(Character::isDigit)) {
            categoryCount++;
        }
        if (value.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
            categoryCount++;
        }

        return categoryCount >= MIN_CATEGORIES;
    }

}
