package cn.projectan.strix.core.module.ai.provider;

import cn.projectan.strix.model.db.system.AiModelConfig;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * AI 云提供商适配器接口
 * <p>
 * 每个提供商实现一个 Adapter，负责：
 * <ol>
 *   <li>识别该提供商的配置（{@link #supports}）</li>
 *   <li>向请求体注入流式/非流式特有参数（{@link #applyStreamingParams}/{@link #applyNonStreamingParams}）</li>
 *   <li>解析 usage 字段（不同提供商字段名不同）（{@link #parseUsage}）</li>
 *   <li>提取思考内容（delta 中的字段名因提供商而异）（{@link #extractReasoningContent}）</li>
 * </ol>
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
public interface AiProviderAdapter {

    /**
     * 是否支持该配置。
     * <p>优先判断 {@code providerType} 字段；若为 null 或 0（AUTO），则按 baseUrl 模式匹配。
     */
    boolean supports(AiModelConfig config);

    /**
     * 向流式请求体注入参数（stream=true 场景）。
     * <p>基类已注入标准 OpenAI 参数和 stream_options，Provider 在此基础上追加特有参数。
     */
    void applyStreamingParams(Map<String, Object> body, AiModelConfig config);

    /**
     * 向非流式请求体注入参数（stream=false 场景）。
     * <p>与流式的区别：无 stream_options；部分参数（如 code_interpreter）仅在流式生效。
     */
    void applyNonStreamingParams(Map<String, Object> body, AiModelConfig config);

    /**
     * 解析 usage JSON 节点，提取 Token 统计信息。
     *
     * @param usageNode 响应中的 usage JsonNode，保证非 null
     * @return 解析结果，字段可为 null
     */
    AiUsageDetail parseUsage(JsonNode usageNode);

    /**
     * 从流式 delta 节点中提取思考/推理内容。
     * <p>DashScope 使用 {@code reasoning_content}，OpenAI 原生暂无此字段。
     *
     * @param delta 流式 chunk.choices[0].delta 节点
     * @return 思考内容（非空则有效），null 表示此 delta 无思考内容
     */
    String extractReasoningContent(JsonNode delta);
}
