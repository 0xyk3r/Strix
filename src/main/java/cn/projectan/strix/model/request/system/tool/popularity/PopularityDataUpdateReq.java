package cn.projectan.strix.model.request.system.tool.popularity;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import cn.projectan.strix.model.annotation.FormSchema;

/**
 * @author ProjectAn
 * @since 2023/10/6 11:25
 */
@Schema(description = "热度数据更新请求")
@FormSchema
@Data
public class PopularityDataUpdateReq {

    @Schema(description = "原始数值")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.popularity.value}")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "{validation.outOfRange:field.popularity.value}")
    @UpdateField
    private Long originalValue;

}
