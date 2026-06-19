package cn.projectan.strix.core.module.ai.stt.dashscope;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.core.module.ai.stt.OfflineSttProvider;
import cn.projectan.strix.core.module.ai.stt.SttParams;
import cn.projectan.strix.core.module.ai.stt.SttResult;
import cn.projectan.strix.core.module.ai.stt.SttSentence;
import cn.projectan.strix.model.db.system.AiModelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 阿里云百炼 qwen3-asr-flash 同步录音文件识别 Provider（≤5min/10MB）。
 * <p>不走异步轮询，直接同步调用 multimodal-generation 接口。支持情感（7 类）与语种；
 * 不返回时间戳/说话人/字级。结果归并为单条 {@link SttSentence}。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
@Component
@RequiredArgsConstructor
public class DashScopeQwenFlashSttProvider implements OfflineSttProvider {

    private final DashScopeHttpClient dashScopeHttpClient;

    @Override
    public boolean supports(AiModelConfig config) {
        String model = config.getModelName() == null ? "" : config.getModelName().toLowerCase();
        return model.contains("qwen") && model.contains("asr") && model.contains("flash")
                && !model.contains("filetrans") && !model.contains("realtime");
    }

    @Override
    public SttResult transcribe(AiModelConfig config, String audioUrl, SttParams params) {
        SttParams p = params == null ? SttParams.empty() : params;

        JSONObject asrOptions = JSONUtil.createObj();
        String lang = p.language() != null ? p.language() : config.getLanguage();
        if (StringUtils.hasText(lang)) {
            asrOptions.set("language", lang);
        }
        asrOptions.set("enable_itn", p.enableItn() != null ? p.enableItn() : false);

        JSONArray content = JSONUtil.createArray();
        content.add(JSONUtil.createObj().set("audio", audioUrl));
        JSONArray messages = JSONUtil.createArray();
        messages.add(JSONUtil.createObj().set("role", "user").set("content", content));

        String reqBody = JSONUtil.createObj()
                .set("model", config.getModelName())
                .set("input", JSONUtil.createObj().set("messages", messages))
                .set("parameters", JSONUtil.createObj().set("asr_options", asrOptions))
                .toJSONString(0);

        JSONObject output = dashScopeHttpClient.multimodalGenerationSync(
                config.getApiKey(), config.getBaseUrl(), reqBody);
        return parseSyncResponse(output);
    }

    /**
     * 解析同步响应 output：choices[0].message.content[0].text + annotations[0].emotion/language。静态以便单测。
     */
    static SttResult parseSyncResponse(JSONObject output) {
        JSONArray choices = output.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return new SttResult("", null, null, List.of());
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String text = "";
        JSONArray content = message != null ? message.getJSONArray("content") : null;
        if (content != null && !content.isEmpty()) {
            text = content.getJSONObject(0).getStr("text", "");
        }
        String emotion = null;
        String language = null;
        JSONArray annotations = message != null ? message.getJSONArray("annotations") : null;
        if (annotations != null && !annotations.isEmpty()) {
            JSONObject a = annotations.getJSONObject(0);
            emotion = a.getStr("emotion", null);
            language = a.getStr("language", null);
        }
        SttSentence sentence = new SttSentence(text, null, null, null, emotion, language, null);
        return new SttResult(text, null, language, List.of(sentence));
    }
}
