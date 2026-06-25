# AI Provider Adapter 重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除 Spring AI 依赖，统一使用 OkHttp 实现流式和非流式 AI 聊天，引入 Provider Adapter 模式支持 DashScope、DeepSeek
等多提供商，并通过 `providerType` 字段显式配置提供商。

**Architecture:** 所有 `/chat/completions` 调用通过统一的 `AiChatClient`（OkHttp）发出；每个提供商对应一个
`AiProviderAdapter` 实现，负责注入流式/非流式特有参数和解析 usage 字段；`AiProviderRegistry` 按 `providerType` 字段（优先）或
baseUrl 模式（兜底）选择 adapter。`AiService` 的核心流程（会话管理、SSE 推送、消息落库）保持不变，移除对 Spring AI 消息类型和
`OpenAI Java SDK` 流式 API 的依赖。

**Tech Stack:** Java 21, OkHttp 5, Jackson, Spring Boot 4（Bean 注入）

---

## 文件结构

### 新建文件

| 路径                                                                     | 职责                        |
|------------------------------------------------------------------------|---------------------------|
| `core/module/ai/provider/AiProviderAdapter.java`                       | Provider 接口定义             |
| `core/module/ai/provider/AiUsageDetail.java`                           | Usage 解析结果 record         |
| `core/module/ai/provider/BaseOpenAICompatAdapter.java`                 | 标准 OpenAI 参数处理（抽象基类）      |
| `core/module/ai/provider/DashScopeProviderAdapter.java`                | DashScope 全部特有参数          |
| `core/module/ai/provider/DeepSeekProviderAdapter.java`                 | DeepSeek 特有参数（含缓存字段差异）    |
| `core/module/ai/provider/DefaultOpenAIProviderAdapter.java`            | 兜底实现（BaseUrl 无法识别时）       |
| `core/module/ai/provider/AiProviderRegistry.java`                      | 按配置选择 adapter             |
| `core/module/ai/AiChatClient.java`                                     | 统一 OkHttp 聊天客户端（流式 + 非流式） |
| `model/dict/system/AiProviderType.java`                                | 提供商类型 Dict 常量类            |
| `docs/database/changelog/2026-06-25-ai-model-config-provider-type.sql` | DB 迁移                     |

### 修改文件

| 路径                                                           | 变更内容                                                   |
|--------------------------------------------------------------|--------------------------------------------------------|
| `build.gradle`                                               | 移除 `spring-ai-openai`，保留 `okhttp`、`jackson`            |
| `model/db/system/AiModelConfig.java`                         | 新增 `providerType SHORT` 字段                             |
| `model/request/system/module/ai/AiModelConfigUpdateReq.java` | 新增 `providerType` 字段                                   |
| `model/response/system/ai/AiModelConfigResp.java`            | 新增 `providerType` 字段                                   |
| `core/module/ai/AiClientFactory.java`                        | 完全重写（移除 Spring AI，仅创建 OkHttpClient）                    |
| `core/module/ai/AiModelStore.java`                           | 简化（仅缓存 OkHttpClient）                                   |
| `service/system/AiService.java`                              | 完全重写（移除 Spring AI，使用 AiChatClient + AiProviderAdapter） |

---

## Task 1: 数据模型层 — providerType 字段和 Dict

**Files:**

- Create: `src/main/java/cn/projectan/strix/model/dict/system/AiProviderType.java`
- Modify: `src/main/java/cn/projectan/strix/model/db/system/AiModelConfig.java`
- Modify: `src/main/java/cn/projectan/strix/model/request/system/module/ai/AiModelConfigUpdateReq.java`
- Modify: `src/main/java/cn/projectan/strix/model/response/system/ai/AiModelConfigResp.java`
- Create: `src/main/java/cn/projectan/strix/docs/database/changelog/2026-06-25-ai-model-config-provider-type.sql`

- [ ] **Step 1: 创建 AiProviderType 常量类**

参照 `AiModelType.java` 的风格：

```java
package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 云提供商类型
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Dict(key = "AiProviderType", value = "AI提供商类型")
@Schema(description = "AI提供商类型")
public class AiProviderType implements BaseDict {

    /** 自动识别（根据 baseUrl 判断，兜底策略） */
    @DictData(label = "自动识别", sort = 0, style = DictDataStyle.DEFAULT)
    public static final short AUTO = 0;

    /** 阿里云 DashScope / 百炼 */
    @DictData(label = "DashScope（阿里云）", sort = 1, style = DictDataStyle.PRIMARY)
    public static final short DASHSCOPE = 1;

    /** DeepSeek 官方 API */
    @DictData(label = "DeepSeek", sort = 2, style = DictDataStyle.SUCCESS)
    public static final short DEEPSEEK = 2;

    /** 标准 OpenAI API */
    @DictData(label = "OpenAI", sort = 3, style = DictDataStyle.INFO)
    public static final short OPENAI = 3;

    /** 其他 OpenAI 兼容端点 */
    @DictData(label = "其他兼容端点", sort = 9, style = DictDataStyle.DEFAULT)
    public static final short COMPATIBLE = 9;
}
```

