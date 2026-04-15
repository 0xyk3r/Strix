package cn.projectan.strix.model.event.cache;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 缓存失效事件基类
 * <p>
 * sealed class — 所有缓存失效事件必须在此列举
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public abstract sealed class CacheInvalidationEvent extends ApplicationEvent
        permits MenuChangedEvent, PermissionChangedEvent, RoleChangedEvent,
                RoleMenuChangedEvent, RolePermissionChangedEvent,
                ManagerPermissionChangedEvent, ConfigChangedEvent, RegionChangedEvent {

    /** 发布此事件的实例 ID, 用于 Pub/Sub 防回环 */
    private final String instanceId;

    /** 是否来自远程实例 (Redis Pub/Sub), 为 true 时不再广播 */
    private final boolean remote;

    protected CacheInvalidationEvent(Object source, String instanceId, boolean remote) {
        super(source);
        this.instanceId = instanceId;
        this.remote = remote;
    }

    protected CacheInvalidationEvent(Object source, String instanceId) {
        this(source, instanceId, false);
    }

    protected CacheInvalidationEvent(Object source) {
        this(source, null, false);
    }

    /** 返回事件类型名称, 用于 Pub/Sub 序列化 */
    public abstract String getEventType();
}
