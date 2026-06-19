package cn.projectan.strix.core.module.ai.stt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SttParamsTest {

    @Test
    void fromJson_null_returnsEmpty() {
        SttParams p = SttParams.fromJson(null);
        assertNotNull(p);
        assertNull(p.diarizationEnabled());
        assertNull(p.language());
        assertNull(p.vocabularyId());
    }

    @Test
    void fromJson_invalid_returnsEmpty() {
        SttParams p = SttParams.fromJson("not-json");
        assertNotNull(p);
        assertNull(p.speakerCount());
    }

    @Test
    void fromJson_parsesFields() {
        String json = "{\"languageHints\":[\"zh\",\"en\"],\"language\":\"zh\",\"enableItn\":true,"
                + "\"enableWords\":true,\"diarizationEnabled\":true,\"speakerCount\":2,"
                + "\"disfluencyRemovalEnabled\":false,\"timestampAlignmentEnabled\":true,"
                + "\"vocabularyId\":\"v1\",\"channelId\":[0,1]}";
        SttParams p = SttParams.fromJson(json);
        assertEquals(List.of("zh", "en"), p.languageHints());
        assertEquals("zh", p.language());
        assertEquals(true, p.enableItn());
        assertEquals(true, p.enableWords());
        assertEquals(true, p.diarizationEnabled());
        assertEquals(2, p.speakerCount());
        assertEquals(false, p.disfluencyRemovalEnabled());
        assertEquals(true, p.timestampAlignmentEnabled());
        assertEquals("v1", p.vocabularyId());
        assertEquals(List.of(0, 1), p.channelId());
    }

    @Test
    void merge_overrideTakesPrecedence_nullKeepsBase() {
        SttParams base = SttParams.fromJson("{\"diarizationEnabled\":true,\"vocabularyId\":\"base\"}");
        SttParams override = SttParams.fromJson("{\"vocabularyId\":\"req\"}");
        SttParams merged = base.merge(override);
        assertEquals("req", merged.vocabularyId());
        assertEquals(true, merged.diarizationEnabled());
    }

    @Test
    void merge_nullOverride_returnsBase() {
        SttParams base = SttParams.fromJson("{\"speakerCount\":3}");
        assertEquals(3, base.merge(null).speakerCount());
    }
}
