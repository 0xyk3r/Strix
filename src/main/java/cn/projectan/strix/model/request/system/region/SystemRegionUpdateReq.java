package cn.projectan.strix.model.request.system.region;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/9/29 18:50
 */
@Schema(description = "地区更新请求")
@Data
public class SystemRegionUpdateReq {

    @Schema(description = "地区名称")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "地区名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 32, message = "地区名称长度不符合要求")
    @UpdateField
    private String name;

    @Schema(description = "父级区划ID")
    @UpdateField
    private String parentId;

    @Schema(description = "备注信息")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 128, message = "地区备注信息长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String remarks;

}
