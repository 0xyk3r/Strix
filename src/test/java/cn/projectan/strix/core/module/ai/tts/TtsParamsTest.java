package cn.projectan.strix.core.module.ai.tts;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TtsParamsTest {

    @Test
    void fromJson_null_returnsEmptyInstance() {
        TtsParams p = TtsParams.fromJson(null);
        assertNotNull(p);
        assertNull(p.voice());
        assertNull(p.format());
        assertNull(p.rate());
    }

    @Test
    void fromJson_invalid_returnsEmptyInstance() {
        TtsParams p = TtsParams.fromJson("not-json");
        assertNotNull(p);
        assertNull(p.voice());
    }

    @Test
    void fromJson_parsesFields() {
        String json = "{\"voice\":\"v-123\",\"format\":\"mp3\",\"sampleRate\":24000,"
                + "\"volume\":80,\"rate\":1.2,\"pitch\":0.9,\"bitRate\":64,"
                + "\"instruction\":\"用激昂的语气\",\"enableSsml\":true,\"seed\":12345,"
                + "\"languageHints\":[\"zh\",\"en\"]}";
        TtsParams p = TtsParams.fromJson(json);
        assertEquals("v-123", p.voice());
        assertEquals("mp3", p.format());
        assertEquals(24000, p.sampleRate());
        assertEquals(80, p.volume());
        assertEquals(1.2, p.rate());
        assertEquals(0.9, p.pitch());
        assertEquals(64, p.bitRate());
        assertEquals("用激昂的语气", p.instruction());
        assertEquals(true, p.enableSsml());
        assertEquals(12345, p.seed());
        assertEquals(List.of("zh", "en"), p.languageHints());
    }

    @Test
    void merge_overrideTakesPrecedence_butNullKeepsBase() {
        TtsParams base = TtsParams.fromJson("{\"voice\":\"base-voice\",\"format\":\"wav\",\"rate\":1.0}");
        TtsParams override = TtsParams.fromJson("{\"rate\":1.5}");
        TtsParams merged = base.merge(override);
        // override 非空字段覆盖
        assertEquals(1.5, merged.rate());
        // override 为 null 的字段保留 base
        assertEquals("base-voice", merged.voice());
        assertEquals("wav", merged.format());
    }

    @Test
    void merge_voiceIdOverride_replacesVoice() {
        TtsParams base = TtsParams.fromJson("{\"voice\":\"old\",\"format\":\"mp3\"}");
        TtsParams voiceOverride = new TtsParams("new-voice", null, null, null, null, null, null, null, null, null, null);
        TtsParams merged = base.merge(voiceOverride);
        assertEquals("new-voice", merged.voice());
        assertEquals("mp3", merged.format());
    }

    @Test
    void merge_nullOverride_returnsBase() {
        TtsParams base = TtsParams.fromJson("{\"voice\":\"v\",\"rate\":1.3}");
        TtsParams merged = base.merge(null);
        assertEquals(1.3, merged.rate());
        assertEquals("v", merged.voice());
    }
}