- [ ] **Step 2: 向 AiModelConfig 实体新增 providerType 字段**

在 `AiModelConfig.java` 中，在 `baseUrl` 字段 **之前** 添加：

```java
/**
 * 云提供商类型（0=自动识别 1=DashScope 2=DeepSeek 3=OpenAI 9=其他兼容）
 *
 * @see cn.projectan.strix.model.dict.system.AiProviderType
 */
private Short providerType;
```

- [ ] **Step 3: 向 AiModelConfigUpdateReq 新增 providerType**

在 `key` 字段附近添加（`@UpdateField` 标记可更新）：

```java

@Schema(description = "云提供商类型：0=自动识别 1=DashScope 2=DeepSeek 3=OpenAI 9=其他", example = "1")
@UpdateField(allowEmpty = true)
private Short providerType;
```

- [ ] **Step 4: 向 AiModelConfigResp 新增 providerType**

在 `type` 字段附近添加：

```java

@Schema(description = "云提供商类型：0=自动识别 1=DashScope 2=DeepSeek 3=OpenAI 9=其他")
private Short providerType;
```

同时在 `from()` 方法中增加映射：

```java
resp.setProviderType(config.getProviderType());
```

- [ ] **Step 5: 创建 DB 迁移 SQL**

创建 `docs/database/changelog/2026-06-25-ai-model-config-provider-type.sql`：

```sql
-- 2026-06-25 AI 模型配置新增云提供商类型字段
ALTER TABLE sys_ai_model_config
    ADD COLUMN provider_type TINYINT DEFAULT 0 COMMENT '云提供商类型: 0=自动识别 1=DashScope 2=DeepSeek 3=OpenAI 9=其他兼容' AFTER `type`;
```

- [ ] **Step 6: 验证编译**

```bash
cd Strix && ./gradlew compileJava 2>&1 | Select-Object -Last 5
```

期望输出：无错误

---

## Task 2: AiProviderAdapter 接口与 AiUsageDetail

**Files:**

- Create: `src/main/java/cn/projectan/strix/core/module/ai/provider/AiUsageDetail.java`
- Create: `src/main/java/cn/projectan/strix/core/module/ai/provider/AiProviderAdapter.java`

- [ ] **Step 1: 创建 AiUsageDetail record**

```java
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
    /** 空对象，所有字段为 null */
    public static final AiUsageDetail EMPTY = new AiUsageDetail(null, null, null, null, null);

    /** 辅助：从 Jackson int 值构建（0 视为 null） */
    public static Integer intOrNull(int value) {
        return value > 0 ? value : null;
    }
}
```

- [ ] **Step 2: 创建 AiProviderAdapter 接口**

```java
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
     * <p>不同提供商字段名不同：
     * <ul>
     *   <li>DashScope: {@code cache_creation_input_tokens}（写入），{@code prompt_tokens_details.cached_tokens}（命中）</li>
     *   <li>DeepSeek:  {@code prompt_cache_hit_tokens}（命中），{@code prompt_cache_miss_tokens}（写入）</li>
     *   <li>OpenAI:    {@code prompt_tokens_details.cached_tokens}（命中）</li>
     * </ul>
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
```

- [ ] **Step 3: 验证编译**

```bash
cd Strix && ./gradlew compileJava 2>&1 | Select-Object -Last 5
```

---

## Task 3: BaseOpenAICompatAdapter 抽象基类

**Files:**

- Create: `src/main/java/cn/projectan/strix/core/module/ai/provider/BaseOpenAICompatAdapter.java`

- [ ] **Step 1: 创建 BaseOpenAICompatAdapter**

```java
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

        // 缓存命中
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

    /** 标准 OpenAI delta 无 reasoning_content 字段 */
    @Override
    public String extractReasoningContent(JsonNode delta) {
        return null;
    }
}
```

---

## Task 4: DashScopeProviderAdapter

**Files:**

- Create: `src/main/java/cn/projectan/strix/core/module/ai/provider/DashScopeProviderAdapter.java`

- [ ] **Step 1: 创建 DashScopeProviderAdapter**

```java
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

        // 推理力度（DeepSeek-V4 / DashScope 均支持）
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

        // DashScope 缓存写入字段名
        int cacheWriteTokens = usageNode.path("cache_creation_input_tokens").asInt(-1);

        return new AiUsageDetail(
                base.promptTokens(),
                base.completionTokens(),
                base.cacheHitTokens(),
                cacheWriteTokens >= 0 ? cacheWriteTokens : base.cacheWriteTokens(),
                base.reasoningTokens()
        );
    }

    /** DashScope 流式 delta 使用 reasoning_content 字段 */
    @Override
    public String extractReasoningContent(JsonNode delta) {
        JsonNode node = delta.get("reasoning_content");
        if (node == null || node.isNull()) return null;
        String val = node.asText("");
        return val.isEmpty() ? null : val;
    }
}
```

