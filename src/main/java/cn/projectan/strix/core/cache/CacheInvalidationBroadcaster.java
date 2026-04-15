package cn.projectan.strix.core.cache;

import cn.projectan.strix.model.event.cache.*;
import cn.projectan.strix.util.common.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存失效广播器
 * <p>
 * 将本地 CacheInvalidationEvent 序列化为 JSON 并发布到 Redis Pub/Sub channel,
 * 使其他实例也能收到缓存失效通知.
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationBroadcaster {

    public static final String CHANNEL = "strix:cache:invalidation";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 广播缓存失效事件到 Redis Pub/Sub
     *
     * @param event 本地事件 (remote=false 时才应调用)
     */
    public void broadcast(CacheInvalidationEvent event) {
        try {
            CacheInvalidationMessage message = new CacheInvalidationMessage();
            message.setEventType(event.getEventType());
            message.setInstanceId(event.getInstanceId());
            message.setPayload(buildPayload(event));

            String json = ObjectMapperUtil.writeValue(message);
            stringRedisTemplate.convertAndSend(CHANNEL, json);
            log.debug("缓存失效广播已发送: type={}, instanceId={}", event.getEventType(), event.getInstanceId());
        } catch (Exception e) {
            log.error("缓存失效广播失败: type={}", event.getEventType(), e);
        }
    }

    private Map<String, Object> buildPayload(CacheInvalidationEvent event) {
        Map<String, Object> payload = new HashMap<>();
        switch (event) {
            case RoleChangedEvent e -> payload.put("roleId", e.getRoleId());
            case RoleMenuChangedEvent e -> payload.put("roleId", e.getRoleId());
            case RolePermissionChangedEvent e -> payload.put("roleId", e.getRoleId());
            case ManagerPermissionChangedEvent e -> payload.put("managerId", e.getManagerId());
            case ConfigChangedEvent e -> payload.put("configKey", e.getConfigKey());
            case RegionChangedEvent e -> payload.put("regionIds", e.getRegionIds());
            case MenuChangedEvent ignored -> {}
            case PermissionChangedEvent ignored -> {}
        }
        return payload;
    }
}
