package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 系统配置变更事件 — 配置值修改/新增/删除时发布
 * <p>
 * 影响范围: 特定 config key 的缓存
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class ConfigChangedEvent extends CacheInvalidationEvent {

    private final String configKey;

    public ConfigChangedEvent(Object source, String configKey) {
        super(source);
        this.configKey = configKey;
    }

    public ConfigChangedEvent(Object source, String instanceId, String configKey) {
        super(source, instanceId);
        this.configKey = configKey;
    }

    public ConfigChangedEvent(Object source, String instanceId, boolean remote, String configKey) {
        super(source, instanceId, remote);
        this.configKey = configKey;
    }

    @Override
    public String getEventType() {
        return "CONFIG_CHANGED";
    }
}
