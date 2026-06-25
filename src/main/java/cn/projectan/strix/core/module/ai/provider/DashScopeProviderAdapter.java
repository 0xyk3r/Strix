package cn.projectan.strix.core.module.ai.provider;

import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云 DashScope（百炼）提供商适配器
 * <p>
 * 处理 DashScope 特有参数：
 * <ul>
 *   <li>思考模式（enable_thinking、thinking_budget、preserve_thinking）</li>
 *   <li>代码解释器（enable_code_interpreter，仅流式）</li>
 *   <li>推理力度（reasoning_effort）</li>
 *   <li>联网搜索（enable_search 及全部 search_options 子参数）</li>
 *   <li>视觉参数（vl_high_resolution_images、min_pixels、max_pixels）</li>
 *   <li>视频帧率（fps）</li>
 *   <li>图文混排（enable_text_image_mixed）</li>
 *   <li>repetition_penalty、top_k（非标准 OpenAI 参数）</li>
 * </ul>
 * Usage 解析差异：
 * <ul>
 *   <li>缓存写入：{@code cache_creation_input_tokens}（顶层字段）</li>
 *   <li>缓存命中：{@code prompt_tokens_details.cached_tokens}（与 OpenAI 相同）</li>
 *   <li>思考链：{@code completion_tokens_details.reasoning_tokens}（与 OpenAI 相同）</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Component
public class DashScopeProviderAdapter extends BaseOpenAICompatAdapter {

    @Override
    public boolean supports(AiModelConfig config) {
        // 优先 providerType 字段
        if (config.getProviderType() != null && config.getProviderType() != AiProviderType.AUTO) {
            return config.getProviderType() == AiProviderType.DASHSCOPE;
        }
        // 兜底：baseUrl 模式
        String baseUrl = config.getBaseUrl();
        return baseUrl != null && (baseUrl.contains("dashscope") || baseUrl.contains("aliyuncs.com"));
    }

    @Override
    public void applyStreamingParams(Map<String, Object> body, AiModelConfig config) {
        super.applyStreamingParams(body, config);
        applyDashScopeParams(body, config, true);
    }

    @Override
    public void applyNonStreamingParams(Map<String, Object> body, AiModelConfig config) {
        super.applyNonStreamingParams(body, config);
        applyDashScopeParams(body, config, false);
    }

    private void applyDashScopeParams(Map<String, Object> body, AiModelConfig config, boolean streaming) {
        // top_k（非标准 OpenAI）
        if (config.getTopK() != null) body.put("top_k", config.getTopK());

        // repetition_penalty（非标准 OpenAI）
        if (config.getRepetitionPenalty() != null)
            body.put("repetition_penalty", config.getRepetitionPenalty().doubleValue());

        // 思考模式
        if (config.getEnableThinking() != null && config.getEnableThinking() == 1) {
            body.put("enable_thinking", true);
            if (config.getThinkingBudget() != null) body.put("thinking_budget", config.getThinkingBudget());
            if (config.getPreserveThinking() != null && config.getPreserveThinking() == 1) {
                body.put("preserve_thinking", true);
            }
            // 代码解释器仅在流式模式下生效
            if (streaming && config.getEnableCodeInterpreter() != null && config.getEnableCodeInterpreter() == 1) {
                body.put("enable_code_interpreter", true);
            }
        }

        // 推理力度
        if (StringUtils.hasText(config.getReasoningEffort())) {
            body.put("reasoning_effort", config.getReasoningEffort());
        }

        // 图文混排
        if (config.getEnableTextImageMixed() != null && config.getEnableTextImageMixed() == 1) {
            body.put("enable_text_image_mixed", true);
        }

        // 视觉参数
        if (config.getVlHighResolutionImages() != null && config.getVlHighResolutionImages() == 1) {
            body.put("vl_high_resolution_images", true);
        } else {
            if (config.getMinPixels() != null) body.put("min_pixels", config.getMinPixels());
            if (config.getMaxPixels() != null) body.put("max_pixels", config.getMaxPixels());
        }
        if (config.getVideoFps() != null) body.put("fps", config.getVideoFps().doubleValue());

        // 联网搜索
        if (config.getEnableSearch() != null && config.getEnableSearch() == 1) {
            body.put("enable_search", true);
            Map<String, Object> searchOptions = new HashMap<>();
            if (StringUtils.hasText(config.getSearchStrategy()))
                searchOptions.put("search_strategy", config.getSearchStrategy());
            if (config.getForcedSearch() != null && config.getForcedSearch() == 1)
                searchOptions.put("forced_search", true);
            if (config.getEnableSource() != null && config.getEnableSource() == 1)
                searchOptions.put("enable_source", true);
            if (config.getSearchFreshness() != null)
                searchOptions.put("freshness", config.getSearchFreshness());
            if (config.getEnableSearchExtension() != null && config.getEnableSearchExtension() == 1)
                searchOptions.put("enable_search_extension", true);
            if (!searchOptions.isEmpty()) body.put("search_options", searchOptions);
        }
    }

    /**
     * DashScope 使用 cache_creation_input_tokens（顶层）表示缓存写入，
     * 其余字段与 OpenAI 标准一致。
     */
    @Override
    public AiUsageDetail parseUsage(JsonNode usageNode) {
        AiUsageDetail base = super.parseUsage(usageNode);

        // DashScope 缓存写入字段名（顶层，与 OpenAI 的 prompt_tokens_details.cache_creation_input_tokens 不同）
        int cacheWriteRaw = usageNode.path("cache_creation_input_tokens").asInt(-1);
        Integer cacheWriteTokens = cacheWriteRaw >= 0 ? Integer.valueOf(cacheWriteRaw) : base.cacheWriteTokens();

        return new AiUsageDetail(
                base.promptTokens(),
                base.completionTokens(),
                base.cacheHitTokens(),
                cacheWriteTokens,
                base.reasoningTokens()
        );
    }

    /**
     * DashScope 流式 delta 使用 reasoning_content 字段
     */
    @Override
    public String extractReasoningContent(JsonNode delta) {
        JsonNode node = delta.get("reasoning_content");
        if (node == null || node.isNull()) return null;
        String val = node.asText("");
        return val.isEmpty() ? null : val;
    }
}
