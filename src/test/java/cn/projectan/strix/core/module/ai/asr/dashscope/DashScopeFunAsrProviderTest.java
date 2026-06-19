package cn.projectan.strix.core.module.ai.asr.dashscope;

import cn.hutool.json.JSONObject;
import cn.projectan.strix.core.module.ai.asr.AsrSessionParams;
import cn.projectan.strix.model.db.system.AiModelConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DashScopeFunAsrProviderTest {

    private final DashScopeFunAsrProvider provider = new DashScopeFunAsrProvider(null);

    private AiModelConfig config(String model) {
        AiModelConfig c = new AiModelConfig();
        c.setModelName(model);
        return c;
    }

    @Test
    void supports_funAsr() {
        assertTrue(provider.supports(config("fun-asr-realtime")));
        assertTrue(provider.supports(config("fun-asr-flash-8k-realtime")));
        assertFalse(provider.supports(config("paraformer-realtime-v2")));
        assertFalse(provider.supports(config("qwen3-asr-flash-realtime")));
    }

    @Test
    void funAsr_neverSupportsEmotion() {
        assertFalse(provider.supportsEmotion(config("fun-asr-realtime")));
    }

    @Test
    void buildParameters_includesSpeechNoiseThreshold_andOmitsNulls() {
        AsrSessionParams p = AsrSessionParams.fromJson(
                "{\"speechNoiseThreshold\":0.3,\"maxSentenceSilence\":800,"
                        + "\"languageHints\":[\"zh\"],\"vocabularyId\":\"v1\"}");
        JSONObject params = provider.buildParameters(config("fun-asr-realtime"), p);
        assertEquals(0.3, params.getDouble("speech_noise_threshold"));
        assertEquals(800, params.getInt("max_sentence_silence"));
        assertEquals(List.of("zh"), params.getJSONArray("language_hints").toList(String.class));
        assertEquals("v1", params.getStr("vocabulary_id"));
        // Fun-ASR 不支持顺滑/标点/ITN → 不应出现
        assertFalse(params.containsKey("disfluency_removal_enabled"));
        assertFalse(params.containsKey("punctuation_prediction_enabled"));
        assertFalse(params.containsKey("inverse_text_normalization_enabled"));
    }

    @Test
    void buildParameters_emptyParams_emptyObject() {
        JSONObject params = provider.buildParameters(config("fun-asr-realtime"), AsrSessionParams.empty());
        assertTrue(params.isEmpty());
    }
}
