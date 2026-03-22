package cn.projectan.strix.model.response.system.region;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2021/9/29 18:06
 */
@Schema(description = "地区详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemRegionResp {

    @Schema(description = "区划ID")
    private String id;

    @Schema(description = "区划名称")
    private String name;

    @Schema(description = "区划层级")
    private Short level;

    @Schema(description = "父级区划ID")
    private String parentId;

    @Schema(description = "完整路径")
    private String fullPath;

    @Schema(description = "完整名称")
    private String fullName;

    @Schema(description = "备注")
    private String remarks;

}
