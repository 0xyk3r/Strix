package cn.projectan.strix.core.module.ai.provider;

import cn.projectan.strix.model.db.system.AiModelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI Provider 注册中心
 * <p>
 * 自动注入所有 {@link AiProviderAdapter} Bean（按 {@code @Order} 排序），
 * 提供 {@link #getAdapter(AiModelConfig)} 方法根据配置选择适配器。
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Component
@RequiredArgsConstructor
public class AiProviderRegistry {

    private final List<AiProviderAdapter> adapters;

    /**
     * 选择支持该配置的第一个 adapter。
     * <p>
     * Adapter 按 Spring {@code @Order} 排序，专用 Adapter 优先（低序号），
     * {@link DefaultOpenAIProviderAdapter} 排最后（{@code Order(MAX_VALUE)}）作为兜底。
     *
     * @throws IllegalStateException 若无任何 adapter 支持（理论上不可能，DefaultOpenAI 是兜底）
     */
    public AiProviderAdapter getAdapter(AiModelConfig config) {
        return adapters.stream()
                .filter(a -> a.supports(config))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "无法找到适配提供商的 AiProviderAdapter，配置 key=" + config.getKey()));
    }
}
