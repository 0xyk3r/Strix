package cn.projectan.strix.core.module.ai;

import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import com.openai.client.OpenAIClient;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 模型客户端惰性缓存容器
 * <p>
 * 按模型配置 key 缓存已创建的 Spring AI 客户端实例，首次使用时创建，
 * 配置变更时可通过 {@link #invalidate(String)} 清除缓存。
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelStore {

    private final AiClientFactory clientFactory;

    private final Map<String, OpenAiChatModel> chatModelMap = new ConcurrentHashMap<>();
    private final Map<String, OpenAIClient> syncClientMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建文本/视觉对话模型（TYPE = TEXT 或 VISION）
     */
    public OpenAiChatModel getChatModel(AiModelConfig config) {
        return chatModelMap.computeIfAbsent(config.getKey(), k -> {
            log.info("AI: 初始化 Chat 模型实例 <{}>", config.getKey());
            return clientFactory.createChatModel(config);
        });
    }

    /**
     * 获取或创建同步客户端（用于 SSE 真正流式推送）
     */
    public OpenAIClient getSyncClient(AiModelConfig config) {
        return syncClientMap.computeIfAbsent(config.getKey(), k -> {
            log.info("AI: 初始化 Sync 客户端实例 <{}>", config.getKey());
            return clientFactory.createSyncClient(config);
        });
    }

    /**
     * 根据模型类型获取对应客户端
     * <p>
     * 当前仅支持 TEXT 和 VISION 类型（通过百炼 OpenAI 兼容端点）。
     */
    public Object getModel(AiModelConfig config) {
        return switch (config.getType()) {
            case AiModelType.TEXT, AiModelType.VISION -> getChatModel(config);
            default -> throw new UnsupportedOperationException(
                    "模型类型 " + config.getType() + " 暂不支持（需 DashScope 原生 API）");
        };
    }

    /**
     * 清除指定 key 的所有缓存（配置更新时调用）
     */
    public void invalidate(String key) {
        chatModelMap.remove(key);
        syncClientMap.remove(key);
        log.info("AI: 已清除模型缓存 <{}>", key);
    }

    @PreDestroy
    private void destroy() {
        chatModelMap.clear();
        syncClientMap.clear();
        log.info("AI 模型缓存已清空");
    }

}
