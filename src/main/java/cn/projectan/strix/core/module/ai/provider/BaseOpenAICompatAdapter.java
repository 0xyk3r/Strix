package cn.projectan.strix.core.module.ai.provider;

import cn.projectan.strix.model.db.system.AiModelConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 标准 OpenAI 兼容参数处理基类
 * <p>
 * 注入所有通用的 OpenAI 兼容参数（temperature、top_p、max_completion_tokens 等），
 * Provider 子类在此基础上追加特有参数。
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Slf4j
public abstract class BaseOpenAICompatAdapter implements AiProviderAdapter {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void applyStreamingParams(Map<String, Object> body, AiModelConfig config) {
        applyCommonParams(body, config);
        body.put("stream_options", Map.of("include_usage", true));
    }

    @Override
    public void applyNonStreamingParams(Map<String, Object> body, AiModelConfig config) {
        applyCommonParams(body, config);
    }

    /**
     * 注入标准 OpenAI 兼容参数（流式/非流式均适用）
     */
    protected void applyCommonParams(Map<String, Object> body, AiModelConfig config) {
        if (config.getTemperature() != null) body.put("temperature", config.getTemperature().doubleValue());
        if (config.getTopP() != null) body.put("top_p", config.getTopP().doubleValue());

        // max_completion_tokens 优先，回退 max_tokens
        if (config.getMaxCompletionTokens() != null) {
            body.put("max_completion_tokens", config.getMaxCompletionTokens());
        } else if (config.getMaxTokens() != null) {
            body.put("max_completion_tokens", config.getMaxTokens());
        }

        if (config.getPresencePenalty() != null)
            body.put("presence_penalty", config.getPresencePenalty().doubleValue());
        if (config.getFrequencyPenalty() != null)
            body.put("frequency_penalty", config.getFrequencyPenalty().doubleValue());
        if (config.getSeed() != null) body.put("seed", config.getSeed());
        if (config.getN() != null) body.put("n", config.getN().intValue());

        // stop sequences
        if (StringUtils.hasText(config.getStopSequences())) {
            try {
                List<String> stops = MAPPER.readValue(config.getStopSequences(), new TypeReference<>() {
                });
                if (!stops.isEmpty()) body.put("stop", stops);
            } catch (Exception e) {
                log.warn("AI: 解析 stopSequences 失败: {}", config.getStopSequences());
            }
        }

        // json_object response_format
        if ("json_object".equals(config.getResponseFormat())) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        // logprobs
        if (config.getLogprobs() != null && config.getLogprobs() == 1) {
            body.put("logprobs", true);
            if (config.getTopLogprobs() != null) body.put("top_logprobs", config.getTopLogprobs().intValue());
        }
    }

    /**
     * 标准 OpenAI 格式 usage 解析（prompt_tokens_details.cached_tokens，completion_tokens_details.reasoning_tokens）
     */
    @Override
    public AiUsageDetail parseUsage(JsonNode usageNode) {
        int promptTokens = usageNode.path("prompt_tokens").asInt(-1);
        int completionTokens = usageNode.path("completion_tokens").asInt(-1);

        // 缓存命中（标准 OpenAI）
        int cachedTokens = usageNode.path("prompt_tokens_details").path("cached_tokens").asInt(-1);
        // 缓存写入（OpenAI 标准无此字段，子类可覆盖）
        int cacheWriteTokens = -1;
        // 思考链
        int reasoningTokens = usageNode.path("completion_tokens_details").path("reasoning_tokens").asInt(-1);

        return new AiUsageDetail(
                promptTokens >= 0 ? promptTokens : null,
                completionTokens >= 0 ? completionTokens : null,
                cachedTokens >= 0 ? cachedTokens : null,
                cacheWriteTokens >= 0 ? cacheWriteTokens : null,
                reasoningTokens >= 0 ? reasoningTokens : null
        );
    }

    /**
     * 标准 OpenAI delta 无 reasoning_content 字段
     */
    @Override
    public String extractReasoningContent(JsonNode delta) {
        return null;
    }
}
