package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.core.validation.annotation.DynamicDictValue;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2023/5/28 23:03
 */
@Schema(description = "字典更新请求")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictUpdateReq {

    @Schema(description = "字典 Key", example = "DictStatus")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典 Key 不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "字典 Key 长度不符合要求")
    @UpdateField
    private String key;

    @Schema(description = "字典名称", example = "字典状态")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 32, message = "字典名称长度不符合要求")
    @UpdateField
    private String name;

    @Schema(description = "字典数据类型", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典数据类型不可为空")
    @UpdateField
    private Short dataType;

    @Schema(description = "字典状态", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典状态不可为空")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "DictStatus", message = "字典状态不合法")
    @UpdateField
    private Short status;

    @Schema(description = "备注", example = "这是一条备注")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 255, message = "字典备注长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String remark;

}
