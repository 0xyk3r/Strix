package cn.projectan.strix.core.module.ai.provider;

import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeepSeekProviderAdapterTest {

    private DeepSeekProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DeepSeekProviderAdapter();
    }

    // ── supports ──────────────────────────────────────────────────────────

    @Test
    void supports_deepseekProviderType_returnsTrue() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setBaseUrl("https://example.com/v1");
        assertTrue(adapter.supports(config));
    }

    @Test
    void supports_deepseekBaseUrl_returnsTrue() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.AUTO)
                .setBaseUrl("https://api.deepseek.com/v1");
        assertTrue(adapter.supports(config));
    }

    @Test
    void supports_dashscopeProviderType_returnsFalse() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DASHSCOPE)
                .setBaseUrl("https://api.deepseek.com/v1");
        assertFalse(adapter.supports(config));
    }

    // ── thinking object ────────────────────────────────────────────────────

    @Test
    void applyParams_enableThinkingOn_injectsThinkingEnabledObject() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setEnableThinking((short) 1);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("temperature", 0.7);

        adapter.applyNonStreamingParams(body, config);

        @SuppressWarnings("unchecked")
        Map<String, Object> thinking = (Map<String, Object>) body.get("thinking");
        assertNotNull(thinking, "thinking object must be present");
        assertEquals("enabled", thinking.get("type"));
    }

    @Test
    void applyParams_enableThinkingOff_injectsThinkingDisabledObject() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setEnableThinking((short) 0);
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        @SuppressWarnings("unchecked")
        Map<String, Object> thinking = (Map<String, Object>) body.get("thinking");
        assertNotNull(thinking);
        assertEquals("disabled", thinking.get("type"));
    }

    @Test
    void applyParams_enableThinkingNull_doesNotInjectThinkingObject() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setEnableThinking(null);
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        assertNull(body.get("thinking"), "thinking object must NOT be present when enableThinking is null");
    }

    // ── incompatible param suppression ────────────────────────────────────

    @Test
    void applyParams_thinkingEnabled_suppressesIncompatibleParams() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setEnableThinking((short) 1)
                .setTemperature(new BigDecimal("0.8"))
                .setTopP(new BigDecimal("0.9"))
                .setPresencePenalty(new BigDecimal("0.5"))
                .setFrequencyPenalty(new BigDecimal("0.3"));
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        assertNull(body.get("temperature"), "temperature must be removed in thinking mode");
        assertNull(body.get("top_p"), "top_p must be removed in thinking mode");
        assertNull(body.get("presence_penalty"), "presence_penalty must be removed in thinking mode");
        assertNull(body.get("frequency_penalty"), "frequency_penalty must be removed in thinking mode");
    }

    @Test
    void applyParams_thinkingDisabled_keepsCompatibleParams() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setEnableThinking((short) 0)
                .setTemperature(new BigDecimal("0.8"));
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        assertNotNull(body.get("temperature"), "temperature must be kept when thinking is disabled");
    }

    // ── reasoning_effort normalization ───────────────────────────────────

    @Test
    void applyParams_reasoningEffortHigh_passesThrough() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setReasoningEffort("high");
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        assertEquals("high", body.get("reasoning_effort"));
    }

    @Test
    void applyParams_reasoningEffortLow_normalizesToHigh() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setReasoningEffort("low");
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        assertEquals("high", body.get("reasoning_effort"));
    }

    @Test
    void applyParams_reasoningEffortMedium_normalizesToHigh() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setReasoningEffort("medium");
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        assertEquals("high", body.get("reasoning_effort"));
    }

    @Test
    void applyParams_reasoningEffortXhigh_normalizesToMax() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setReasoningEffort("xhigh");
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        assertEquals("max", body.get("reasoning_effort"));
    }

    @Test
    void applyParams_reasoningEffortMax_passesThrough() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setReasoningEffort("max");
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        assertEquals("max", body.get("reasoning_effort"));
    }

    @Test
    void applyParams_reasoningEffortNull_notInjected() {
        AiModelConfig config = new AiModelConfig()
                .setProviderType((short) AiProviderType.DEEPSEEK)
                .setModelName("deepseek-v4-pro")
                .setReasoningEffort(null);
        Map<String, Object> body = new LinkedHashMap<>();

        adapter.applyNonStreamingParams(body, config);

        assertNull(body.get("reasoning_effort"));
    }

    // ── capabilities ──────────────────────────────────────────────────────

    @Test
    void supportsFim_returnsTrue() {
        assertTrue(adapter.supportsFim());
    }

    @Test
    void supportsChatPrefix_returnsTrue() {
        assertTrue(adapter.supportsChatPrefix());
    }
}
