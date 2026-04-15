package cn.projectan.strix.model.event.cache;

import lombok.Getter;

import java.util.List;

/**
 * 地区变更事件 — 地区增/改/删时发布
 * <p>
 * 影响范围: 指定地区 ID 的缓存
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class RegionChangedEvent extends CacheInvalidationEvent {

    private final List<String> regionIds;

    public RegionChangedEvent(Object source, List<String> regionIds) {
        super(source);
        this.regionIds = regionIds;
    }

    public RegionChangedEvent(Object source, String instanceId, List<String> regionIds) {
        super(source, instanceId);
        this.regionIds = regionIds;
    }

    public RegionChangedEvent(Object source, String instanceId, boolean remote, List<String> regionIds) {
        super(source, instanceId, remote);
        this.regionIds = regionIds;
    }

    @Override
    public String getEventType() {
        return "REGION_CHANGED";
    }
}
