package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 角色-菜单关联变更事件 — 修改角色的菜单分配时发布
 * <p>
 * 影响范围: 该角色的菜单缓存 + 该角色下管理员的菜单缓存 + LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class RoleMenuChangedEvent extends CacheInvalidationEvent {

    private final String roleId;

    public RoleMenuChangedEvent(Object source, String instanceId, String roleId) {
        super(source, instanceId);
        this.roleId = roleId;
    }

    public RoleMenuChangedEvent(Object source, String instanceId, boolean remote, String roleId) {
        super(source, instanceId, remote);
        this.roleId = roleId;
    }

    @Override
    public String getEventType() {
        return "ROLE_MENU_CHANGED";
    }
}
