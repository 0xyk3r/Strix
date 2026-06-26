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
 * 与 OpenAI 标准的差异：
 * <ul>
 *   <li>思考模式：使用 {@code thinking: {type: "enabled/disabled"}} 对象控制（而非 DashScope 的布尔值）</li>
 *   <li>思考模式开启时，{@code temperature}、{@code top_p}、{@code presence_penalty}、
 *       {@code frequency_penalty} 无效，主动移除避免误导</li>
 *   <li>{@code reasoning_effort} 可独立使用；值归一化：low/medium→high, xhigh→max</li>
 *   <li>缓存命中：顶层 {@code prompt_cache_hit_tokens}</li>
 *   <li>缓存写入（未命中）：顶层 {@code prompt_cache_miss_tokens}</li>
 *   <li>支持 FIM Beta 端点（{@code /beta/completions}）和 Chat Prefix Beta</li>
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
        // 1. thinking 对象控制
        //    enableThinking == 1  → {"thinking": {"type": "enabled"}}  并移除不兼容参数
        //    enableThinking == 0  → {"thinking": {"type": "disabled"}}
        //    enableThinking == null → 不设置（服务端默认 enabled）
        if (config.getEnableThinking() != null) {
            String thinkingType = config.getEnableThinking() == 1 ? "enabled" : "disabled";
            body.put("thinking", Map.of("type", thinkingType));

            if (config.getEnableThinking() == 1) {
                // 思考模式开启时，这四个参数无效，移除以避免误导
                body.remove("temperature");
                body.remove("top_p");
                body.remove("presence_penalty");
                body.remove("frequency_penalty");
            }
        }

        // 2. reasoning_effort 归一化后注入（独立于 thinking 状态）
        //    DeepSeek 文档：low/medium → high，xhigh → max
        if (StringUtils.hasText(config.getReasoningEffort())) {
            body.put("reasoning_effort", normalizeReasoningEffort(config.getReasoningEffort()));
        }
    }

    /**
     * 归一化 reasoning_effort 值：
     * low / medium → high，xhigh → max，其余原样传递。
     */
    private String normalizeReasoningEffort(String effort) {
        return switch (effort.toLowerCase()) {
            case "low", "medium" -> "high";
            case "xhigh" -> "max";
            default -> effort;
        };
    }

    /**
     * DeepSeek 缓存字段名与 OpenAI 标准不同，使用顶层字段。
     */
    @Override
    public AiUsageDetail parseUsage(JsonNode usageNode) {
        int promptTokens = usageNode.path("prompt_tokens").asInt(-1);
        int completionTokens = usageNode.path("completion_tokens").asInt(-1);

        // DeepSeek 使用顶层字段（非 prompt_tokens_details 嵌套）
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
     * DeepSeek 同样使用 reasoning_content 字段（与 DashScope 一致）。
     */
    @Override
    public String extractReasoningContent(JsonNode delta) {
        JsonNode node = delta.get("reasoning_content");
        if (node == null || node.isNull()) return null;
        String val = node.asText("");
        return val.isEmpty() ? null : val;
    }

    @Override
    public boolean supportsFim() {
        return true;
    }

    @Override
    public boolean supportsChatPrefix() {
        return true;
    }
}

