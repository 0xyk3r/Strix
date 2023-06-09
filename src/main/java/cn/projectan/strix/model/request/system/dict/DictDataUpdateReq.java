package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 安炯奕
 * @date 2023/5/30 10:45
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictDataUpdateReq {

    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "字典 Key 不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 64, message = "字典 Key 长度不符合要求")
    @UpdateField
    private String key;

    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "字典值不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 1, max = 64, message = "字典值长度不符合要求")
    @UpdateField
    private String value;

    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "字典标签不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 1, max = 64, message = "字典标签长度不符合要求")
    @UpdateField
    private String label;

    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "字典排序值不可为空")
    @Min(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, value = 0, message = "字典排序值不可小于 0")
    @Max(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, value = 999, message = "字典排序值不可大于 999")
    @UpdateField
    private Integer sort;

    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, max = 32, message = "字典样式长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String style;

    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "字典状态不可为空")
    @UpdateField
    private Integer status;

    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, max = 255, message = "字典备注长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String remark;

}
