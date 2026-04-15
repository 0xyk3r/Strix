package cn.projectan.strix.model.event.cache;

/**
 * 权限变更事件 — 权限增/改/删时发布
 * <p>
 * 影响范围: 所有角色的权限缓存 + 所有管理员的权限缓存 + 所有在线管理员的 LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
public final class PermissionChangedEvent extends CacheInvalidationEvent {

    public PermissionChangedEvent(Object source, String instanceId) {
        super(source, instanceId);
    }

    public PermissionChangedEvent(Object source) {
        super(source);
    }

    public PermissionChangedEvent(Object source, String instanceId, boolean remote) {
        super(source, instanceId, remote);
    }

    @Override
    public String getEventType() {
        return "PERMISSION_CHANGED";
    }
}