---

## Task 5: DeepSeekProviderAdapter 和 DefaultOpenAIProviderAdapter

**Files:**

- Create: `src/main/java/cn/projectan/strix/core/module/ai/provider/DeepSeekProviderAdapter.java`
- Create: `src/main/java/cn/projectan/strix/core/module/ai/provider/DefaultOpenAIProviderAdapter.java`

- [ ] **Step 1: 创建 DeepSeekProviderAdapter**

DeepSeek 的差异：

- reasoning_effort 参数支持
- 缓存字段：`prompt_cache_hit_tokens`（命中）、`prompt_cache_miss_tokens`（未命中/写入）
- 流式 delta 有 `reasoning_content` 字段（与 DashScope 相同）

```java
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
        // DeepSeek 支持 reasoning_effort
        if (StringUtils.hasText(config.getReasoningEffort())) {
            body.put("reasoning_effort", config.getReasoningEffort());
        }
        // DeepSeek 不使用 top_k、repetition_penalty、enable_thinking 等 DashScope 特有参数
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

    /** DeepSeek 同样使用 reasoning_content 字段 */
    @Override
    public String extractReasoningContent(JsonNode delta) {
        JsonNode node = delta.get("reasoning_content");
        if (node == null || node.isNull()) return null;
        String val = node.asText("");
        return val.isEmpty() ? null : val;
    }
}
```

- [ ] **Step 2: 创建 DefaultOpenAIProviderAdapter（兜底）**

```java
package cn.projectan.strix.core.module.ai.provider;

import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiProviderType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 兜底适配器：标准 OpenAI 及其他兼容端点
 * <p>
 * 仅使用 {@link BaseOpenAICompatAdapter} 定义的标准 OpenAI 参数，无额外特有参数。
 * {@code @Order(Integer.MAX_VALUE)} 确保此 bean 排在所有专用 adapter 之后，
 * 仅在无匹配专用 adapter 时生效。
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Order(Integer.MAX_VALUE)
@Component
public class DefaultOpenAIProviderAdapter extends BaseOpenAICompatAdapter {

    @Override
    public boolean supports(AiModelConfig config) {
        // 兜底：总是返回 true
        return true;
    }
}
```

---

## Task 6: AiProviderRegistry

**Files:**

- Create: `src/main/java/cn/projectan/strix/core/module/ai/provider/AiProviderRegistry.java`

- [ ] **Step 1: 创建 AiProviderRegistry**

```java
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
```

---

## Task 7: AiChatClient（统一 OkHttp 聊天客户端）

**Files:**

- Create: `src/main/java/cn/projectan/strix/core/module/ai/AiChatClient.java`

- [ ] **Step 1: 创建 AiChatClient**

```java
package cn.projectan.strix.core.module.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 统一 AI 聊天客户端（OkHttp）
 * <p>
 * 支持流式（SSE）和非流式两种调用模式，所有 /chat/completions 请求均通过此类发出。
 * <p>
 * <b>线程安全：</b>OkHttpClient 是线程安全的，该 Bean 为单例。
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Slf4j
@Component
public class AiChatClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(5))
            .writeTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 非流式聊天（同步阻塞，返回完整响应 JsonNode）
     *
     * @param baseUrl API 基础 URL
     * @param apiKey  API Key（Bearer token）
     * @param body    请求体 Map（不含 stream 字段，由此方法自动设为 false）
     * @return 完整响应 JsonNode
     * @throws IOException 网络/HTTP 错误
     */
    public JsonNode chat(String baseUrl, String apiKey, Map<String, Object> body) throws IOException {
        body.put("stream", false);
        String url = normalizeUrl(baseUrl);
        String jsonBody = MAPPER.writeValueAsString(body);
        Request request = buildRequest(url, apiKey, jsonBody);

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("AI API 返回错误 " + response.code() + ": " + responseBody);
            }
            return MAPPER.readTree(responseBody);
        }
    }

    /**
     * 流式聊天（SSE，逐行回调 chunk JsonNode）
     *
     * @param baseUrl API 基础 URL
     * @param apiKey  API Key
     * @param body    请求体 Map（不含 stream 字段，由此方法自动设为 true）
     * @param handler 每个 SSE data chunk 的回调（chunk 为解析后的 JsonNode）
     * @throws IOException 网络/HTTP/流读取错误
     */
    public void streamChat(String baseUrl, String apiKey, Map<String, Object> body,
                           SseChunkHandler handler) throws IOException {
        body.put("stream", true);
        String url = normalizeUrl(baseUrl);
        String jsonBody = MAPPER.writeValueAsString(body);
        Request request = buildRequest(url, apiKey, jsonBody);

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("AI API 返回错误 " + response.code() + ": " + errorBody);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || !line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        handler.onChunk(MAPPER.readTree(data));
                    } catch (Exception e) {
                        log.debug("AI: 解析 SSE chunk 失败，跳过: {}", data);
                    }
                }
            }
        }
    }

    /**
     * SSE chunk 回调接口（每个有效 data 行触发一次）
     */
    @FunctionalInterface
    public interface SseChunkHandler {
        void onChunk(JsonNode chunk) throws IOException;
    }

    private Request buildRequest(String url, String apiKey, String jsonBody) {
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON_TYPE))
                .build();
    }

    private String normalizeUrl(String baseUrl) {
        return baseUrl.replaceAll("/+$", "") + "/chat/completions";
    }
}
```

