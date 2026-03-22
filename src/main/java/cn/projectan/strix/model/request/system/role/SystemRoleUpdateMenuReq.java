package cn.projectan.strix.model.request.system.role;

import cn.projectan.strix.core.validation.group.UpdateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/7/23 17:17
 */
@Schema(description = "角色菜单更新请求")
@Data
public class SystemRoleUpdateMenuReq {

    @NotNull(message = "菜单ID列表不能为空", groups = UpdateGroup.class)
    @Schema(description = "菜单ID列表")
    private String menuIds;

    @NotNull(message = "权限ID列表不能为空", groups = UpdateGroup.class)
    @Schema(description = "权限ID列表")
    private String permissionIds;

}
