package cn.projectan.strix.core.module.ai.asr;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.List;

/**
 * 实时 ASR run-task 有效参数（各模型参数并集，字段均可空）。
 * <p>分层来源：会话级覆盖（前端 config 消息）&gt; 模型配置默认（asr_params 列）&gt; 系统硬编码默认。
 * 本类只负责承载与合并；各字段在不同模型下的适用性由对应 Provider 的 buildParameters 决定。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
public record AsrSessionParams(
        Boolean semanticPunctuationEnabled,
        Integer maxSentenceSilence,
        Boolean multiThresholdModeEnabled,
        Boolean disfluencyRemovalEnabled,
        Boolean punctuationPredictionEnabled,
        Boolean inverseTextNormalizationEnabled,
        Double speechNoiseThreshold,
        List<String> languageHints,
        String vocabularyId
) {

    /**
     * 全 null 空实例
     */
    public static AsrSessionParams empty() {
        return new AsrSessionParams(null, null, null, null, null, null, null, null, null);
    }

    /**
     * 解析模型配置 asr_params JSON；null/空/非法返回空实例
     */
    public static AsrSessionParams fromJson(String json) {
        if (json == null || json.isBlank()) return empty();
        try {
            JSONObject o = JSONUtil.parseObj(json);
            List<String> langs = null;
            if (o.containsKey("languageHints") && o.get("languageHints") != null) {
                langs = o.getJSONArray("languageHints").toList(String.class);
            }
            return new AsrSessionParams(
                    o.getBool("semanticPunctuationEnabled", null),
                    o.getInt("maxSentenceSilence", null),
                    o.getBool("multiThresholdModeEnabled", null),
                    o.getBool("disfluencyRemovalEnabled", null),
                    o.getBool("punctuationPredictionEnabled", null),
                    o.getBool("inverseTextNormalizationEnabled", null),
                    o.getDouble("speechNoiseThreshold", null),
                    langs,
                    o.getStr("vocabularyId", null)
            );
        } catch (Exception e) {
            return empty();
        }
    }

    /**
     * 以 override 的非空字段覆盖 this，返回新实例；override 为 null 时返回 this
     */
    public AsrSessionParams merge(AsrSessionParams o) {
        if (o == null) return this;
        return new AsrSessionParams(
                o.semanticPunctuationEnabled != null ? o.semanticPunctuationEnabled : this.semanticPunctuationEnabled,
                o.maxSentenceSilence != null ? o.maxSentenceSilence : this.maxSentenceSilence,
                o.multiThresholdModeEnabled != null ? o.multiThresholdModeEnabled : this.multiThresholdModeEnabled,
                o.disfluencyRemovalEnabled != null ? o.disfluencyRemovalEnabled : this.disfluencyRemovalEnabled,
                o.punctuationPredictionEnabled != null ? o.punctuationPredictionEnabled : this.punctuationPredictionEnabled,
                o.inverseTextNormalizationEnabled != null ? o.inverseTextNormalizationEnabled : this.inverseTextNormalizationEnabled,
                o.speechNoiseThreshold != null ? o.speechNoiseThreshold : this.speechNoiseThreshold,
                o.languageHints != null ? o.languageHints : this.languageHints,
                o.vocabularyId != null ? o.vocabularyId : this.vocabularyId
        );
    }
}
