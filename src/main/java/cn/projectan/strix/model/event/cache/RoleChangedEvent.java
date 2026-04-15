package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 角色基本信息变更事件 — 角色名称/状态/排序等修改或删除时发布
 * <p>
 * 影响范围: role select_data 缓存 + 该角色下管理员的 LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class RoleChangedEvent extends CacheInvalidationEvent {

    private final String roleId;

    public RoleChangedEvent(Object source, String roleId) {
        super(source);
        this.roleId = roleId;
    }

    public RoleChangedEvent(Object source, String instanceId, String roleId) {
        super(source, instanceId);
        this.roleId = roleId;
    }

    public RoleChangedEvent(Object source, String instanceId, boolean remote, String roleId) {
        super(source, instanceId, remote);
        this.roleId = roleId;
    }

    @Override
    public String getEventType() {
        return "ROLE_CHANGED";
    }
}
