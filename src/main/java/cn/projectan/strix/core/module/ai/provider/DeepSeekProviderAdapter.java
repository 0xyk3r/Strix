package cn.projectan.strix.core.module.ai.provider;

import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * DeepSeek 官方 API 适配器
 * <p>
 * Usage 解析差异（vs OpenAI 标准）：
 * <ul>
 *   <li>缓存命中：顶层 {@code prompt_cache_hit_tokens}</li>
 *   <li>缓存写入（未命中）：顶层 {@code prompt_cache_miss_tokens}</li>
 *   <li>思考链：{@code completion_tokens_details.reasoning_tokens}（与 OpenAI 一致）</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Component
public class DeepSeekProviderAdapter extends BaseOpenAICompatAdapter {

    @Override
    public boolean supports(AiModelConfig config) {
        if (config.getProviderType() != null && config.getProviderType() != AiProviderType.AUTO) {
            return config.getProviderType() == AiProviderType.DEEPSEEK;
        }
        String baseUrl = config.getBaseUrl();
        return baseUrl != null && baseUrl.contains("api.deepseek.com");
    }

    @Override
    public void applyStreamingParams(Map<String, Object> body, AiModelConfig config) {
        super.applyStreamingParams(body, config);
        applyDeepSeekParams(body, config);
    }

    @Override
    public void applyNonStreamingParams(Map<String, Object> body, AiModelConfig config) {
        super.applyNonStreamingParams(body, config);
        applyDeepSeekParams(body, config);
    }

    private void applyDeepSeekParams(Map<String, Object> body, AiModelConfig config) {
        // DeepSeek 支持 reasoning_effort（r1 系列模型）
        if (StringUtils.hasText(config.getReasoningEffort())) {
            body.put("reasoning_effort", config.getReasoningEffort());
        }
    }

    /**
     * DeepSeek 缓存字段名与 OpenAI 标准不同，使用顶层字段。
     */
    @Override
    public AiUsageDetail parseUsage(JsonNode usageNode) {
        int promptTokens = usageNode.path("prompt_tokens").asInt(-1);
        int completionTokens = usageNode.path("completion_tokens").asInt(-1);

        // DeepSeek 使用顶层字段
        int cacheHitTokens = usageNode.path("prompt_cache_hit_tokens").asInt(-1);
        int cacheWriteTokens = usageNode.path("prompt_cache_miss_tokens").asInt(-1);

        // 思考链与 OpenAI 标准相同
        int reasoningTokens = usageNode.path("completion_tokens_details")
                .path("reasoning_tokens").asInt(-1);

        return new AiUsageDetail(
                promptTokens >= 0 ? promptTokens : null,
                completionTokens >= 0 ? completionTokens : null,
                cacheHitTokens >= 0 ? cacheHitTokens : null,
                cacheWriteTokens >= 0 ? cacheWriteTokens : null,
                reasoningTokens >= 0 ? reasoningTokens : null
        );
    }

    /**
     * DeepSeek 同样使用 reasoning_content 字段
     */
    @Override
    public String extractReasoningContent(JsonNode delta) {
        JsonNode node = delta.get("reasoning_content");
        if (node == null || node.isNull()) return null;
        String val = node.asText("");
        return val.isEmpty() ? null : val;
    }
}
