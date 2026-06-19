package cn.projectan.strix.core.module.ai.asr.dashscope;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.asr.AsrSessionParams;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.model.db.system.AiModelConfig;
import org.springframework.stereotype.Component;

/**
 * 阿里云百炼 fun-asr-realtime 系列实时识别 Provider。
 * <p>采用 DashScope run-task 协议（见基类）。支持语义断句、VAD 断句阈值、多阈值模式、
 * Fun-ASR 专属噪音判定阈值、语种、热词。不支持顺滑/标点/ITN，亦不支持情感识别。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
@Component
public class DashScopeFunAsrProvider extends AbstractDashScopeRunTaskAsrProvider {

    public DashScopeFunAsrProvider(DashScopeHttpClient dashScopeHttpClient) {
        super(dashScopeHttpClient);
    }

    @Override
    public boolean supports(AiModelConfig config) {
        String model = config.getModelName() == null ? "" : config.getModelName().toLowerCase();
        return model.contains("fun-asr") || model.contains("funasr");
    }

    @Override
    protected JSONObject buildParameters(AiModelConfig config, AsrSessionParams p) {
        JSONObject params = JSONUtil.createObj();
        if (p.semanticPunctuationEnabled() != null) {
            params.set("semantic_punctuation_enabled", p.semanticPunctuationEnabled());
        }
        if (p.maxSentenceSilence() != null) {
            params.set("max_sentence_silence", p.maxSentenceSilence());
        }
        if (p.multiThresholdModeEnabled() != null) {
            params.set("multi_threshold_mode_enabled", p.multiThresholdModeEnabled());
        }
        if (p.speechNoiseThreshold() != null) {
            params.set("speech_noise_threshold", p.speechNoiseThreshold());
        }
        if (p.languageHints() != null && !p.languageHints().isEmpty()) {
            params.set("language_hints", new JSONArray(p.languageHints()));
        }
        if (p.vocabularyId() != null && !p.vocabularyId().isBlank()) {
            params.set("vocabulary_id", p.vocabularyId());
        }
        return params;
    }
}
