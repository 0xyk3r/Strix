package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 安炯奕
 * @date 2023/5/28 23:03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictUpdateReq {

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典 Key 不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "字典 Key 长度不符合要求")
    @UpdateField
    private String key;

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 32, message = "字典名称长度不符合要求")
    @UpdateField
    private String name;

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典数据类型不可为空")
    @UpdateField
    private Integer dataType;

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典状态不可为空")
    @UpdateField
    private Integer status;

    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 255, message = "字典备注长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String remark;

}
