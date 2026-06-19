package cn.projectan.strix.core.module.ai.stt;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.List;

/**
 * 离线 STT 有效参数（各模型参数并集，字段均可空）。
 * <p>分层来源：请求级覆盖（前端本次提交）&gt; 模型配置默认（stt_params 列）&gt; 系统硬编码默认。
 * 各字段在不同模型下的适用性由对应 Provider 的 buildParameters 决定。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
public record SttParams(
        List<String> languageHints,
        String language,
        Boolean enableItn,
        Boolean enableWords,
        Boolean diarizationEnabled,
        Integer speakerCount,
        Boolean disfluencyRemovalEnabled,
        Boolean timestampAlignmentEnabled,
        String vocabularyId,
        List<Integer> channelId
) {

    public static SttParams empty() {
        return new SttParams(null, null, null, null, null, null, null, null, null, null);
    }

    public static SttParams fromJson(String json) {
        if (json == null || json.isBlank()) {
            return empty();
        }
        try {
            JSONObject o = JSONUtil.parseObj(json);
            List<String> langs = null;
            if (o.get("languageHints") != null) {
                langs = o.getJSONArray("languageHints").toList(String.class);
            }
            List<Integer> channels = null;
            if (o.get("channelId") != null) {
                channels = o.getJSONArray("channelId").toList(Integer.class);
            }
            return new SttParams(
                    langs,
                    o.getStr("language", null),
                    o.getBool("enableItn", null),
                    o.getBool("enableWords", null),
                    o.getBool("diarizationEnabled", null),
                    o.getInt("speakerCount", null),
                    o.getBool("disfluencyRemovalEnabled", null),
                    o.getBool("timestampAlignmentEnabled", null),
                    o.getStr("vocabularyId", null),
                    channels
            );
        } catch (Exception e) {
            return empty();
        }
    }

    public SttParams merge(SttParams o) {
        if (o == null) {
            return this;
        }
        return new SttParams(
                o.languageHints != null ? o.languageHints : this.languageHints,
                o.language != null ? o.language : this.language,
                o.enableItn != null ? o.enableItn : this.enableItn,
                o.enableWords != null ? o.enableWords : this.enableWords,
                o.diarizationEnabled != null ? o.diarizationEnabled : this.diarizationEnabled,
                o.speakerCount != null ? o.speakerCount : this.speakerCount,
                o.disfluencyRemovalEnabled != null ? o.disfluencyRemovalEnabled : this.disfluencyRemovalEnabled,
                o.timestampAlignmentEnabled != null ? o.timestampAlignmentEnabled : this.timestampAlignmentEnabled,
                o.vocabularyId != null ? o.vocabularyId : this.vocabularyId,
                o.channelId != null ? o.channelId : this.channelId
        );
    }
}
