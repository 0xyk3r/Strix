package cn.projectan.strix.core.module.ai.stt.dashscope;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.core.module.ai.stt.*;
import cn.projectan.strix.model.db.system.AiModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * DashScope 异步录音文件识别（{@code services/audio/asr/transcription}）Provider 基类。
 * <p>
 * 承载 Fun-ASR / Paraformer / Qwen-Filetrans 共用的「提交任务 → 轮询 → 下载 transcription JSON → 解析」流程，
 * 子类只定制请求体（{@link #buildInput} / {@link #buildParameters}）、结果路径（{@link #extractResult}）、
 * 情感与说话人开关（{@link #supportsEmotion} / {@link #supportsSpeaker}）。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
@Slf4j
public abstract class AbstractDashScopeAsyncSttProvider implements OfflineSttProvider {

    private static final String ENDPOINT = "services/audio/asr/transcription";

    protected final DashScopeHttpClient dashScopeHttpClient;

    protected AbstractDashScopeAsyncSttProvider(DashScopeHttpClient dashScopeHttpClient) {
        this.dashScopeHttpClient = dashScopeHttpClient;
    }

    /**
     * 拼装 input 对象（file_urls 数组 vs file_url 单对象）。
     */
    protected abstract JSONObject buildInput(String audioUrl);

    /**
     * 拼装 parameters（各模型支持的字段集，省略 null）。
     */
    protected abstract JSONObject buildParameters(AiModelConfig config, SttParams params);

    /**
     * 从轮询完成的 output 中取出子结果对象（results[0] vs result）。
     */
    protected abstract JSONObject extractResult(JSONObject output);

    /**
     * 该模型是否支持情感（仅 Qwen 系列）。默认 false。
     */
    protected boolean supportsEmotion() {
        return false;
    }

    /**
     * 该模型是否支持说话人分离（Fun-ASR / Paraformer）。默认 false。
     */
    protected boolean supportsSpeaker() {
        return false;
    }

    @Override
    public SttResult transcribe(AiModelConfig config, String audioUrl, SttParams params) {
        String reqBody = JSONUtil.createObj()
                .set("model", config.getModelName())
                .set("input", buildInput(audioUrl))
                .set("parameters", buildParameters(config, params == null ? SttParams.empty() : params))
                .toJSONString(0);

        String taskId = dashScopeHttpClient.submitAsyncTask(
                config.getApiKey(), config.getBaseUrl(), ENDPOINT, reqBody);
        log.info("DashScope STT 任务已提交: taskId={}, model={}", taskId, config.getModelName());

        JSONObject output = dashScopeHttpClient.pollTaskUntilDone(
                config.getApiKey(), config.getBaseUrl(), taskId);

        JSONObject result = extractResult(output);
        if (result == null) {
            throw new RuntimeException("DashScope STT 未返回结果 (taskId=" + taskId + ")");
        }
        // 子任务级失败检测（任务可能整体 SUCCEEDED 但子任务 FAILED）
        if ("FAILED".equalsIgnoreCase(result.getStr("subtask_status", null))
                || result.getStr("code", null) != null) {
            throw new RuntimeException("DashScope STT 子任务失败: "
                    + result.getStr("message", result.getStr("code", "未知错误")));
        }
        String url = result.getStr("transcription_url", null);
        if (!StringUtils.hasText(url)) {
            throw new RuntimeException("DashScope STT 未返回 transcription_url (taskId=" + taskId + ")");
        }

        byte[] bytes = dashScopeHttpClient.downloadBytes(url);
        JSONObject json = JSONUtil.parseObj(new String(bytes, StandardCharsets.UTF_8));
        return parseTranscription(json, supportsEmotion(), supportsSpeaker());
    }

    /**
     * 解析 transcription JSON（{@code transcripts[].sentences[]}）为 {@link SttResult}。静态以便单测。
     *
     * @param json            下载的 transcription JSON
     * @param supportsEmotion 是否提取句级情感（Qwen）
     * @param supportsSpeaker 是否提取说话人 ID（Fun-ASR/Paraformer）
     */
    static SttResult parseTranscription(JSONObject json, boolean supportsEmotion, boolean supportsSpeaker) {
        Long durationMs = null;
        JSONObject props = json.getJSONObject("properties");
        if (props != null) {
            durationMs = props.getLong("original_duration_in_milliseconds", null);
        }

        List<SttSentence> sentences = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        String topLanguage = null;

        JSONArray transcripts = json.getJSONArray("transcripts");
        if (transcripts != null) {
            for (Object to : transcripts) {
                JSONObject transcript = (JSONObject) to;
                JSONArray sents = transcript.getJSONArray("sentences");
                if (sents == null) {
                    continue;
                }
                for (Object so : sents) {
                    JSONObject s = (JSONObject) so;
                    String text = s.getStr("text", "");
                    Long begin = s.getLong("begin_time", null);
                    Long end = s.getLong("end_time", null);
                    Integer speakerId = supportsSpeaker ? s.getInt("speaker_id", null) : null;
                    String emotion = supportsEmotion ? s.getStr("emotion", null) : null;
                    String lang = s.getStr("language", null);
                    if (topLanguage == null && lang != null) {
                        topLanguage = lang;
                    }

                    List<SttWord> words = null;
                    JSONArray wordsArr = s.getJSONArray("words");
                    if (wordsArr != null && !wordsArr.isEmpty()) {
                        words = new ArrayList<>(wordsArr.size());
                        for (Object wo : wordsArr) {
                            JSONObject w = (JSONObject) wo;
                            words.add(new SttWord(
                                    w.getLong("begin_time", null),
                                    w.getLong("end_time", null),
                                    w.getStr("text", ""),
                                    w.getStr("punctuation", "")));
                        }
                    }

                    sentences.add(new SttSentence(text, begin, end, speakerId, emotion, lang, words));
                    fullText.append(text);
                }
            }
        }
        return new SttResult(fullText.toString(), durationMs, topLanguage, sentences);
    }
}
