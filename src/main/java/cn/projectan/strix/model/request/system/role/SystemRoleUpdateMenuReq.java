package cn.projectan.strix.model.request.system.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/7/23 17:17
 */
@Schema(description = "角色菜单更新请求")
@Data
public class SystemRoleUpdateMenuReq {

    @Schema(description = "菜单ID列表")
    private String menuIds;

    @Schema(description = "权限ID列表")
    private String permissionIds;

}
