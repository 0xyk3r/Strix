package cn.projectan.strix.core.validation.validator;

import cn.projectan.strix.core.validation.annotation.DynamicDictValue;
import cn.projectan.strix.model.response.common.CommonDictResp;
import cn.projectan.strix.service.DictService;
import cn.projectan.strix.utils.SpringUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态字典值校验器
 * <p>适用于字典值可被后台动态修改的情况
 *
 * @author ProjectAn
 * @date 2023/9/6 21:50
 */
@Slf4j
public class DynamicDictValueValidator implements ConstraintValidator<DynamicDictValue, Object> {

    private final List<String> validValues = new ArrayList<>();

    @Override
    public void initialize(DynamicDictValue constraintAnnotation) {
        DictService dictService = SpringUtil.getBean(DictService.class);
        CommonDictResp dictResp = dictService.getDictResp(constraintAnnotation.dictName());
        if (dictResp == null) {
            throw new RuntimeException("StrixDictValueValidator: 字典不存在");
        }
        dictResp.getDictDataList().forEach(dictData -> validValues.add(dictData.getValue()));
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
