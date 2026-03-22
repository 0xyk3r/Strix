package cn.projectan.strix.model.response.system.role;

import cn.projectan.strix.model.db.system.SystemRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2021/7/1 16:37
 */
@Schema(description = "角色列表响应")
@Getter
@NoArgsConstructor
public class SystemRoleListResp {

    @Schema(description = "角色列表")
    private final List<SystemRoleItem> systemRoleList = new ArrayList<>();

    public SystemRoleListResp(List<SystemRole> roles) {
        for (SystemRole sr : roles) {
            SystemRoleItem item = new SystemRoleItem(sr.getId(), sr.getName(), sr.getRegionPermissionType(), sr.getBuiltin());
            systemRoleList.add(item);
        }
    }

    @Schema(description = "角色列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemRoleItem {

        @Schema(description = "角色ID")
        private String id;

        @Schema(description = "角色名称")
        private String name;

        @Schema(description = "区域权限类型")
        private Short regionPermissionType;

        @Schema(description = "是否内置角色")
        private Short builtin;

    }

}
