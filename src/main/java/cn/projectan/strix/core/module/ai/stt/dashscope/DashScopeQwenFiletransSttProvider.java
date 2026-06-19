package cn.projectan.strix.core.module.ai.stt.dashscope;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.core.module.ai.stt.SttParams;
import cn.projectan.strix.model.db.system.AiModelConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 阿里云百炼 qwen3-asr-flash-filetrans 离线录音文件识别 Provider。
 * <p>异步 run-task 协议，但 input 为 {@code file_url} 单对象、结果为 {@code output.result} 单对象。
 * 支持情感（7 类）、单语种、ITN、字级时间戳；不支持说话人分离。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
@Component
public class DashScopeQwenFiletransSttProvider extends AbstractDashScopeAsyncSttProvider {

    public DashScopeQwenFiletransSttProvider(DashScopeHttpClient dashScopeHttpClient) {
        super(dashScopeHttpClient);
    }

    @Override
    public boolean supports(AiModelConfig config) {
        String model = config.getModelName() == null ? "" : config.getModelName().toLowerCase();
        return model.contains("qwen") && model.contains("asr") && model.contains("filetrans")
                && !model.contains("realtime");
    }

    @Override
    protected boolean supportsEmotion() {
        return true;
    }

    @Override
    protected JSONObject buildInput(String audioUrl) {
        return JSONUtil.createObj().set("file_url", audioUrl);
    }

    @Override
    protected JSONObject buildParameters(AiModelConfig config, SttParams p) {
        JSONObject params = JSONUtil.createObj();
        String lang = p.language();
        if (lang == null && StringUtils.hasText(config.getLanguage())) {
            lang = config.getLanguage();
        }
        if (StringUtils.hasText(lang)) {
            params.set("language", lang);
        }
        if (p.enableItn() != null) {
            params.set("enable_itn", p.enableItn());
        }
        if (p.enableWords() != null) {
            params.set("enable_words", p.enableWords());
        }
        if (p.channelId() != null && !p.channelId().isEmpty()) {
            params.set("channel_id", new JSONArray(p.channelId()));
        }
        return params;
    }

    @Override
    protected JSONObject extractResult(JSONObject output) {
        return output.getJSONObject("result");
    }
}
