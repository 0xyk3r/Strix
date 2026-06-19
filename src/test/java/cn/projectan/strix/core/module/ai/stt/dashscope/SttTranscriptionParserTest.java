package cn.projectan.strix.core.module.ai.stt.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.stt.SttResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SttTranscriptionParserTest {

    @Test
    void parse_funAsrShape_speakerNoEmotion() {
        String json = "{\"properties\":{\"original_duration_in_milliseconds\":6500},"
                + "\"transcripts\":[{\"text\":\"你好世界\",\"sentences\":["
                + "{\"begin_time\":100,\"end_time\":3820,\"text\":\"你好\",\"speaker_id\":0,\"emotion\":\"happy\","
                + "\"words\":[{\"begin_time\":100,\"end_time\":596,\"text\":\"你\",\"punctuation\":\"\"}]},"
                + "{\"begin_time\":3820,\"end_time\":6500,\"text\":\"世界\",\"speaker_id\":1}]}]}";
        JSONObject t = JSONUtil.parseObj(json);
        // 支持说话人、不支持情感（Fun-ASR/Paraformer）
        SttResult r = AbstractDashScopeAsyncSttProvider.parseTranscription(t, false, true);
        assertEquals(6500L, r.durationMs());
        assertEquals("你好世界", r.text());
        assertEquals(2, r.sentences().size());
        assertEquals(0, r.sentences().get(0).speakerId());
        assertEquals(1, r.sentences().get(1).speakerId());
        // 即便上游带 emotion，不支持时也忽略
        assertNull(r.sentences().get(0).emotion());
        assertEquals(100L, r.sentences().get(0).beginTime());
        assertEquals("你", r.sentences().get(0).words().get(0).text());
    }

    @Test
    void parse_qwenShape_emotionLanguageNoSpeaker() {
        String json = "{\"transcripts\":[{\"text\":\"欢迎\",\"sentences\":["
                + "{\"begin_time\":0,\"end_time\":1440,\"text\":\"欢迎\",\"emotion\":\"neutral\",\"language\":\"zh\","
                + "\"speaker_id\":9}]}]}";
        JSONObject t = JSONUtil.parseObj(json);
        // 支持情感、不支持说话人（Qwen）
        SttResult r = AbstractDashScopeAsyncSttProvider.parseTranscription(t, true, false);
        assertEquals("neutral", r.sentences().get(0).emotion());
        assertEquals("zh", r.sentences().get(0).language());
        assertEquals("zh", r.language());
        // 不支持说话人时即便上游带 speaker_id 也为 null
        assertNull(r.sentences().get(0).speakerId());
    }

    @Test
    void parse_empty_returnsEmpty() {
        SttResult r = AbstractDashScopeAsyncSttProvider.parseTranscription(JSONUtil.createObj(), true, true);
        assertEquals("", r.text());
        assertTrue(r.sentences().isEmpty());
        assertNull(r.durationMs());
    }
}
