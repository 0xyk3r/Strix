package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 角色-权限关联变更事件 — 修改角色的权限分配时发布
 * <p>
 * 影响范围: 该角色的权限缓存 + 该角色下管理员的权限缓存 + LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class RolePermissionChangedEvent extends CacheInvalidationEvent {

    private final String roleId;

    public RolePermissionChangedEvent(Object source, String instanceId, String roleId) {
        super(source, instanceId);
        this.roleId = roleId;
    }

    public RolePermissionChangedEvent(Object source, String instanceId, boolean remote, String roleId) {
        super(source, instanceId, remote);
        this.roleId = roleId;
    }

    @Override
    public String getEventType() {
        return "ROLE_PERMISSION_CHANGED";
    }
}
