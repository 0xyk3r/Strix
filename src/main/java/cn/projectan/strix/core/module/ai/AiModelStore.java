package cn.projectan.strix.core.module.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 模型状态管理（配置变更通知）
 * <p>
 * 原先负责缓存 Spring AI 客户端实例。移除 Spring AI 后，聊天请求由单例
 * {@link AiChatClient} 处理（每次请求直接使用配置中的 baseUrl/apiKey），
 * 无需额外缓存客户端实例。
 * <p>
 * 此类保留以备其他模块调用 invalidate() 时不报错，可在未来扩展为
 * 缓存其他类型资源（如预热的 HTTP 连接）。
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Slf4j
@Component
public class AiModelStore {

    /**
     * 清除指定 key 的客户端缓存（配置更新时调用）
     * <p>当前 AiChatClient 为单例无状态，此方法为空实现。
     */
    public void invalidate(String key) {
        log.info("AI: 配置变更通知 <{}>（当前无需清除客户端缓存）", key);
    }
}

