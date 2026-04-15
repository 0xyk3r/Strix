package cn.projectan.strix.model.event.cache;

/**
 * 菜单变更事件 — 菜单增/改/删时发布
 * <p>
 * 影响范围: 所有角色的菜单缓存 + 所有管理员的菜单缓存 + 所有在线管理员的 LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
public final class MenuChangedEvent extends CacheInvalidationEvent {

    public MenuChangedEvent(Object source, String instanceId) {
        super(source, instanceId);
    }

    public MenuChangedEvent(Object source) {
        super(source);
    }

    public MenuChangedEvent(Object source, String instanceId, boolean remote) {
        super(source, instanceId, remote);
    }

    @Override
    public String getEventType() {
        return "MENU_CHANGED";
    }
}
