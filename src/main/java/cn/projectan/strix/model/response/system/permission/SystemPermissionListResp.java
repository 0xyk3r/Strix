package cn.projectan.strix.model.response.system.permission;

import cn.projectan.strix.model.db.system.SystemPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2021/7/6 14:32
 */
@Schema(description = "权限列表响应")
@Getter
@NoArgsConstructor
public class SystemPermissionListResp {

    @Schema(description = "权限列表")
    private final List<SystemPermissionListResp.SystemPermissionItem> systemPermissionList = new ArrayList<>();

    public SystemPermissionListResp(List<SystemPermission> permissions) {
        for (SystemPermission sp : permissions) {
            SystemPermissionListResp.SystemPermissionItem item = new SystemPermissionListResp.SystemPermissionItem(sp.getId(), sp.getName(), sp.getKey(), sp.getMenuId(), sp.getDescription());
            systemPermissionList.add(item);
        }
    }

    @Schema(description = "权限列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemPermissionItem {

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

}
