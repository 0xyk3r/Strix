package cn.projectan.strix.core.module.ai.stt.dashscope;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.core.module.ai.stt.SttParams;
import cn.projectan.strix.model.db.system.AiModelConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 阿里云百炼 paraformer-v2 / paraformer-8k-v2 等离线录音文件识别 Provider。
 * <p>异步 run-task 协议。支持说话人分离、顺滑、时间戳校准、语种、热词；不支持情感。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
@Component
public class DashScopeParaformerSttProvider extends AbstractDashScopeAsyncSttProvider {

    public DashScopeParaformerSttProvider(DashScopeHttpClient dashScopeHttpClient) {
        super(dashScopeHttpClient);
    }

    @Override
    public boolean supports(AiModelConfig config) {
        String model = config.getModelName() == null ? "" : config.getModelName().toLowerCase();
        return model.contains("paraformer") && !model.contains("realtime");
    }

    @Override
    protected boolean supportsSpeaker() {
        return true;
    }

    @Override
    protected JSONObject buildInput(String audioUrl) {
        return JSONUtil.createObj().set("file_urls", List.of(audioUrl));
    }

    @Override
    protected JSONObject buildParameters(AiModelConfig config, SttParams p) {
        JSONObject params = JSONUtil.createObj();
        List<String> langs = p.languageHints();
        if ((langs == null || langs.isEmpty()) && StringUtils.hasText(config.getLanguage())) {
            langs = List.of(config.getLanguage());
        }
        if (langs != null && !langs.isEmpty()) {
            params.set("language_hints", new JSONArray(langs));
        }
        if (p.vocabularyId() != null && !p.vocabularyId().isBlank()) {
            params.set("vocabulary_id", p.vocabularyId());
        }
        if (p.diarizationEnabled() != null) {
            params.set("diarization_enabled", p.diarizationEnabled());
        }
        if (p.speakerCount() != null) {
            params.set("speaker_count", p.speakerCount());
        }
        if (p.disfluencyRemovalEnabled() != null) {
            params.set("disfluency_removal_enabled", p.disfluencyRemovalEnabled());
        }
        if (p.timestampAlignmentEnabled() != null) {
            params.set("timestamp_alignment_enabled", p.timestampAlignmentEnabled());
        }
        return params;
    }

    @Override
    protected JSONObject extractResult(JSONObject output) {
        JSONArray results = output.getJSONArray("results");
        return (results == null || results.isEmpty()) ? null : results.getJSONObject(0);
    }
}