---

## Task 8: 重写 AiClientFactory 和 AiModelStore（移除 Spring AI）

**Files:**

- Modify: `src/main/java/cn/projectan/strix/core/module/ai/AiClientFactory.java`
- Modify: `src/main/java/cn/projectan/strix/core/module/ai/AiModelStore.java`

- [ ] **Step 1: 重写 AiClientFactory**

`AiChatClient` 是单例 Bean，`AiClientFactory` 现在只需要负责其他类型客户端（如 DashScope 原生 API WebSocket 客户端），聊天类调用全部通过
`AiChatClient`。

由于 `AiModelStore` 原本缓存 `OpenAiChatModel` 和 `OpenAIClient`，现在 `AiChatClient` 是单例无需缓存，`AiModelStore`
简化为仅在其他地方（如 TTS、STT、ASR 的 WebSocket）有需要时才使用。

将 `AiClientFactory.java` 替换为：

```java
package cn.projectan.strix.core.module.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 客户端工厂（保留为扩展点）
 * <p>
 * 聊天类调用已统一由 {@link AiChatClient}（OkHttp）处理，无需此工厂创建聊天客户端。
 * 此类保留用于未来可能的特殊客户端创建需求（如需要特殊认证的提供商）。
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Slf4j
@Component
public class AiClientFactory {
    // 聊天客户端统一使用 AiChatClient（Bean 注入），无需工厂方法
    // 此类保留为扩展点，暂无实现
}
```

- [ ] **Step 2: 重写 AiModelStore**

`AiModelStore` 原先缓存 Spring AI `OpenAiChatModel` 和 OpenAI SDK `OpenAIClient`。移除后，它只需负责在配置变更时通知相关组件（可保留
`invalidate` 方法以备使用）：

```java
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
```

---

## Task 9: 重写 AiService（核心）

**Files:**

- Modify: `src/main/java/cn/projectan/strix/service/system/AiService.java`

这是最核心的改动。将整个 `AiService.java` 重写，移除所有 Spring AI 依赖，统一使用 `AiChatClient` + `AiProviderAdapter`。

> **注意**：以下为完整的新版 `AiService.java`，直接替换原文件内容。

- [ ] **Step 1: 替换 AiService.java**

