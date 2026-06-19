package cn.projectan.strix.core.module.ai.stt.dashscope;

import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.stt.SttResult;
import cn.projectan.strix.model.db.system.AiModelConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashScopeQwenFlashSttProviderTest {

    private final DashScopeQwenFlashSttProvider provider = new DashScopeQwenFlashSttProvider(null);

    private AiModelConfig config(String model) {
        AiModelConfig c = new AiModelConfig();
        c.setModelName(model);
        return c;
    }

    @Test
    void supports_qwenFlash_excludesFiletransAndRealtime() {
        assertTrue(provider.supports(config("qwen3-asr-flash")));
        assertTrue(provider.supports(config("qwen3-asr-flash-2026-02-10")));
        assertFalse(provider.supports(config("qwen3-asr-flash-filetrans")));
        assertFalse(provider.supports(config("qwen3-asr-flash-realtime")));
    }

    @Test
    void parseSyncResponse_textEmotionLanguage() {
        String out = "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                + "\"annotations\":[{\"type\":\"audio_info\",\"language\":\"zh\",\"emotion\":\"neutral\"}],"
                + "\"content\":[{\"text\":\"欢迎使用阿里云。\"}]}}]}";
        SttResult r = DashScopeQwenFlashSttProvider.parseSyncResponse(JSONUtil.parseObj(out));
        assertEquals("欢迎使用阿里云。", r.text());
        assertEquals("zh", r.language());
        assertEquals(1, r.sentences().size());
        assertEquals("neutral", r.sentences().get(0).emotion());
        assertNull(r.sentences().get(0).beginTime());
        assertNull(r.sentences().get(0).speakerId());
    }

    @Test
    void parseSyncResponse_empty() {
        SttResult r = DashScopeQwenFlashSttProvider.parseSyncResponse(JSONUtil.createObj());
        assertEquals("", r.text());
        assertTrue(r.sentences().isEmpty());
    }
}
