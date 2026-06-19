package cn.projectan.strix.core.module.ai.asr;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AsrSessionParamsTest {

    @Test
    void fromJson_null_returnsEmptyInstance() {
        AsrSessionParams p = AsrSessionParams.fromJson(null);
        assertNotNull(p);
        assertNull(p.semanticPunctuationEnabled());
        assertNull(p.maxSentenceSilence());
        assertNull(p.vocabularyId());
    }

    @Test
    void fromJson_invalid_returnsEmptyInstance() {
        AsrSessionParams p = AsrSessionParams.fromJson("not-json");
        assertNotNull(p);
        assertNull(p.maxSentenceSilence());
    }

    @Test
    void fromJson_parsesFields() {
        String json = "{\"semanticPunctuationEnabled\":false,\"maxSentenceSilence\":800,"
                + "\"speechNoiseThreshold\":0.3,\"languageHints\":[\"zh\",\"en\"],\"vocabularyId\":\"vocab-1\"}";
        AsrSessionParams p = AsrSessionParams.fromJson(json);
        assertEquals(false, p.semanticPunctuationEnabled());
        assertEquals(800, p.maxSentenceSilence());
        assertEquals(0.3, p.speechNoiseThreshold());
        assertEquals(List.of("zh", "en"), p.languageHints());
        assertEquals("vocab-1", p.vocabularyId());
    }

    @Test
    void merge_overrideTakesPrecedence_butNullKeepsBase() {
        AsrSessionParams base = AsrSessionParams.fromJson(
                "{\"maxSentenceSilence\":1300,\"vocabularyId\":\"base-vocab\"}");
        AsrSessionParams override = AsrSessionParams.fromJson(
                "{\"maxSentenceSilence\":500}");
        AsrSessionParams merged = base.merge(override);
        // override 非空字段覆盖
        assertEquals(500, merged.maxSentenceSilence());
        // override 为 null 的字段保留 base
        assertEquals("base-vocab", merged.vocabularyId());
    }

    @Test
    void merge_nullOverride_returnsBase() {
        AsrSessionParams base = AsrSessionParams.fromJson("{\"maxSentenceSilence\":1300}");
        AsrSessionParams merged = base.merge(null);
        assertEquals(1300, merged.maxSentenceSilence());
    }
}
