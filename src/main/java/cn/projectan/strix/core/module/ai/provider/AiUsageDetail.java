package cn.projectan.strix.core.module.ai.provider;

/**
 * AI 响应 usage 字段解析结果
 * <p>
 * 各提供商 usage 字段名称不同，由对应 {@link AiProviderAdapter} 实现负责解析，
 * 统一映射到此 record，供上层统一处理。
 *
 * @param promptTokens     输入 Token 数
 * @param completionTokens 输出 Token 数（含思考链）
 * @param cacheHitTokens   缓存命中 Token（DashScope: prompt_tokens_details.cached_tokens，
 *                         DeepSeek: prompt_cache_hit_tokens）
 * @param cacheWriteTokens 缓存写入 Token（DashScope: cache_creation_input_tokens，
 *                         DeepSeek: prompt_cache_miss_tokens）
 * @param reasoningTokens  思考链 Token（completion_tokens_details.reasoning_tokens）
 */
public record AiUsageDetail(
        Integer promptTokens,
        Integer completionTokens,
        Integer cacheHitTokens,
        Integer cacheWriteTokens,
        Integer reasoningTokens
) {
    /**
     * 空对象，所有字段为 null
     */
    public static final AiUsageDetail EMPTY = new AiUsageDetail(null, null, null, null, null);
}
