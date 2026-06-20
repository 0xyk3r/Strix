package cn.projectan.strix.core.module.ai.tts;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.List;

/**
 * TTS 语音合成有效参数（各模型参数并集，字段均可空）。
 * <p>分层来源：会话/请求级覆盖（前端）&gt; 模型配置默认（tts_params 列）&gt; 系统硬编码默认。
 * 本类只负责承载与合并；各字段在不同模型下的适用性由对应 Provider 的 buildParameters 决定。
 *
 * @param voice         音色 ID（声音复刻/设计返回的 voice_id；cosyvoice-v3.5 无系统音色）
 * @param format        音频格式：mp3 / wav / pcm / opus
 * @param sampleRate    采样率（Hz）：8000/16000/22050/24000/44100/48000
 * @param volume        音量 [0,100]
 * @param rate          语速 [0.5,2.0]
 * @param pitch         音调 [0.5,2.0]
 * @param bitRate       opus 码率（kbps）[6,510]
 * @param instruction   指令控制文本（cosyvoice-v3.5 复刻/设计音色支持任意指令，≤100 字符）
 * @param enableSsml    是否启用 SSML 解析
 * @param seed          随机种子 [0,65535]
 * @param languageHints 目标语言提示
 * @author ProjectAn
 * @since 2026-06-20
 */
public record TtsParams(
        String voice,
        String format,
        Integer sampleRate,
        Integer volume,
        Double rate,
        Double pitch,
        Integer bitRate,
        String instruction,
        Boolean enableSsml,
        Integer seed,
        List<String> languageHints
) {

    /**
     * 全 null 空实例
     */
    public static TtsParams empty() {
        return new TtsParams(null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 解析模型配置 tts_params JSON 或前端 JSON；null/空/非法返回空实例
     */
    public static TtsParams fromJson(String json) {
        if (json == null || json.isBlank()) {
            return empty();
        }
        try {
            JSONObject o = JSONUtil.parseObj(json);
            List<String> langs = null;
            if (o.containsKey("languageHints") && o.get("languageHints") != null) {
                langs = o.getJSONArray("languageHints").toList(String.class);
            }
            return new TtsParams(
                    o.getStr("voice", null),
                    o.getStr("format", null),
                    o.getInt("sampleRate", null),
                    o.getInt("volume", null),
                    o.getDouble("rate", null),
                    o.getDouble("pitch", null),
                    o.getInt("bitRate", null),
                    o.getStr("instruction", null),
                    o.getBool("enableSsml", null),
                    o.getInt("seed", null),
                    langs
            );
        } catch (Exception e) {
            return empty();
        }
    }

    /**
     * 以 override 的非空字段覆盖 this，返回新实例；override 为 null 时返回 this
     */
    public TtsParams merge(TtsParams o) {
        if (o == null) {
            return this;
        }
        return new TtsParams(
                o.voice != null ? o.voice : this.voice,
                o.format != null ? o.format : this.format,
                o.sampleRate != null ? o.sampleRate : this.sampleRate,
                o.volume != null ? o.volume : this.volume,
                o.rate != null ? o.rate : this.rate,
                o.pitch != null ? o.pitch : this.pitch,
                o.bitRate != null ? o.bitRate : this.bitRate,
                o.instruction != null ? o.instruction : this.instruction,
                o.enableSsml != null ? o.enableSsml : this.enableSsml,
                o.seed != null ? o.seed : this.seed,
                o.languageHints != null ? o.languageHints : this.languageHints
        );
    }
}
