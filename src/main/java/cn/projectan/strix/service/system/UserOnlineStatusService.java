package cn.projectan.strix.service.system;

import cn.projectan.strix.model.constant.system.ChatRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户在线状态服务
 * <p>
 * 使用 Redis Set 管理用户的 WebSocket 连接，支持分布式多实例、多设备/多标签页在线
 * <p>
 * 设计思路：
 * 1. 每个用户在 Redis 中维护一个 Set，存储该用户的所有 WebSocket 连接 ID (sessionId)
 * 2. 当 WebSocket 连接建立时，将 sessionId 添加到用户的 Set 中
 * 3. 当 WebSocket 连接关闭时，将 sessionId 从用户的 Set 中移除
 * 4. 只有当 Set 为空时，才认为用户离线
 * 5. 通过 WebSocket 心跳定期刷新 Set 的过期时间，防止异常断开导致的"假在线"
 *
 * @author ProjectAn
 * @since 2026/2/2 12:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOnlineStatusService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 添加用户的 WebSocket 连接
     * <p>
     * 当 WebSocket 连接建立时调用
     *
     * @param userId    用户 ID
     * @param sessionId WebSocket Session ID
     */
    public void addConnection(String userId, String sessionId) {
        String key = ChatRedisKeys.USER_ONLINE_CONNECTIONS_PREFIX + userId;
        redisTemplate.opsForSet().add(key, sessionId);
        redisTemplate.expire(key, ChatRedisKeys.USER_ONLINE_CONNECTIONS_EXPIRE, TimeUnit.SECONDS);
        log.info("用户连接已添加: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 移除用户的 WebSocket 连接
     * <p>
     * 当 WebSocket 连接关闭时调用
     *
     * @param userId    用户 ID
     * @param sessionId WebSocket Session ID
     */
    public void removeConnection(String userId, String sessionId) {
        String key = ChatRedisKeys.USER_ONLINE_CONNECTIONS_PREFIX + userId;
        redisTemplate.opsForSet().remove(key, sessionId);

        // 检查是否还有其他连接
        Long size = redisTemplate.opsForSet().size(key);
        if (size == null || size == 0) {
            // 没有连接了，删除 key
            redisTemplate.delete(key);
            log.info("用户已离线: userId={}", userId);
        } else {
            log.info("用户连接已移除: userId={}, sessionId={}, 剩余连接数={}", userId, sessionId, size);
        }
    }

    /**
     * 刷新用户连接的过期时间（心跳）
     * <p>
     * WebSocket 心跳时调用，防止异常断开导致的"假在线"
     *
     * @param userId    用户 ID
     * @param sessionId WebSocket Session ID（可选，如果提供则验证连接是否存在）
     */
    public void refreshConnection(String userId, String sessionId) {
        String key = ChatRedisKeys.USER_ONLINE_CONNECTIONS_PREFIX + userId;

        // 验证连接是否存在
        if (sessionId != null) {
            Boolean exists = redisTemplate.opsForSet().isMember(key, sessionId);
            if (Boolean.FALSE.equals(exists)) {
                log.warn("刷新连接失败，连接不存在: userId={}, sessionId={}", userId, sessionId);
                return;
            }
        }

        // 刷新过期时间
        redisTemplate.expire(key, ChatRedisKeys.USER_ONLINE_CONNECTIONS_EXPIRE, TimeUnit.SECONDS);
        log.debug("用户连接已刷新: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 查询单个用户在线状态
     *
     * @param userId 用户 ID
     * @return true-在线（至少有一个连接），false-离线
     */
    public boolean isUserOnline(String userId) {
        String key = ChatRedisKeys.USER_ONLINE_CONNECTIONS_PREFIX + userId;
        Long size = redisTemplate.opsForSet().size(key);
        return size != null && size > 0;
    }

    /**
     * 获取用户的所有在线连接数
     *
     * @param userId 用户 ID
     * @return 连接数
     */
    public long getConnectionCount(String userId) {
        String key = ChatRedisKeys.USER_ONLINE_CONNECTIONS_PREFIX + userId;
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0;
    }

    /**
     * 获取用户的所有在线连接 ID
     *
     * @param userId 用户 ID
     * @return WebSocket Session ID 列表
     */
    public Set<Object> getConnections(String userId) {
        String key = ChatRedisKeys.USER_ONLINE_CONNECTIONS_PREFIX + userId;
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 批量查询用户在线状态
     * <p>
     * 使用 Redis Pipeline 优化性能
     *
     * @param userIds 用户 ID 列表
     * @return Map<用户ID, 是否在线>
     */
    public Map<String, Boolean> batchGetOnlineStatus(List<String> userIds) {
        Map<String, Boolean> result = new HashMap<>();

        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        // 使用 executePipelined 批量查询
        List<Object> pipelineResults = redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (String userId : userIds) {
                String key = ChatRedisKeys.USER_ONLINE_CONNECTIONS_PREFIX + userId;
                connection.setCommands().sCard(key.getBytes());
            }
            return null;
        });

        // 填充结果
        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            Long size = (pipelineResults != null && i < pipelineResults.size())
                    ? (Long) pipelineResults.get(i)
                    : null;
            result.put(userId, size != null && size > 0);
        }

        return result;
    }

}
