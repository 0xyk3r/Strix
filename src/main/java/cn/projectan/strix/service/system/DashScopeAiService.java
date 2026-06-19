package cn.projectan.strix.service.system;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.model.db.system.AiModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * DashScope 原生 API 服务（TTS / 批量 ASR / 图片生成）
 * <p>使用 OkHttp 直接调用 DashScope REST API，不依赖 DashScope SDK</p>
 * <p>前置条件：数据库中需存在对应类型的已启用模型配置，且 base_url 设为
 * {@code https://dashscope.aliyuncs.com/api/v1}</p>
 *
 * @author ProjectAn
 * @since 2026-05-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashScopeAiService {

    private final AiModelConfigService aiModelConfigService;
    private final DashScopeHttpClient dashScopeHttpClient;

    @Autowired(required = false)
    private StrixOssStore ossStore;

    // ============================================================
    //  TTS 语音合成
    // ============================================================

    /**
     * TTS 语音合成，返回 DashScope 预签名音频 URL（临时有效）
     *
     * @param configKey 模型配置 key
     * @param text      要合成的文本
     * @return 音频文件 URL
     */
    public String synthesizeSpeechToUrl(String configKey, String text) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        return doSynthesizeSpeechToUrl(config, text);
    }

    /**
     * TTS 语音合成，返回音频字节数组
     *
     * @param configKey 模型配置 key
     * @param text      要合成的文本
     * @return 音频字节数组
     */
    public byte[] synthesizeSpeech(String configKey, String text) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        String audioUrl = doSynthesizeSpeechToUrl(config, text);
        return dashScopeHttpClient.downloadBytes(audioUrl);
    }

    private String doSynthesizeSpeechToUrl(AiModelConfig config, String text) {
        String voice = config.getVoice();
        Assert.hasText(voice, "TTS 配置缺少 voice 字段，请先调用 /tts/enroll/{configKey} 注册音色: " + config.getKey());

        String format = StringUtils.hasText(config.getResponseFormat()) ? config.getResponseFormat() : "wav";
        double speed = config.getSpeed() != null ? config.getSpeed().doubleValue() : 1.0;

        return dashScopeHttpClient.synthesizeSpeechToUrl(
                config.getApiKey(),
                config.getBaseUrl(),
                config.getModelName(),
                text,
                voice,
                format,
                speed,
                22050
        );
    }

    // ============================================================
    //  TTS 音色注册（声音复刻）
    // ============================================================

    /**
     * TTS 音色注册（声音复刻）：提交注册任务、轮询审核结果，完成后将 voice_id 写入数据库
     *
     * <p>前置条件：模型配置的 {@code prompt_audio_url} 字段须设置为参考音频的公网 URL。</p>
     *
     * @param configKey TTS 模型配置 key
     * @return 注册成功的 voice_id
     */
    public String enrollTtsVoice(String configKey) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        String promptAudioUrl = config.getPromptAudioUrl();
        Assert.hasText(promptAudioUrl, "TTS 配置缺少 prompt_audio_url（参考音频 URL）: " + configKey);

        // 前缀：仅保留小写字母和数字，最多 10 个字符
        String prefix = configKey.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (prefix.length() > 10) prefix = prefix.substring(0, 10);
        if (prefix.isBlank()) prefix = "voice";

        log.info("开始 TTS 音色注册: configKey={}, prefix={}", configKey, prefix);

        String voiceId = dashScopeHttpClient.enrollVoice(
                config.getApiKey(), config.getBaseUrl(), config.getModelName(),
                promptAudioUrl, prefix);
        log.info("音色注册已提交: configKey={}, voiceId={}", configKey, voiceId);

        dashScopeHttpClient.pollVoiceUntilReady(config.getApiKey(), config.getBaseUrl(), voiceId);

        aiModelConfigService.updateVoice(config.getId(), voiceId);
        log.info("音色注册完成并已写入 DB: configKey={}, voiceId={}", configKey, voiceId);

        return voiceId;
    }

    // ============================================================
    //  批量 ASR 语音识别
    // ============================================================

    /**
     * 批量转录：直接传入可公网访问的音频 URL
     *
     * @param configKey 模型配置 key
     * @param audioUrl  音频文件的公网 HTTPS URL
     * @return 识别文本
     */
    public String transcribeAudioUrl(String configKey, String audioUrl) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        return doTranscribeAudioUrl(config, audioUrl);
    }

    /**
     * 批量转录：上传音频流到 OSS 后进行转录（转录完成后自动删除临时文件）
     *
     * @param configKey     模型配置 key（需配置 ossConfigKey 和 ossBucketName）
     * @param audioStream   音频输入流
     * @param contentLength 内容长度（字节）
     * @param fileName      原始文件名（用于生成 OSS 对象名）
     * @return 识别文本
     */
    public String transcribeAudio(String configKey, InputStream audioStream,
                                  long contentLength, String fileName) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        String ossConfigKey = config.getOssConfigKey();
        String ossBucketName = config.getOssBucketName();

        Assert.notNull(ossStore, "OSS 模块未启用，无法上传音频文件（请检查 strix.module.oss 配置）");
        Assert.hasText(ossConfigKey, "STT 配置缺少 ossConfigKey: " + configKey);
        Assert.hasText(ossBucketName, "STT 配置缺少 ossBucketName: " + configKey);

        var ossClient = ossStore.getInstance(ossConfigKey);
        Assert.notNull(ossClient, "OSS 实例不存在: " + ossConfigKey);

        String objectName = "ai/asr/temp/" + UUID.randomUUID() + "_" + fileName;
        ossClient.getPublic().upload(ossBucketName, objectName, audioStream, contentLength);
        log.info("音频文件已上传至 OSS: bucket={}, object={}", ossBucketName, objectName);

        // 生成 1 小时有效的签名 URL
        String signedUrl = ossClient.getPublic().signDownloadUrl(ossBucketName, objectName, 3_600_000L);

        try {
            return doTranscribeAudioUrl(config, signedUrl);
        } finally {
            try {
                ossClient.getPublic().delete(ossBucketName, objectName);
                log.info("ASR 临时音频文件已删除: bucket={}, object={}", ossBucketName, objectName);
            } catch (Exception e) {
                log.warn("ASR 临时文件删除失败: bucket={}, object={}", ossBucketName, objectName, e);
            }
        }
    }

    private String doTranscribeAudioUrl(AiModelConfig config, String audioUrl) {
        JSONObject params = JSONUtil.createObj();
        if (StringUtils.hasText(config.getLanguage())) {
            params.set("language_hints", List.of(config.getLanguage()));
        }

        String reqBody = JSONUtil.createObj()
                .set("model", config.getModelName())
                .set("input", JSONUtil.createObj().set("file_urls", List.of(audioUrl)))
                .set("parameters", params)
                .toJSONString(0);

        String taskId = dashScopeHttpClient.submitAsyncTask(
                config.getApiKey(), config.getBaseUrl(),
                "services/audio/asr/transcription", reqBody);
        log.info("DashScope ASR 任务已提交: taskId={}, configKey={}, audioUrl={}", taskId, config.getKey(), audioUrl);

        JSONObject output = dashScopeHttpClient.pollTaskUntilDone(
                config.getApiKey(), config.getBaseUrl(), taskId);

        JSONArray results = output.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("DashScope ASR 未返回结果 (taskId=" + taskId + ")");
        }

        String transcriptionUrl = results.getJSONObject(0).getStr("transcription_url");
        if (!StringUtils.hasText(transcriptionUrl)) {
            throw new RuntimeException("DashScope ASR 未返回 transcription_url (taskId=" + taskId + ")");
        }

        // 下载并解析转录 JSON
        byte[] transcriptionBytes = dashScopeHttpClient.downloadBytes(transcriptionUrl);
        String transcriptionJson = new String(transcriptionBytes, StandardCharsets.UTF_8);
        JSONObject transcriptionResult = JSONUtil.parseObj(transcriptionJson);

        JSONArray transcripts = transcriptionResult.getJSONArray("transcripts");
        if (transcripts == null || transcripts.isEmpty()) {
            return "";
        }
        return transcripts.getJSONObject(0).getStr("text", "");
    }

    // ============================================================
    //  图片生成
    // ============================================================

    /**
     * 图片生成（qwen-image-2.0-pro 多模态同步模式），返回图片 URL
     *
     * <p>使用 messages 格式，可传入多张参考图片 URL + 文字提示词；
     * 调用 {@code multimodal-generation/generation} 接口，同步返回。</p>
     *
     * @param configKey  模型配置 key
     * @param imageUrls  参考图片 URL 列表（可为空）
     * @param textPrompt 文字提示词
     * @param size       图片尺寸，格式为 "宽*高"，如 "1024*1024"（可为空，使用默认值）
     * @return 生成图片的 URL
     */
    public String generateImage(String configKey, List<String> imageUrls,
                                String textPrompt, String size) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        Assert.hasText(textPrompt, "图片生成提示词不能为空");

        // 构建 content 数组：先放图片，再放文本
        JSONArray contentArr = JSONUtil.createArray();
        if (imageUrls != null) {
            for (String imageUrl : imageUrls) {
                if (StringUtils.hasText(imageUrl)) {
                    contentArr.add(JSONUtil.createObj().set("image", imageUrl));
                }
            }
        }
        contentArr.add(JSONUtil.createObj().set("text", textPrompt));

        JSONArray messagesArr = JSONUtil.createArray();
        messagesArr.add(JSONUtil.createObj()
                .set("role", "user")
                .set("content", contentArr));

        JSONObject parameters = JSONUtil.createObj()
                .set("size", StringUtils.hasText(size) ? size : "1024*1024");

        String reqBody = JSONUtil.createObj()
                .set("model", config.getModelName())
                .set("input", JSONUtil.createObj().set("messages", messagesArr))
                .set("parameters", parameters)
                .toJSONString(0);

        JSONObject output = dashScopeHttpClient.generateImageSync(
                config.getApiKey(), config.getBaseUrl(), reqBody);
        log.info("DashScope 图片生成完成: configKey={}", configKey);

        // 解析 output.choices[0].message.content 中的 image 字段
        JSONArray choices = output.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("DashScope 图片生成未返回 choices");
        }
        JSONArray content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getJSONArray("content");
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("DashScope 图片生成响应中 content 为空");
        }
        for (int i = 0; i < content.size(); i++) {
            String imageUrl = content.getJSONObject(i).getStr("image");
            if (StringUtils.hasText(imageUrl)) {
                return imageUrl;
            }
        }
        throw new RuntimeException("DashScope 图片生成响应中未找到图片 URL");
    }
}
