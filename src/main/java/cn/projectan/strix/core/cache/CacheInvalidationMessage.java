package cn.projectan.strix.core.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Redis Pub/Sub 缓存失效消息
 * <p>
 * 序列化为 JSON 通过 Redis channel 传递, 接收端根据 eventType 重建对应的 Spring Event
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationMessage {

    /** 事件类型 (对应 CacheInvalidationEvent.getEventType()) */
    private String eventType;

    /** 发布实例 ID (防回环) */
    private String instanceId;

    /** 事件携带的业务数据 (roleId, managerId, configKey, regionIds 等) */
    private Map<String, Object> payload;
}
