package cn.projectan.strix.model.response.system.role;

import cn.projectan.strix.model.response.system.menu.SystemMenuListResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionListResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2021/7/1 16:46
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemRoleResp {

    private String id;

    private String name;

    private Byte regionPermissionType;

    /**
     * 角色权限列表
     */
    private List<SystemMenuListResp.SystemMenuItem> menus;

    /**
     * 角色权限列表
     */
    private List<SystemPermissionListResp.SystemPermissionItem> permissions;

}
