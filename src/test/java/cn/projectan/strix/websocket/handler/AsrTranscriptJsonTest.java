package cn.projectan.strix.websocket.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.asr.AsrTranscript;
import cn.projectan.strix.core.module.ai.asr.AsrWord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AsrTranscriptJsonTest {

    @Test
    void qwen_omitsTimestampAndConfidence() {
        // Qwen：有情绪(qwen7)与语种，无时间戳/字级/置信度 → 这些键省略
        AsrTranscript t = new AsrTranscript("item_1", "今天天气不错", false,
                "neutral", "qwen7", null, "zh", null, null, null);
        JSONObject o = JSONUtil.parseObj(AiAsrWebSocketHandler.buildTranscriptJson(t));
        assertEquals("item_1", o.getStr("itemId"));
        assertEquals("今天天气不错", o.getStr("text"));
        assertFalse(o.getBool("final"));
        assertEquals("neutral", o.getStr("emotion"));
        assertEquals("qwen7", o.getStr("emotionScheme"));
        assertEquals("zh", o.getStr("language"));
        assertFalse(o.containsKey("emotionConfidence"));
        assertFalse(o.containsKey("beginTime"));
        assertFalse(o.containsKey("endTime"));
        assertFalse(o.containsKey("words"));
    }

    @Test
    void paraformer_includesTimestampsWordsAndPolarityEmotion() {
        List<AsrWord> words = List.of(
                new AsrWord(170L, 295L, "好", "，"),
                new AsrWord(295L, 503L, "我", ""));
        AsrTranscript t = new AsrTranscript("task-0", "好，我", true,
                "positive", "polarity3", 0.92, "zh", 170L, 503L, words);
        JSONObject o = JSONUtil.parseObj(AiAsrWebSocketHandler.buildTranscriptJson(t));
        assertTrue(o.getBool("final"));
        assertEquals("polarity3", o.getStr("emotionScheme"));
        assertEquals(0.92, o.getDouble("emotionConfidence"));
        assertEquals(170L, o.getLong("beginTime"));
        assertEquals(503L, o.getLong("endTime"));
        assertEquals(2, o.getJSONArray("words").size());
        JSONObject w0 = (JSONObject) o.getJSONArray("words").get(0);
        assertEquals("好", w0.getStr("text"));
        assertEquals("，", w0.getStr("punctuation"));
        assertEquals(295L, w0.getLong("endTime"));
    }

    @Test
    void noEmotion_omitsEmotionKeys() {
        AsrTranscript t = new AsrTranscript("task-1", "x", false,
                null, null, null, null, 0L, null, null);
        JSONObject o = JSONUtil.parseObj(AiAsrWebSocketHandler.buildTranscriptJson(t));
        assertFalse(o.containsKey("emotion"));
        assertFalse(o.containsKey("emotionScheme"));
        assertFalse(o.containsKey("emotionConfidence"));
        assertEquals(0L, o.getLong("beginTime"));
    }
}