```java
package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.ai.AiAttachmentResolver;
import cn.projectan.strix.core.module.ai.AiChatClient;
import cn.projectan.strix.core.module.ai.provider.AiProviderAdapter;
import cn.projectan.strix.core.module.ai.provider.AiProviderRegistry;
import cn.projectan.strix.core.module.ai.provider.AiUsageDetail;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiSession;
import cn.projectan.strix.model.dict.system.AiMessageStatus;
import cn.projectan.strix.model.request.system.module.ai.AiAttachment;
import cn.projectan.strix.model.response.system.ai.AiSseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 核心服务
 * <p>
 * 统一使用 {@link AiChatClient}（OkHttp）调用 OpenAI 兼容端点，
 * 通过 {@link AiProviderAdapter} 处理各提供商特有参数差异。
 * <p>
 * 两种调用方式：
 * <ul>
 *   <li>在线对话（SSE 流式）：{@link #streamChat} / {@link #streamRegenerate}</li>
 *   <li>程序化调用（同步阻塞）：{@link #chat} / {@link #analyzeMedia}</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-06-25 (重构自 Spring AI 版本)
 */
@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AiService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiAttachmentResolver aiAttachmentResolver;
    private final AiChatClient aiChatClient;
    private final AiProviderRegistry providerRegistry;
    private final AiModelConfigService aiModelConfigService;
    private final AiSessionService aiSessionService;
    private final AiMessageService aiMessageService;

    // ============================================================
    //  流式在线对话（SSE）
    // ============================================================

    /**
     * 流式 AI 对话，将结果推送到 {@link SseEmitter}。应在虚拟线程中调用。
     */
    public void streamChat(String sessionId, String content, List<AiAttachment> attachments,
                           SseEmitter emitter, String managerId) {
        AiSession session = aiSessionService.getById(sessionId);
        if (session == null || !session.getManagerId().equals(managerId)) {
            sendSseError(emitter, "会话不存在或无权限");
            return;
        }
        boolean hasContent = StringUtils.hasText(content);
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        if (!hasContent && !hasAttachments) {
            sendSseError(emitter, "消息内容不能为空");
            return;
        }

        AiModelConfig config = aiModelConfigService.getById(session.getModelConfigId());
        if (config == null || config.getStatus() == null || config.getStatus() != 1) {
            sendSseError(emitter, "AI 模型配置不可用");
            return;
        }

        long startTime = System.currentTimeMillis();

        List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments = aiAttachmentResolver.resolve(attachments);

        AiMessage userMsg = new AiMessage()
                .setSessionId(sessionId)
                .setRole("user")
                .setContent(content)
                .setAttachments(attachmentsToJson(attachments))
                .setModelConfigId(config.getId())
                .setStatus(AiMessageStatus.COMPLETED);
        aiMessageService.save(userMsg);

        AiMessage assistantMsg = new AiMessage()
                .setSessionId(sessionId)
                .setRole("assistant")
                .setContent("")
                .setStatus(AiMessageStatus.GENERATING);
        aiMessageService.save(assistantMsg);

        List<AiMessage> history = aiMessageService.listContextMessages(sessionId);
        List<Map<String, Object>> messages = buildRawMessages(config, history, content, resolvedAttachments);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);

        AiProviderAdapter adapter = providerRegistry.getAdapter(config);
        adapter.applyStreamingParams(body, config);

        runStreaming(config, body, adapter, assistantMsg.getId(), userMsg.getId(), emitter, startTime, sessionId);
    }

    /**
     * 重新生成最后一条 AI 回复（SSE 流式）。应在虚拟线程中调用。
     */
    public void streamRegenerate(String sessionId, SseEmitter emitter, String managerId) {
        AiSession session = aiSessionService.getById(sessionId);
        if (session == null || !session.getManagerId().equals(managerId)) {
            sendSseError(emitter, "会话不存在或无权限");
            return;
        }
        AiModelConfig config = aiModelConfigService.getById(session.getModelConfigId());
        if (config == null || config.getStatus() == null || config.getStatus() != 1) {
            sendSseError(emitter, "AI 模型配置不可用");
            return;
        }

        long startTime = System.currentTimeMillis();

        AiMessage lastAssistant = aiMessageService.lambdaQuery()
                .eq(AiMessage::getSessionId, sessionId)
                .eq(AiMessage::getRole, "assistant")
                .orderByDesc(AiMessage::getId)
                .last("LIMIT 1")
                .one();
        if (lastAssistant != null) {
            aiMessageService.removeById(lastAssistant.getId());
        }

        AiMessage lastUser = aiMessageService.lambdaQuery()
                .eq(AiMessage::getSessionId, sessionId)
                .eq(AiMessage::getRole, "user")
                .orderByDesc(AiMessage::getId)
                .last("LIMIT 1")
                .one();
        if (lastUser == null) {
            sendSseError(emitter, "没有可以重新生成的用户消息");
            return;
        }

        List<AiAttachment> attachments = parseAiAttachmentsJson(lastUser.getAttachments());
        List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments = aiAttachmentResolver.resolve(attachments);

        AiMessage assistantMsg = new AiMessage()
                .setSessionId(sessionId)
                .setRole("assistant")
                .setContent("")
                .setStatus(AiMessageStatus.GENERATING);
        aiMessageService.save(assistantMsg);

        List<AiMessage> history = aiMessageService.listContextMessages(sessionId);
        List<Map<String, Object>> messages = buildRawMessages(config, history, lastUser.getContent(), resolvedAttachments);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);

        AiProviderAdapter adapter = providerRegistry.getAdapter(config);
        adapter.applyStreamingParams(body, config);

        runStreaming(config, body, adapter, assistantMsg.getId(), null, emitter, startTime, sessionId);
    }

    /**
     * 统一流式执行（单一 OkHttp 路径，同时处理纯文本和多模态）
     */
    private void runStreaming(AiModelConfig config, Map<String, Object> body, AiProviderAdapter adapter,
                              String assistantMsgId, String userMsgId,
                              SseEmitter emitter, long startTime, String sessionId) {
        StringBuilder fullContent = new StringBuilder();
        StringBuilder thinkingContent = new StringBuilder();
        AiUsageDetail[] usageHolder = {AiUsageDetail.EMPTY};

        try {
            aiChatClient.streamChat(config.getBaseUrl(), config.getApiKey(), body, chunk -> {
                // Usage（通常在最后一个 chunk）
                JsonNode usageNode = chunk.get("usage");
                if (usageNode != null && !usageNode.isNull()) {
                    usageHolder[0] = adapter.parseUsage(usageNode);
                }

                JsonNode choices = chunk.get("choices");
                if (choices == null || choices.isEmpty()) return;
                JsonNode choice = choices.get(0);
                JsonNode delta = choice.get("delta");
                if (delta == null) return;

                // 思考内容（provider 特有字段）
                String thinkingDelta = adapter.extractReasoningContent(delta);
                if (StringUtils.hasText(thinkingDelta)) {
                    thinkingContent.append(thinkingDelta);
                    sendSseEvent(emitter, AiSseEvent.THINKING, Map.of("content", thinkingDelta));
                }

                // 正文内容
                JsonNode contentNode = delta.get("content");
                if (contentNode != null && !contentNode.isNull()) {
                    String contentDelta = contentNode.asText("");
                    if (!contentDelta.isEmpty()) {
                        fullContent.append(contentDelta);
                        sendSseEvent(emitter, AiSseEvent.CONTENT, Map.of("content", contentDelta));
                    }
                }
            });

            AiUsageDetail usage = usageHolder[0];
            Long durationMs = System.currentTimeMillis() - startTime;

            aiMessageService.markCompleted(assistantMsgId,
                    fullContent.toString(),
                    thinkingContent.isEmpty() ? null : thinkingContent.toString(),
                    usage.promptTokens(), usage.completionTokens(),
                    usage.cacheHitTokens(), usage.cacheWriteTokens(), usage.reasoningTokens(),
                    config.getId(), durationMs);

            Map<String, Object> doneData = new HashMap<>();
            doneData.put("messageId", assistantMsgId);
            if (userMsgId != null) doneData.put("userMessageId", userMsgId);
            doneData.put("modelConfigId", config.getId());
            doneData.put("modelConfigName", config.getName());
            if (usage.promptTokens() != null) doneData.put("promptTokens", usage.promptTokens());
            if (usage.completionTokens() != null) doneData.put("completionTokens", usage.completionTokens());
            if (usage.cacheHitTokens() != null) doneData.put("cacheHitTokens", usage.cacheHitTokens());
            if (usage.cacheWriteTokens() != null) doneData.put("cacheWriteTokens", usage.cacheWriteTokens());
            if (usage.reasoningTokens() != null) doneData.put("reasoningTokens", usage.reasoningTokens());
            sendSseEvent(emitter, AiSseEvent.DONE, doneData);
            emitter.complete();

        } catch (Exception e) {
            log.error("AI 流式调用出错: sessionId={}", sessionId, e);
            aiMessageService.markError(assistantMsgId, e.getMessage());
            sendSseError(emitter, "AI 调用出错: " + e.getMessage());
        }
    }

    // ============================================================
    //  程序化调用 - 文本/视觉对话（同步阻塞）
    // ============================================================

    /**
     * 同步文本对话（程序化调用）
     *
     * @param configKey 模型配置 key
     * @param messages  消息列表（[{"role":"user","content":"..."}]）
     * @return AI 响应文本
     */
    public String chat(String configKey, List<Map<String, Object>> messages) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        AiProviderAdapter adapter = providerRegistry.getAdapter(config);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);
        adapter.applyNonStreamingParams(body, config);

        try {
            JsonNode response = aiChatClient.chat(config.getBaseUrl(), config.getApiKey(), body);
            return response.at("/choices/0/message/content").asText("");
        } catch (IOException e) {
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 同步文本对话（简单单轮，程序化调用）
     *
     * @param configKey 模型配置 key
     * @param userInput 用户输入
     * @return AI 响应文本
     */
    public String chat(String configKey, String userInput) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        List<Map<String, Object>> messages = new ArrayList<>();
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", userInput));
        return chat(configKey, messages);
    }

    /**
     * 视觉模型分析图片/视频（程序化调用）
     *
     * @param configKey 模型配置 key
     * @param prompt    文本提示
     * @param mediaUrls 媒体 URL 列表（图片或视频公网 URL）
     * @param mimeTypes 对应的 MIME 类型（如 "image/jpeg"），与 mediaUrls 一一对应
     * @return AI 分析结果文本
     */
    public String analyzeMedia(String configKey, String prompt, List<String> mediaUrls, List<String> mimeTypes) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        AiProviderAdapter adapter = providerRegistry.getAdapter(config);

        List<Map<String, Object>> contentParts = new ArrayList<>();
        if (StringUtils.hasText(prompt)) {
            contentParts.add(Map.of("type", "text", "text", prompt));
        }
        for (int i = 0; i < mediaUrls.size(); i++) {
            contentParts.add(Map.of("type", "image_url", "image_url", Map.of("url", mediaUrls.get(i))));
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", (Object) contentParts));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);
        adapter.applyNonStreamingParams(body, config);

        try {
            JsonNode response = aiChatClient.chat(config.getBaseUrl(), config.getApiKey(), body);
            return response.at("/choices/0/message/content").asText("");
        } catch (IOException e) {
            throw new RuntimeException("AI 媒体分析失败: " + e.getMessage(), e);
        }
    }

    // ============================================================
    //  内部辅助方法
    // ============================================================

    /**
     * 根据历史消息和当前输入构建原始 messages（Map 结构，直接序列化为 JSON）。
     * 包级可见以便单元测试。
     */
    List<Map<String, Object>> buildRawMessages(AiModelConfig config, List<AiMessage> history,
                                               String currentContent,
                                               List<AiAttachmentResolver.ResolvedAttachment> resolvedAttachments) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // System prompt
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
        }

        // 历史上下文（排除当前刚保存的 user 消息——跳过最后 1 条）
        int skipLast = 1;
        List<AiMessage> contextHistory = history.size() > skipLast
                ? history.subList(0, history.size() - skipLast)
                : List.of();

        for (AiMessage msg : contextHistory) {
            if ("user".equals(msg.getRole())) {
                messages.add(Map.of("role", "user",
                        "content", msg.getContent() != null ? msg.getContent() : ""));
            } else if ("assistant".equals(msg.getRole())) {
                Map<String, Object> assistantMsg = new LinkedHashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", msg.getContent() != null ? msg.getContent() : "");
                // preserve_thinking 由 Provider 在 body 中处理，消息本身不嵌入思考内容
                messages.add(assistantMsg);
            }
        }

        // 当前用户消息
        if (resolvedAttachments != null && !resolvedAttachments.isEmpty()) {
            // 多模态：content 为 parts 数组
            List<Map<String, Object>> parts = new ArrayList<>();
            if (StringUtils.hasText(currentContent)) {
                parts.add(Map.of("type", "text", "text", currentContent));
            }
            for (AiAttachmentResolver.ResolvedAttachment att : resolvedAttachments) {
                switch (att.getType()) {
                    case "image" -> parts.add(Map.of("type", "image_url",
                            "image_url", Map.of("url", att.getDataUrl())));
                    case "video" -> parts.add(Map.of("type", "video_url",
                            "video_url", Map.of("url", att.getDataUrl())));
                    case "audio" -> {
                        Map<String, Object> audioData = new LinkedHashMap<>();
                        audioData.put("data", att.getDataUrl());
                        if (att.getFormat() != null) audioData.put("format", att.getFormat());
                        parts.add(Map.of("type", "input_audio", "input_audio", audioData));
                    }
                }
            }
            messages.add(Map.of("role", "user", "content", (Object) parts));
        } else {
            messages.add(Map.of("role", "user",
                    "content", currentContent != null ? currentContent : ""));
        }

        return messages;
    }

    private String attachmentsToJson(List<AiAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(attachments);
        } catch (Exception e) {
            log.warn("AI: 序列化附件失败", e);
            return null;
        }
    }

    private List<AiAttachment> parseAiAttachmentsJson(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<AiAttachment>>() {
            });
        } catch (Exception e) {
            log.warn("解析附件 JSON 失败: {}", json, e);
            return null;
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.debug("AI SSE 发送事件失败: event={}", eventName);
        }
    }

    private void sendSseError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name(AiSseEvent.ERROR).data(Map.of("message", message)));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }
}
```

