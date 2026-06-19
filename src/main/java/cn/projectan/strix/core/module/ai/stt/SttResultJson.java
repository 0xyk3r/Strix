package cn.projectan.strix.core.module.ai.stt;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 将 {@link SttResult} 手工序列化为下发 JSON 字符串（空字段省略，保证前端按存在性渲染）。
 * <p>不依赖 record 反射序列化，字段名即前后端契约。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
public final class SttResultJson {

    private SttResultJson() {
    }

    public static String toJson(SttResult r) {
        JSONObject obj = JSONUtil.createObj().set("text", r.text());
        if (r.durationMs() != null) {
            obj.set("durationMs", r.durationMs());
        }
        if (r.language() != null) {
            obj.set("language", r.language());
        }
        JSONArray sentences = JSONUtil.createArray();
        if (r.sentences() != null) {
            for (SttSentence s : r.sentences()) {
                JSONObject so = JSONUtil.createObj().set("text", s.text());
                if (s.beginTime() != null) {
                    so.set("beginTime", s.beginTime());
                }
                if (s.endTime() != null) {
                    so.set("endTime", s.endTime());
                }
                if (s.speakerId() != null) {
                    so.set("speakerId", s.speakerId());
                }
                if (s.emotion() != null) {
                    so.set("emotion", s.emotion());
                }
                if (s.language() != null) {
                    so.set("language", s.language());
                }
                if (s.words() != null && !s.words().isEmpty()) {
                    JSONArray warr = JSONUtil.createArray();
                    for (SttWord w : s.words()) {
                        warr.add(JSONUtil.createObj()
                                .set("beginTime", w.beginTime())
                                .set("endTime", w.endTime())
                                .set("text", w.text())
                                .set("punctuation", w.punctuation()));
                    }
                    so.set("words", warr);
                }
                sentences.add(so);
            }
        }
        obj.set("sentences", sentences);
        return obj.toJSONString(0);
    }
}
