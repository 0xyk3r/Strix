package cn.projectan.strix.core.module.ai.livetranslate;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.Map;

/**
 * 实时语音翻译会话参数（各模型参数并集，字段均可空）。
 * <p>分层来源：会话级覆盖（前端 config 消息）&gt; 模型配置默认（live_translate_params 列）&gt; 系统硬编码默认。
 *
 * @param sourceLanguage            源语种代码（zh/en/...），null = 自动检测
 * @param targetLanguage            目标语种代码（zh/en/...），默认 en
 * @param voice                     翻译输出音色，默认 Tina
 * @param modalities                输出模态：["text"] 或 ["text","audio"]，默认 ["text","audio"]
 * @param sampleRate                输入音频采样率（8000/16000），默认 16000
 * @param enableSourceTranscription 是否同时返回源语言转写结果，默认 true
 * @param enableVoiceClone          是否启用声音复刻，默认 false
 * @param voiceCloneFrequency       声音复刻频率（once/always/never），null 时不启用
 * @param hotwords                  热词映射（源语言词 → 目标语言词），null = 不配置
 * @author ProjectAn
 * @since 2026-06-30
 */
public record LiveTranslateSessionParams(
        String sourceLanguage,
        String targetLanguage,
        String voice,
        List<String> modalities,
        Integer sampleRate,
        Boolean enableSourceTranscription,
        Boolean enableVoiceClone,
        String voiceCloneFrequency,
        Map<String, String> hotwords
) {

    /**
     * 全 null 空实例
     */
    public static LiveTranslateSessionParams empty() {
        return new LiveTranslateSessionParams(null, null, null, null, null, null, null, null, null);
    }

    /**
     * 解析模型配置 live_translate_params JSON；null/空/非法返回空实例
     */
    public static LiveTranslateSessionParams fromJson(String json) {
        if (json == null || json.isBlank()) return empty();
        try {
            JSONObject o = JSONUtil.parseObj(json);
            List<String> mods = null;
            if (o.containsKey("modalities") && o.get("modalities") != null) {
                mods = o.getJSONArray("modalities").toList(String.class);
            }
            Map<String, String> hw = null;
            if (o.containsKey("hotwords") && o.get("hotwords") instanceof JSONObject jo) {
                hw = new java.util.HashMap<>();
                for (String k : jo.keySet()) {
                    hw.put(k, jo.getStr(k));
                }
            }
            return new LiveTranslateSessionParams(
                    o.getStr("sourceLanguage", null),
                    o.getStr("targetLanguage", null),
                    o.getStr("voice", null),
                    mods,
                    o.getInt("sampleRate", null),
                    o.getBool("enableSourceTranscription", null),
                    o.getBool("enableVoiceClone", null),
                    o.getStr("voiceCloneFrequency", null),
                    hw
            );
        } catch (Exception e) {
            return empty();
        }
    }

    /**
     * 以 override 的非空字段覆盖 this，返回新实例；override 为 null 时返回 this
     */
    public LiveTranslateSessionParams merge(LiveTranslateSessionParams o) {
        if (o == null) return this;
        return new LiveTranslateSessionParams(
                o.sourceLanguage != null ? o.sourceLanguage : this.sourceLanguage,
                o.targetLanguage != null ? o.targetLanguage : this.targetLanguage,
                o.voice != null ? o.voice : this.voice,
                o.modalities != null ? o.modalities : this.modalities,
                o.sampleRate != null ? o.sampleRate : this.sampleRate,
                o.enableSourceTranscription != null ? o.enableSourceTranscription : this.enableSourceTranscription,
                o.enableVoiceClone != null ? o.enableVoiceClone : this.enableVoiceClone,
                o.voiceCloneFrequency != null ? o.voiceCloneFrequency : this.voiceCloneFrequency,
                o.hotwords != null ? o.hotwords : this.hotwords
        );
    }

    /**
     * 目标语种（带默认值）
     */
    public String effectiveTargetLanguage() {
        return targetLanguage != null ? targetLanguage : "en";
    }

    /**
     * 音色（带默认值）
     */
    public String effectiveVoice() {
        return voice != null ? voice : "Tina";
    }

    /**
     * 采样率（带默认值）
     */
    public int effectiveSampleRate() {
        return sampleRate != null ? sampleRate : 16000;
    }

    /**
     * 输出模态（带默认值）
     */
    public List<String> effectiveModalities() {
        return modalities != null ? modalities : List.of("text", "audio");
    }

    /**
     * 是否同时返回源语言转写（带默认值）
     */
    public boolean effectiveEnableSourceTranscription() {
        return enableSourceTranscription == null || enableSourceTranscription;
    }
}
