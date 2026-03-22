package cn.projectan.strix.model.response.system.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2021/7/6 14:29
 */
@Schema(description = "权限详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemPermissionResp {

    @Schema(description = "权限ID")
    private String id;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限标识")
    private String key;

    @Schema(description = "所属菜单ID")
    private String menuId;

    @Schema(description = "权限描述")
    private String description;

}
