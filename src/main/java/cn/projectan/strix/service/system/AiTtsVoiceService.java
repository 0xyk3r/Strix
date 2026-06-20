package cn.projectan.strix.service.system;

import cn.hutool.json.JSONObject;
import cn.projectan.strix.core.module.ai.dashscope.DashScopeHttpClient;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.mapper.system.AiTtsVoiceMapper;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiTtsVoice;
import cn.projectan.strix.model.dict.system.AiTtsVoiceType;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
 * AI TTS 自定义音色服务（声音复刻 / 声音设计）。
 * <p>
 * 复刻/设计为长耗时操作（复刻需轮询审核 ~5min），由 {@link AiTaskService} 异步执行后调用本服务方法落库。
 * 音色与 DashScope voice_id 一一对应，本地表承载在线管理（列表/删除）与展示元数据。
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTtsVoiceService extends ServiceImpl<AiTtsVoiceMapper, AiTtsVoice> {

    private final AiModelConfigService aiModelConfigService;
    private final DashScopeHttpClient dashScopeHttpClient;

    @Autowired(required = false)
    private StrixOssStore ossStore;

    /**
     * 声音复刻：上传参考音频流到 OSS 得公网 URL → 复刻 → 轮询审核 → 落库。返回 voice_id。
     *
     * @param configKey     TTS 模型配置 Key
     * @param name          音色显示名称
     * @param audioStream   参考音频输入流
     * @param contentLength 内容长度（字节）
     * @param fileName      原始文件名
     * @param remark        备注（可空）
     * @return 注册成功的 voice_id
     */
    public String cloneVoiceByUpload(String configKey, String name, InputStream audioStream,
                                     long contentLength, String fileName, String remark) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        String audioUrl = uploadToOss(config, audioStream, contentLength, fileName);
        return doCloneVoice(config, name, audioUrl, remark);
    }

    /**
     * 声音复刻：直接使用公网音频 URL → 复刻 → 轮询审核 → 落库。返回 voice_id。
     */
    public String cloneVoiceByUrl(String configKey, String name, String audioUrl, String remark) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        Assert.hasText(audioUrl, "参考音频 URL 不能为空");
        return doCloneVoice(config, name, audioUrl, remark);
    }

    private String doCloneVoice(AiModelConfig config, String name, String audioUrl, String remark) {
        String prefix = buildPrefix(config.getKey());
        log.info("开始声音复刻: configKey={}, prefix={}", config.getKey(), prefix);

        String voiceId = dashScopeHttpClient.enrollVoice(
                config.getApiKey(), config.getBaseUrl(), config.getModelName(), audioUrl, prefix);
        log.info("声音复刻已提交: configKey={}, voiceId={}", config.getKey(), voiceId);

        dashScopeHttpClient.pollVoiceUntilReady(config.getApiKey(), config.getBaseUrl(), voiceId);

        AiTtsVoice voice = new AiTtsVoice()
                .setConfigId(config.getId())
                .setConfigKey(config.getKey())
                .setVoiceId(voiceId)
                .setName(StringUtils.hasText(name) ? name : voiceId)
                .setVoiceType(AiTtsVoiceType.CLONE)
                .setTargetModel(config.getModelName())
                .setPromptAudioUrl(audioUrl)
                .setStatus("OK")
                .setRemark(remark);
        save(voice);
        log.info("声音复刻完成并落库: configKey={}, voiceId={}", config.getKey(), voiceId);
        return voiceId;
    }

    /**
     * 声音设计：用文字描述创建音色 → 落库。返回 {@code voiceId|previewBase64}（| 分隔，预览可空）。
     *
     * @param configKey   TTS 模型配置 Key
     * @param name        音色显示名称
     * @param voicePrompt 声音描述文本
     * @param previewText 预览朗读文本
     * @param remark      备注（可空）
     * @return {@code voiceId|previewBase64}
     */
    public String designVoice(String configKey, String name, String voicePrompt,
                              String previewText, String remark) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        Assert.hasText(voicePrompt, "声音描述不能为空");
        Assert.hasText(previewText, "预览文本不能为空");

        String prefix = buildPrefix(config.getKey());
        // 预览音频统一用 wav，便于前端直接以 WAV Blob 试听
        log.info("开始声音设计: configKey={}, prefix={}", config.getKey(), prefix);

        String[] result = dashScopeHttpClient.designVoice(
                config.getApiKey(), config.getBaseUrl(), config.getModelName(),
                voicePrompt, previewText, prefix, 24000, "wav");
        String voiceId = result[0];
        String previewAudio = result[1];

        AiTtsVoice voice = new AiTtsVoice()
                .setConfigId(config.getId())
                .setConfigKey(config.getKey())
                .setVoiceId(voiceId)
                .setName(StringUtils.hasText(name) ? name : voiceId)
                .setVoiceType(AiTtsVoiceType.DESIGN)
                .setTargetModel(config.getModelName())
                .setVoicePrompt(voicePrompt)
                .setPreviewText(previewText)
                .setStatus("OK")
                .setRemark(remark);
        save(voice);
        log.info("声音设计完成并落库: configKey={}, voiceId={}", config.getKey(), voiceId);
        return voiceId + "|" + (previewAudio != null ? previewAudio : "");
    }

    /**
     * 查询某 TTS 配置下的音色列表（按创建时间倒序）
     */
    public List<AiTtsVoice> listByConfigKey(String configKey) {
        return lambdaQuery()
                .eq(AiTtsVoice::getConfigKey, configKey)
                .orderByDesc(AiTtsVoice::getCreatedTime)
                .list();
    }

    /**
     * 同步 DashScope 云端音色到本地：拉取账号下全部音色，筛选属于当前模型（target_model 前缀匹配）
     * 且本地不存在的，补建本地记录。返回新增数量。
     *
     * <p>历史音色（在本系统外创建）无法获知原始名称/描述，名称回退为 voice_id，类型按 voice_id
     * 是否含 {@code -vd-} 推断（设计音色 ID 形如 {@code model-vd-prefix-xxx}）。</p>
     *
     * @param configKey TTS 模型配置 Key
     * @return 新同步的音色数量
     */
    public int syncVoices(String configKey) {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey(configKey);
        List<JSONObject> remote = dashScopeHttpClient.listVoices(config.getApiKey(), config.getBaseUrl(), null);

        // 本地已存在的 voiceId 集合（避免重复）
        java.util.Set<String> existing = lambdaQuery()
                .eq(AiTtsVoice::getConfigKey, configKey)
                .list().stream()
                .map(AiTtsVoice::getVoiceId)
                .collect(java.util.stream.Collectors.toSet());

        String model = config.getModelName() != null ? config.getModelName().toLowerCase() : "";
        int added = 0;
        for (JSONObject v : remote) {
            String voiceId = v.getStr("voice_id");
            if (voiceId == null || voiceId.isBlank() || existing.contains(voiceId)) {
                continue;
            }
            // 仅同步绑定到当前模型的音色（voice_id 以 target_model 开头）
            if (!model.isBlank() && !voiceId.toLowerCase().startsWith(model)) {
                continue;
            }
            short voiceType = voiceId.toLowerCase().contains("-vd-")
                    ? AiTtsVoiceType.DESIGN : AiTtsVoiceType.CLONE;
            String status = v.getStr("status", "OK");

            AiTtsVoice voice = new AiTtsVoice()
                    .setConfigId(config.getId())
                    .setConfigKey(configKey)
                    .setVoiceId(voiceId)
                    .setName(voiceId)
                    .setVoiceType(voiceType)
                    .setTargetModel(config.getModelName())
                    .setStatus(status)
                    .setRemark("云端同步");
            save(voice);
            added++;
        }
        log.info("音色同步完成: configKey={}, 云端 {} 个, 新增 {} 个", configKey, remote.size(), added);
        return added;
    }

    /**
     * 删除音色：同步删除 DashScope 音色 + 本地逻辑删除
     */
    public void deleteVoice(String id) {
        AiTtsVoice voice = getById(id);
        Assert.notNull(voice, "音色不存在: " + id);
        AiModelConfig config = aiModelConfigService.getByKey(voice.getConfigKey());
        if (config != null) {
            try {
                dashScopeHttpClient.deleteVoice(config.getApiKey(), config.getBaseUrl(), voice.getVoiceId());
            } catch (Exception e) {
                log.warn("DashScope 音色删除失败（仍继续本地删除）: voiceId={}", voice.getVoiceId(), e);
            }
        }
        removeById(id);
        log.info("音色已删除: id={}, voiceId={}", id, voice.getVoiceId());
    }

    /**
     * 上传音频流到 OSS，返回 1 小时有效的签名 URL（复用 STT 的 OSS 配置）。
     */
    private String uploadToOss(AiModelConfig config, InputStream audioStream, long contentLength, String fileName) {
        String ossConfigKey = config.getOssConfigKey();
        String ossBucketName = config.getOssBucketName();
        Assert.notNull(ossStore, "OSS 模块未启用，无法上传音频文件（请检查 strix.module.oss 配置）");
        Assert.hasText(ossConfigKey, "TTS 配置缺少 ossConfigKey（声音复刻上传音频需要）: " + config.getKey());
        Assert.hasText(ossBucketName, "TTS 配置缺少 ossBucketName: " + config.getKey());

        var ossClient = ossStore.getInstance(ossConfigKey);
        Assert.notNull(ossClient, "OSS 实例不存在: " + ossConfigKey);

        String objectName = "ai/tts/voice/" + UUID.randomUUID() + "_" + fileName;
        ossClient.getPublic().upload(ossBucketName, objectName, audioStream, contentLength);
        log.info("复刻音频已上传至 OSS: bucket={}, object={}", ossBucketName, objectName);
        return ossClient.getPublic().signDownloadUrl(ossBucketName, objectName, 3_600_000L);
    }

    /**
     * 生成音色前缀：仅保留小写字母和数字，最多 10 个字符。
     */
    private String buildPrefix(String configKey) {
        String prefix = configKey.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (prefix.length() > 10) {
            prefix = prefix.substring(0, 10);
        }
        return prefix.isBlank() ? "voice" : prefix;
    }
}