- [ ] **Step 2: 更新测试文件**

`AiServiceContextTest.java` 调用 `buildMessages()`（Spring AI Message 类型），需完整替换为新版：

完整替换 `src/test/java/cn/projectan/strix/service/system/AiServiceContextTest.java` 内容为：

```java
package cn.projectan.strix.service.system;

import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link AiService#buildRawMessages} 上下文构建的纯单元测试（不依赖 Spring 容器 / 数据库 / 网络）。
 */
class AiServiceContextTest {

    /** buildRawMessages 不使用任何注入依赖，传 null 即可构造（6 个依赖全为 null） */
    private final AiService aiService = new AiService(null, null, null, null, null, null);

    private static AiMessage msg(String role, String content) {
        return new AiMessage().setRole(role).setContent(content);
    }

    private static String textOf(Map<String, Object> msg) {
        Object content = msg.get("content");
        return content instanceof String s ? s : "";
    }

    @Test
    @DisplayName("buildRawMessages 仅跳过刚保存的 user 消息，保留上一轮 assistant 回复")
    void buildMessagesKeepsLastAssistant() {
        AiModelConfig config = new AiModelConfig().setType(AiModelType.TEXT);

        List<AiMessage> history = List.of(
                msg("user", "u1"), msg("assistant", "a1"),
                msg("user", "u2"), msg("assistant", "a2"),
                msg("user", "u3"));

        List<Map<String, Object>> messages = aiService.buildRawMessages(config, history, "u3", null);

        List<String> texts = messages.stream().map(AiServiceContextTest::textOf).toList();
        assertEquals(List.of("u1", "a1", "u2", "a2", "u3"), texts,
                "应保留上一轮 assistant 回复 a2，且当前输入 u3 仅出现一次");
    }

    @Test
    @DisplayName("buildRawMessages 含 system prompt 时置于首位")
    void buildMessagesWithSystemPrompt() {
        AiModelConfig config = new AiModelConfig().setType(AiModelType.TEXT).setSystemPrompt("你是助手");

        List<AiMessage> history = List.of(msg("user", "hi"));
        List<Map<String, Object>> messages = aiService.buildRawMessages(config, history, "hi", null);

        List<String> texts = messages.stream().map(AiServiceContextTest::textOf).toList();
        assertEquals(List.of("你是助手", "hi"), texts, "system prompt 应在首位，当前输入随后");
    }

    @Test
    @DisplayName("buildRawMessages 首轮无历史时仅含当前输入")
    void buildMessagesFirstTurn() {
        AiModelConfig config = new AiModelConfig().setType(AiModelType.TEXT);

        List<AiMessage> history = List.of(msg("user", "first"));
        List<Map<String, Object>> messages = aiService.buildRawMessages(config, history, "first", null);

        List<String> texts = messages.stream().map(AiServiceContextTest::textOf).toList();
        assertEquals(List.of("first"), texts, "首轮应只含当前输入一条");
    }
}
```

