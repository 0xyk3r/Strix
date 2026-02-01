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

}
