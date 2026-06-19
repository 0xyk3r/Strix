package cn.projectan.strix.core.module.ai.stt.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.stt.SttParams;
import cn.projectan.strix.model.db.system.AiModelConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashScopeQwenFiletransSttProviderTest {

    private final DashScopeQwenFiletransSttProvider provider = new DashScopeQwenFiletransSttProvider(null);

    private AiModelConfig config(String model) {
        AiModelConfig c = new AiModelConfig();
        c.setModelName(model);
        return c;
    }

    @Test
    void supports_qwenFiletrans() {
        assertTrue(provider.supports(config("qwen3-asr-flash-filetrans")));
        assertFalse(provider.supports(config("qwen3-asr-flash")));
        assertFalse(provider.supports(config("paraformer-v2")));
    }

    @Test
    void buildInput_singleFileUrl() {
        JSONObject input = provider.buildInput("https://x/a.mp3");
        assertEquals("https://x/a.mp3", input.getStr("file_url"));
        assertFalse(input.containsKey("file_urls"));
    }

    @Test
    void buildParameters_qwenFields() {
        SttParams p = SttParams.fromJson(
                "{\"language\":\"zh\",\"enableItn\":true,\"enableWords\":true,\"channelId\":[0]}");
        JSONObject params = provider.buildParameters(config("qwen3-asr-flash-filetrans"), p);
        assertEquals("zh", params.getStr("language"));
        assertEquals(true, params.getBool("enable_itn"));
        assertEquals(true, params.getBool("enable_words"));
        assertEquals(0, params.getJSONArray("channel_id").getInt(0));
    }

    @Test
    void extractResult_singleResultObject() {
        JSONObject output = JSONUtil.parseObj("{\"result\":{\"transcription_url\":\"https://x/r.json\"}}");
        assertEquals("https://x/r.json", provider.extractResult(output).getStr("transcription_url"));
    }
}
