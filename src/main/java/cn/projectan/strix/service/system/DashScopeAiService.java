package cn.projectan.strix.service.system;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.core.module.ai.stt.OfflineSttProvider;
import cn.projectan.strix.core.module.ai.stt.SttResultJson;
import cn.projectan.strix.core.module.ai.tts.TtsAudioListener;
import cn.projectan.strix.core.module.ai.tts.TtsParams;
import cn.projectan.strix.core.module.ai.tts.TtsProvider;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.model.db.system.AiModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.InputStream;
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
    private final java.util.List<OfflineSttProvider> sttProviders;
    private final java.util.List<TtsProvider> ttsProviders;

    @Autowired(required = false)
    private StrixOssStore ossStore;

    // ============================================================
    //  TTS 语音合成
    // ============================================================

    /**
     * TTS 语音合成（非流式），返回音频字节数组
     *
     * @param configKey  模型配置 key
     * @param text       要合成的文本
     * @param voiceId    音色 ID（声音复刻/设计的 voice_id，覆盖参数中的 voice）
     * @param paramsJson 请求级覆盖参数（JSON，可为 null）
     * @return 音频字节数组
     */
    public byte[] synthesizeSpeech(String configKey, String text, String voiceId, String paramsJson) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        TtsParams params = mergeTtsParams(config, voiceId, paramsJson);
        Assert.hasText(params.voice(), "缺少音色（voice），请先选择或注册音色后再合成");
        return selectProvider(config).synthesize(config, text, params);
    }

    /**
     * TTS 流式语音合成（HTTP SSE），逐段回调音频字节
     *
     * @param configKey  模型配置 key
     * @param text       要合成的文本
     * @param voiceId    音色 ID
     * @param paramsJson 请求级覆盖参数（JSON，可为 null）
     * @param listener   流式音频回调
     */
    public void synthesizeSpeechStream(String configKey, String text, String voiceId,
                                       String paramsJson, TtsAudioListener listener) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        TtsParams params = mergeTtsParams(config, voiceId, paramsJson);
        if (!StringUtils.hasText(params.voice())) {
            listener.onError("缺少音色（voice），请先选择或注册音色后再合成");
            return;
        }
        selectProvider(config).synthesizeStream(config, text, params, listener);
    }

    /**
     * 合并 TTS 参数：模型默认（tts_params 列）作底，请求级覆盖在上，voiceId 最终覆盖 voice。
     */
    public TtsParams mergeTtsParams(AiModelConfig config, String voiceId, String paramsJson) {
        TtsParams merged = TtsParams.fromJson(config.getTtsParams())
                .merge(TtsParams.fromJson(paramsJson));
        if (StringUtils.hasText(voiceId)) {
            merged = merged.merge(new TtsParams(voiceId, null, null, null, null, null, null, null, null, null, null));
        }
        // 兜底：模型配置存量 voice 字段（旧数据兼容）
        if (!StringUtils.hasText(merged.voice()) && StringUtils.hasText(config.getVoice())) {
            merged = merged.merge(new TtsParams(config.getVoice(), null, null, null, null, null, null, null, null, null, null));
        }
        // 兜底：模型配置 responseFormat 作为默认 format
        if (!StringUtils.hasText(merged.format()) && StringUtils.hasText(config.getResponseFormat())) {
            merged = merged.merge(new TtsParams(null, config.getResponseFormat(), null, null, null, null, null, null, null, null, null));
        }
        return merged;
    }

    /**
     * 选首个匹配的 TTS Provider。
     */
    public TtsProvider selectProvider(AiModelConfig config) {
        return ttsProviders.stream()
                .filter(p -> p.supports(config))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到匹配的 TTS Provider: " + config.getModelName()));
    }

    // ============================================================
    //  批量 ASR 语音识别
    // ============================================================

    /**
     * 批量转录：直接传入可公网访问的音频 URL，返回结构化结果的 JSON 字符串
     *
     * @param configKey  模型配置 key
     * @param paramsJson 请求级覆盖参数（JSON，可为 null）
     * @param audioUrl   音频文件的公网 HTTPS URL
     * @return SttResult 的 JSON 字符串
     */
    public String transcribeAudioUrl(String configKey, String paramsJson, String audioUrl) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        return SttResultJson.toJson(doTranscribe(config, paramsJson, audioUrl));
    }

    /**
     * 批量转录：上传音频流到 OSS 后转录（完成后自动删除临时文件），返回结构化结果的 JSON 字符串
     *
     * @param configKey     模型配置 key（需配置 ossConfigKey 和 ossBucketName）
     * @param paramsJson    请求级覆盖参数（JSON，可为 null）
     * @param audioStream   音频输入流
     * @param contentLength 内容长度（字节）
     * @param fileName      原始文件名（用于生成 OSS 对象名）
     * @return SttResult 的 JSON 字符串
     */
    public String transcribeAudio(String configKey, String paramsJson, InputStream audioStream,
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
            return SttResultJson.toJson(doTranscribe(config, paramsJson, signedUrl));
        } finally {
            try {
                ossClient.getPublic().delete(ossBucketName, objectName);
                log.info("STT 临时音频文件已删除: bucket={}, object={}", ossBucketName, objectName);
            } catch (Exception e) {
                log.warn("STT 临时文件删除失败: bucket={}, object={}", ossBucketName, objectName, e);
            }
        }
    }

    /**
     * 合并分层参数 → 选首个匹配 Provider → 转写。
     */
    private cn.projectan.strix.core.module.ai.stt.SttResult doTranscribe(
            AiModelConfig config, String paramsJson, String audioUrl) {
        cn.projectan.strix.core.module.ai.stt.SttParams params =
                cn.projectan.strix.core.module.ai.stt.SttParams.fromJson(config.getSttParams())
                        .merge(cn.projectan.strix.core.module.ai.stt.SttParams.fromJson(paramsJson));
        return sttProviders.stream()
                .filter(p -> p.supports(config))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到匹配的 STT Provider: " + config.getModelName()))
                .transcribe(config, audioUrl, params);
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

        JSONObject output = dashScopeHttpClient.multimodalGenerationSync(
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