---

## Task 10: 移除 Spring AI 依赖

**Files:**

- Modify: `build.gradle`

- [ ] **Step 1: 从 build.gradle 移除 spring-ai-openai**

找到：

```groovy
implementation "org.springframework.ai:spring-ai-openai"
```

删除此行。保留 OkHttp（已有 `com.squareup.okhttp3:okhttp:5.3.2`）。

- [ ] **Step 2: 验证编译**

```bash
cd Strix && ./gradlew build -x test 2>&1 | Select-Object -Last 8
```

期望：`BUILD SUCCESSFUL`

若有残留的 `org.springframework.ai.*` import 报错，检查 `DashScopeAiService`、`AiTtsWebSocketHandler` 等是否还有 Spring AI
依赖。这些文件依赖 DashScope 原生 API（非 OpenAI 兼容），不受此次重构影响，但若有 `spring-ai-openai` 的 import 需逐一排查。

---

## Task 11: 前端 — providerType 字段支持

**Files:**

- Modify: `StrixPage/src/api/ai.ts`
- Modify: `StrixPage/src/views/System/SystemAi/AiModelConfig/AiModelConfigForm.vue`

- [ ] **Step 1: 更新 api/ai.ts**

在 `AiModelConfigResp` 接口中添加：

```typescript
/** 云提供商类型：0=自动识别 1=DashScope 2=DeepSeek 3=OpenAI 9=其他兼容 */
providerType ? : number
```

