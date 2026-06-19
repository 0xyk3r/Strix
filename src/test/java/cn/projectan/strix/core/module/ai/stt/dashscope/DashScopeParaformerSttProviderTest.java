package cn.projectan.strix.core.module.ai.stt.dashscope;

import cn.hutool.json.JSONObject;
import cn.projectan.strix.core.module.ai.stt.SttParams;
import cn.projectan.strix.model.db.system.AiModelConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DashScopeParaformerSttProviderTest {

    private final DashScopeParaformerSttProvider provider = new DashScopeParaformerSttProvider(null);

    private AiModelConfig config(String model) {
        AiModelConfig c = new AiModelConfig();
        c.setModelName(model);
        return c;
    }

    @Test
    void supports_paraformerOffline_excludesRealtime() {
        assertTrue(provider.supports(config("paraformer-v2")));
        assertTrue(provider.supports(config("paraformer-8k-v2")));
        assertFalse(provider.supports(config("paraformer-realtime-v2")));
        assertFalse(provider.supports(config("fun-asr")));
        assertFalse(provider.supports(config("qwen3-asr-flash")));
    }

    @Test
    void buildInput_fileUrlsArray() {
        JSONObject input = provider.buildInput("https://x/a.wav");
        assertEquals("https://x/a.wav", input.getJSONArray("file_urls").getStr(0));
    }

    @Test
    void buildParameters_paraformerFields_omitsNulls() {
        SttParams p = SttParams.fromJson(
                "{\"languageHints\":[\"zh\"],\"vocabularyId\":\"v1\",\"diarizationEnabled\":true,"
                        + "\"speakerCount\":2,\"disfluencyRemovalEnabled\":true,\"timestampAlignmentEnabled\":true}");
        JSONObject params = provider.buildParameters(config("paraformer-v2"), p);
        assertEquals(List.of("zh"), params.getJSONArray("language_hints").toList(String.class));
        assertEquals("v1", params.getStr("vocabulary_id"));
        assertEquals(true, params.getBool("diarization_enabled"));
        assertEquals(2, params.getInt("speaker_count"));
        assertEquals(true, params.getBool("disfluency_removal_enabled"));
        assertEquals(true, params.getBool("timestamp_alignment_enabled"));
    }

    @Test
    void buildParameters_languageFallbackToConfig() {
        AiModelConfig c = config("paraformer-v2");
        c.setLanguage("en");
        JSONObject params = provider.buildParameters(c, SttParams.empty());
        assertEquals(List.of("en"), params.getJSONArray("language_hints").toList(String.class));
    }
}
