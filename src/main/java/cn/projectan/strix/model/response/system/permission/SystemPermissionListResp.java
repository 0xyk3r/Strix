package cn.projectan.strix.model.response.system.permission;

import cn.projectan.strix.model.db.SystemPermission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/7/6 14:32
 */
@Getter
@NoArgsConstructor
public class SystemPermissionListResp {

    private List<SystemPermissionListResp.SystemPermissionItem> systemPermissionList = new ArrayList<>();

    public SystemPermissionListResp(List<SystemPermission> permissions) {
        for (SystemPermission sp : permissions) {
            SystemPermissionListResp.SystemPermissionItem item = new SystemPermissionListResp.SystemPermissionItem(sp.getId(), sp.getName(), sp.getKey(), sp.getMenuId(), sp.getDescription());
            systemPermissionList.add(item);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemPermissionItem {

        private String id;

        private String name;

        private String key;

        private String menuId;

        private String description;

    }

}