在 `AiModelConfigUpdateReq` 接口中添加：

```typescript
providerType ? : number
```

- [ ] **Step 2: 更新 AiModelConfigForm.vue**

在模型类型选择下方新增提供商类型选择：

```html

<n-form-item label="提供商类型" path="providerType">
    <n-space vertical style="width: 100%">
        <n-select
                v-model:value="form.providerType"
                :options="providerTypeOptions"
                placeholder="默认自动识别（按 Base URL 判断）"
                clearable
        />
        <n-text depth="3" style="font-size: 12px">显式指定提供商类型可确保参数正确注入；留空则按 Base URL 自动识别
        </n-text>
    </n-space>
</n-form-item>
```

在 `<script>` 中添加 options 定义：

```typescript
const providerTypeOptions = [
    {label: '自动识别（按 Base URL）', value: 0},
    {label: 'DashScope（阿里云百炼）', value: 1},
    {label: 'DeepSeek', value: 2},
    {label: 'OpenAI', value: 3},
    {label: '其他兼容端点', value: 9}
]
```

在 `getDefaultForm()` 中添加：

```typescript
providerType: 0
```

- [ ] **Step 3: 运行前端类型检查**

```bash
cd StrixPage && pnpm type-check
```

---

## Task 12: 回归验证

- [ ] **Step 1: 后端完整构建**

```bash
cd Strix && ./gradlew build 2>&1 | Select-Object -Last 10
```

期望：`BUILD SUCCESSFUL`（含测试通过）

- [ ] **Step 2: 前端类型检查**

```bash
cd StrixPage && pnpm type-check
```

期望：无错误

---

## 自检 — 规格覆盖

| 需求                             | 对应 Task                               |
|--------------------------------|---------------------------------------|
| 移除 Spring AI                   | Task 10                               |
| 流式和非流式统一 OkHttp                | Task 7, 9                             |
| Provider Adapter 模式            | Task 2-6                              |
| DashScope 特有参数（完整）             | Task 4                                |
| DeepSeek 特有参数                  | Task 5                                |
| `providerType` 字段              | Task 1, 11                            |
| 流式与非流式参数差异（code_interpreter 等） | Task 3, 4（`streaming` bool 参数）        |
| Usage 解析差异（cache 字段名）          | Task 4, 5（覆盖 parseUsage）              |
| thinking content 提取差异          | Task 4, 5（覆盖 extractReasoningContent） |
| 默认兜底 Adapter                   | Task 5（DefaultOpenAIProviderAdapter）  |
| 前端配置界面                         | Task 11                               |
| DB 迁移                          | Task 1                                |
