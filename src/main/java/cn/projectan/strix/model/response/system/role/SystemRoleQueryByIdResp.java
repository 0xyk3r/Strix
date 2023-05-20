package cn.projectan.strix.model.response.system.role;

import cn.projectan.strix.model.response.system.menu.SystemMenuListQueryResp;
import cn.projectan.strix.model.response.system.permission.SystemPermissionListQueryResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/7/1 16:46
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemRoleQueryByIdResp {

    private String id;

    private String name;

    /**
     * 角色权限列表
     */
    private List<SystemMenuListQueryResp.SystemMenuItem> menus;

    /**
     * 角色权限列表
     */
    private List<SystemPermissionListQueryResp.SystemPermissionItem> permissions;

}
