package cn.projectan.strix.core.module.ai.tts.dashscope;

import cn.projectan.strix.model.db.system.AiModelConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashScopeCosyVoiceTtsProviderTest {

    private final DashScopeCosyVoiceTtsProvider provider = new DashScopeCosyVoiceTtsProvider(null);

    private AiModelConfig config(String model) {
        AiModelConfig c = new AiModelConfig();
        c.setModelName(model);
        return c;
    }

    @Test
    void supports_cosyVoice() {
        assertTrue(provider.supports(config("cosyvoice-v3.5-plus")));
        assertTrue(provider.supports(config("cosyvoice-v2")));
        assertTrue(provider.supports(config("CosyVoice-v3-flash")));
        assertFalse(provider.supports(config("qwen3-tts-flash")));
        assertFalse(provider.supports(config("MiniMax/speech-2.8-hd")));
    }

    @Test
    void supports_nullModel_false() {
        assertFalse(provider.supports(new AiModelConfig()));
    }
}
