package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.core.validation.annotation.DynamicDictValue;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2023/5/30 10:45
 */
@Schema(description = "字典数据更新请求")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictDataUpdateReq {

    @Schema(description = "字典 Key", example = "DictDataStatus")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典 Key 不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "字典 Key 长度不符合要求")
    @UpdateField
    private String key;

    @Schema(description = "字典值", example = "1")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典值不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 64, message = "字典值长度不符合要求")
    @UpdateField
    private String value;

    @Schema(description = "字典标签", example = "正常")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典标签不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 64, message = "字典标签长度不符合要求")
    @UpdateField
    private String label;

    @Schema(description = "排序值，越小越靠前", example = "0")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典排序值不可为空")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "字典排序值不可小于 0")
    @Max(groups = {InsertGroup.class, UpdateGroup.class}, value = 999, message = "字典排序值不可大于 999")
    @UpdateField
    private Short sort;

    @Schema(description = "字典样式", example = "primary")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 32, message = "字典样式长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String style;

    @Schema(description = "字典数据状态", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "字典状态不可为空")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "DictDataStatus", message = "字典状态不合法")
    @UpdateField
    private Short status;

    @Schema(description = "备注", example = "这是一条备注")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 255, message = "字典备注长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String remark;

}
