package cn.projectan.strix.model.constant.system;

/**
 * 聊天系统 Redis Keys 常量
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
public interface ChatRedisKeys {

    /**
     * 消息幂等 Key 前缀
     */
    String CHAT_MESSAGE_IDEMPOTENT_PREFIX = "strix:chat:msg:idempotent:";

    /**
     * 消息幂等过期时间（秒）
     */
    long CHAT_MESSAGE_IDEMPOTENT_EXPIRE = 600L;

    /**
     * 用户在线连接集合 Key 前缀（存储用户的所有 WebSocket 连接 ID）
     * Key: strix:user:online:connections:{userId}
     * Value: Set<String> (WebSocket Session IDs)
     */
    String USER_ONLINE_CONNECTIONS_PREFIX = "strix:user:online:connections:";

    /**
     * 用户在线连接过期时间（秒），默认 10 分钟
     * WebSocket 心跳会定期刷新此过期时间
     */
    long USER_ONLINE_CONNECTIONS_EXPIRE = 600L;

}
