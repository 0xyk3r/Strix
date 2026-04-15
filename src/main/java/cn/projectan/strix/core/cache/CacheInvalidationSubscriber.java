package cn.projectan.strix.core.cache;

import cn.projectan.strix.model.event.cache.*;
import cn.projectan.strix.util.common.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 缓存失效订阅器
 * <p>
 * 监听 Redis Pub/Sub channel, 将远程消息重建为 Spring ApplicationEvent 并发布.
 * 使用 instanceId 防回环 — 忽略自身实例发布的消息.
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber implements MessageListener {

    @Value("${strix.instance-id:#{T(java.util.UUID).randomUUID().toString().substring(0, 8)}}")
    private String instanceId;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            CacheInvalidationMessage msg = ObjectMapperUtil.readValue(json, CacheInvalidationMessage.class);

            if (msg == null || instanceId.equals(msg.getInstanceId())) {
                return;
            }

            log.info("收到远程缓存失效消息: type={}, from={}", msg.getEventType(), msg.getInstanceId());

            CacheInvalidationEvent event = rebuildEvent(msg);
            if (event != null) {
                eventPublisher.publishEvent(event);
            }
        } catch (Exception e) {
            log.error("处理远程缓存失效消息失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private CacheInvalidationEvent rebuildEvent(CacheInvalidationMessage msg) {
        Map<String, Object> payload = msg.getPayload();
        String srcInstanceId = msg.getInstanceId();

        return switch (msg.getEventType()) {
            case "MENU_CHANGED" ->
                    new MenuChangedEvent(this, srcInstanceId, true);
            case "PERMISSION_CHANGED" ->
                    new PermissionChangedEvent(this, srcInstanceId, true);
            case "ROLE_CHANGED" ->
                    new RoleChangedEvent(this, srcInstanceId, true, (String) payload.get("roleId"));
            case "ROLE_MENU_CHANGED" ->
                    new RoleMenuChangedEvent(this, srcInstanceId, true, (String) payload.get("roleId"));
            case "ROLE_PERMISSION_CHANGED" ->
                    new RolePermissionChangedEvent(this, srcInstanceId, true, (String) payload.get("roleId"));
            case "MANAGER_PERMISSION_CHANGED" ->
                    new ManagerPermissionChangedEvent(this, srcInstanceId, true, (String) payload.get("managerId"));
            case "CONFIG_CHANGED" ->
                    new ConfigChangedEvent(this, srcInstanceId, true, (String) payload.get("configKey"));
            case "REGION_CHANGED" ->
                    new RegionChangedEvent(this, srcInstanceId, true, (List<String>) payload.get("regionIds"));
            default -> {
                log.warn("未知的缓存失效事件类型: {}", msg.getEventType());
                yield null;
            }
        };
    }

    /**
     * 获取本实例的 instanceId (供事件发布者使用)
     */
    public String getInstanceId() {
        return instanceId;
    }
}
