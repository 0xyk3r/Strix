package cn.projectan.strix.core.module.ai.stt;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SttResultJsonTest {

    @Test
    void toJson_fullSentence_includesAllFields() {
        SttResult r = new SttResult("好，我知道了", 920L, "zh", List.of(
                new SttSentence("好，我知道了", 170L, 920L, 0, "happy", "zh",
                        List.of(new SttWord(170L, 295L, "好", "，")))));
        JSONObject o = JSONUtil.parseObj(SttResultJson.toJson(r));
        assertEquals("好，我知道了", o.getStr("text"));
        assertEquals(920L, o.getLong("durationMs"));
        assertEquals("zh", o.getStr("language"));
        JSONObject s = o.getJSONArray("sentences").getJSONObject(0);
        assertEquals(170L, s.getLong("beginTime"));
        assertEquals(920L, s.getLong("endTime"));
        assertEquals(0, s.getInt("speakerId"));
        assertEquals("happy", s.getStr("emotion"));
        assertEquals("zh", s.getStr("language"));
        JSONObject w = s.getJSONArray("words").getJSONObject(0);
        assertEquals("好", w.getStr("text"));
        assertEquals("，", w.getStr("punctuation"));
        assertEquals(295L, w.getLong("endTime"));
    }

    @Test
    void toJson_omitsNullFields() {
        // Qwen-Flash 形态：无时间戳/说话人/字级
        SttResult r = new SttResult("欢迎使用阿里云。", null, "zh", List.of(
                new SttSentence("欢迎使用阿里云。", null, null, null, "neutral", "zh", null)));
        JSONObject o = JSONUtil.parseObj(SttResultJson.toJson(r));
        assertFalse(o.containsKey("durationMs"));
        JSONObject s = o.getJSONArray("sentences").getJSONObject(0);
        assertFalse(s.containsKey("beginTime"));
        assertFalse(s.containsKey("endTime"));
        assertFalse(s.containsKey("speakerId"));
        assertFalse(s.containsKey("words"));
        assertEquals("neutral", s.getStr("emotion"));
    }

    @Test
    void toJson_emptySentences_emptyArray() {
        SttResult r = new SttResult("", null, null, List.of());
        JSONObject o = JSONUtil.parseObj(SttResultJson.toJson(r));
        assertEquals("", o.getStr("text"));
        assertTrue(o.getJSONArray("sentences").isEmpty());
    }
}
