package cn.projectan.strix.core.module.ai.asr.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.asr.AsrTranscript;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RunTaskSentenceParserTest {

    @Test
    void parseSentence_withWordsAndTimestamps() {
        String json = "{\"begin_time\":170,\"end_time\":920,\"text\":\"好，我知道了\",\"sentence_end\":true,"
                + "\"words\":[{\"begin_time\":170,\"end_time\":295,\"text\":\"好\",\"punctuation\":\"，\"},"
                + "{\"begin_time\":295,\"end_time\":503,\"text\":\"我\",\"punctuation\":\"\"}]}";
        JSONObject s = JSONUtil.parseObj(json);
        AsrTranscript t = AbstractDashScopeRunTaskAsrProvider.parseSentence(s, "task-0", false);
        assertEquals("task-0", t.itemId());
        assertEquals("好，我知道了", t.text());
        assertTrue(t.isFinal());
        assertEquals(170L, t.beginTime());
        assertEquals(920L, t.endTime());
        assertNotNull(t.words());
        assertEquals(2, t.words().size());
        assertEquals("好", t.words().get(0).text());
        assertEquals("，", t.words().get(0).punctuation());
        assertEquals(295L, t.words().get(0).endTime());
        // 不支持情感 → 无情感字段
        assertNull(t.emotion());
        assertNull(t.emotionScheme());
    }

    @Test
    void parseSentence_intermediate_nullEndTime() {
        String json = "{\"begin_time\":170,\"end_time\":null,\"text\":\"好\",\"sentence_end\":false}";
        JSONObject s = JSONUtil.parseObj(json);
        AsrTranscript t = AbstractDashScopeRunTaskAsrProvider.parseSentence(s, "task-1", false);
        assertFalse(t.isFinal());
        assertEquals(170L, t.beginTime());
        assertNull(t.endTime());
    }

    @Test
    void parseSentence_emotion_onlyWhenSupportedAndFinal() {
        String json = "{\"begin_time\":0,\"end_time\":500,\"text\":\"太好了\",\"sentence_end\":true,"
                + "\"emo_tag\":\"positive\",\"emo_confidence\":0.92}";
        JSONObject s = JSONUtil.parseObj(json);
        AsrTranscript t = AbstractDashScopeRunTaskAsrProvider.parseSentence(s, "task-2", true);
        assertEquals("positive", t.emotion());
        assertEquals("polarity3", t.emotionScheme());
        assertEquals(0.92, t.emotionConfidence());
    }

    @Test
    void parseSentence_emotionIgnored_whenNotFinal() {
        // 情感仅在句末（sentence_end=true）返回；中间结果即便带 emo_tag 也忽略
        String json = "{\"text\":\"太好\",\"sentence_end\":false,\"emo_tag\":\"positive\",\"emo_confidence\":0.5}";
        JSONObject s = JSONUtil.parseObj(json);
        AsrTranscript t = AbstractDashScopeRunTaskAsrProvider.parseSentence(s, "task-3", true);
        assertNull(t.emotion());
        assertNull(t.emotionScheme());
        assertNull(t.emotionConfidence());
    }

    @Test
    void parseSentence_emotionIgnored_whenNotSupported() {
        // Fun-ASR（supportsEmotion=false）即便上游异常带了 emo_tag 也不解析
        String json = "{\"text\":\"x\",\"sentence_end\":true,\"emo_tag\":\"positive\",\"emo_confidence\":0.5}";
        JSONObject s = JSONUtil.parseObj(json);
        AsrTranscript t = AbstractDashScopeRunTaskAsrProvider.parseSentence(s, "task-4", false);
        assertNull(t.emotion());
    }
}
