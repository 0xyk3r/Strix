package cn.projectan.strix.core.module.ai.stt.dashscope;

import cn.hutool.json.JSONObject;
import cn.projectan.strix.core.module.ai.stt.SttParams;
import cn.projectan.strix.model.db.system.AiModelConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashScopeFunAsrSttProviderTest {

    private final DashScopeFunAsrSttProvider provider = new DashScopeFunAsrSttProvider(null);

    private AiModelConfig config(String model) {
        AiModelConfig c = new AiModelConfig();
        c.setModelName(model);
        return c;
    }

    @Test
    void supports_funAsrOffline_excludesRealtime() {
        assertTrue(provider.supports(config("fun-asr")));
        assertTrue(provider.supports(config("fun-asr-mtl")));
        assertFalse(provider.supports(config("fun-asr-realtime")));
        assertFalse(provider.supports(config("paraformer-v2")));
    }

    @Test
    void buildParameters_funAsrFields_noDisfluency() {
        SttParams p = SttParams.fromJson(
                "{\"languageHints\":[\"zh\",\"en\"],\"vocabularyId\":\"v1\","
                        + "\"diarizationEnabled\":true,\"speakerCount\":3,\"disfluencyRemovalEnabled\":true}");
        JSONObject params = provider.buildParameters(config("fun-asr"), p);
        assertEquals("v1", params.getStr("vocabulary_id"));
        assertEquals(true, params.getBool("diarization_enabled"));
        assertEquals(3, params.getInt("speaker_count"));
        assertEquals(2, params.getJSONArray("language_hints").size());
        // Fun-ASR 无顺滑/标点/ITN
        assertFalse(params.containsKey("disfluency_removal_enabled"));
        assertFalse(params.containsKey("timestamp_alignment_enabled"));
    }

    @Test
    void buildInput_fileUrlsArray() {
        assertEquals("https://x/a.wav",
                provider.buildInput("https://x/a.wav").getJSONArray("file_urls").getStr(0));
    }
}
