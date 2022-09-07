package cn.projectan.strix.model.response.system.systempermission;

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
public class SystemPermissionListQueryResp {

    private List<SystemPermissionListQueryResp.SystemPermissionItem> systemPermissionList = new ArrayList<>();

    public SystemPermissionListQueryResp(List<SystemPermission> permissions) {
        for (SystemPermission sp : permissions) {
            SystemPermissionListQueryResp.SystemPermissionItem item = new SystemPermissionListQueryResp.SystemPermissionItem(sp.getId(), sp.getName(), sp.getPermissionKey(), sp.getPermissionType(), sp.getDescription());
            systemPermissionList.add(item);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemPermissionItem {

        private String id;

        private String name;

        private String permissionKey;

        private Integer permissionType;

        private String description;

    }

}
