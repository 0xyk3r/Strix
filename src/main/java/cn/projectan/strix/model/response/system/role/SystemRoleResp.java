package cn.projectan.strix.model.response.system.role;

import cn.projectan.strix.model.response.system.menu.SystemMenuListResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionListResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2021/7/1 16:46
 */
@Schema(description = "角色详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemRoleResp {

    @Schema(description = "角色ID")
    private String id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "区域权限类型")
    private Short regionPermissionType;

    /**
     * 角色权限列表
     */
    @Schema(description = "角色菜单列表")
    private List<SystemMenuListResp.SystemMenuManageItem> menus;

    /**
     * 角色权限列表
     */
    @Schema(description = "角色权限列表")
    private List<SystemPermissionListResp.SystemPermissionItem> permissions;

}
