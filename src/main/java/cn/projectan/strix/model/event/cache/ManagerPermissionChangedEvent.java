package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 管理员权限变更事件 — 修改管理员的角色分配时发布
 * <p>
 * 影响范围: 该管理员的菜单/权限缓存 + LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class ManagerPermissionChangedEvent extends CacheInvalidationEvent {

    private final String managerId;

    public ManagerPermissionChangedEvent(Object source, String instanceId, String managerId) {
        super(source, instanceId);
        this.managerId = managerId;
    }

    public ManagerPermissionChangedEvent(Object source, String instanceId, boolean remote, String managerId) {
        super(source, instanceId, remote);
        this.managerId = managerId;
    }

    @Override
    public String getEventType() {
        return "MANAGER_PERMISSION_CHANGED";
    }
}
