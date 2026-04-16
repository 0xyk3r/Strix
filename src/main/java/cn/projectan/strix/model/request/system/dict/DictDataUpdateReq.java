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
import cn.projectan.strix.model.annotation.FormSchema;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/5/30 10:45
 */
@Schema(description = "字典数据更新请求")
@FormSchema
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictDataUpdateReq {

    @Schema(description = "字典 Key", example = "DictDataStatus")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.dictData.key}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "{validation.length:field.dictData.key}")
    @UpdateField
    private String key;

    @Schema(description = "字典值", example = "1")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.dictData.value}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 64, message = "{validation.length:field.dictData.value}")
    @UpdateField
    private String value;

    @Schema(description = "字典标签", example = "正常")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.dictData.label}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 64, message = "{validation.length:field.dictData.label}")
    @UpdateField
    private String label;

    @Schema(description = "排序值，越小越靠前", example = "0")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.dictData.sort}")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "{validation.minValue:field.dictData.sort}")
    @Max(groups = {InsertGroup.class, UpdateGroup.class}, value = 999, message = "{validation.maxValue:field.dictData.sort}")
    @UpdateField
    private Short sort;

    @Schema(description = "字典样式", example = "primary")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 32, message = "{validation.length:field.dictData.style}")
    @UpdateField(allowEmpty = true)
    private String style;

    @Schema(description = "字典数据状态", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.dictData.status}")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "DictDataStatus", message = "{validation.invalid:field.dictData.status}")
    @UpdateField
    private Short status;

    @Schema(description = "备注", example = "这是一条备注")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 255, message = "{validation.length:field.dictData.remark}")
    @UpdateField(allowEmpty = true)
    private String remark;

    @Schema(description = "父级字典数据值")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64)
    @UpdateField(allowEmpty = true)
    private String parentValue;

    @Schema(description = "是否默认值: 0=否, 1=是")
    @UpdateField
    private Short isDefault;

    @Schema(description = "生效开始时间")
    @UpdateField(allowEmpty = true)
    private LocalDateTime validFrom;

    @Schema(description = "生效结束时间")
    @UpdateField(allowEmpty = true)
    private LocalDateTime validTo;

}
