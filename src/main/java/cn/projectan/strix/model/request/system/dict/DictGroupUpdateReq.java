package cn.projectan.strix.model.request.system.dict;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.FormSchema;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典分组更新请求")
@FormSchema
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictGroupUpdateReq {

    @Schema(description = "分组名称")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.dictGroup.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 64, message = "{validation.length:field.dictGroup.name}")
    @UpdateField
    private String name;

    @Schema(description = "分组图标（Lucide 图标名）")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64, message = "{validation.length:field.dictGroup.icon}")
    @UpdateField(allowEmpty = true)
    private String icon;

    @Schema(description = "排序值")
    @UpdateField
    private Short sortValue;

}
