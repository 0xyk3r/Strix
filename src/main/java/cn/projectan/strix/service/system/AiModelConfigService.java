package cn.projectan.strix.service.system;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.ai.AiModelStore;
import cn.projectan.strix.mapper.system.AiModelConfigMapper;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import cn.projectan.strix.model.response.system.ai.AiModelInfoResp;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 模型配置服务
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelConfigService extends ServiceImpl<AiModelConfigMapper, AiModelConfig> {

    private final AiModelStore aiModelStore;

    /**
     * 复用的 OkHttpClient（获取模型列表用）
     * <p>避免每次调用 {@code new OkHttpClient()} 导致连接池/调度线程泄漏。
     */
    private final OkHttpClient modelListHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 根据 key 查询配置
     */
    public AiModelConfig getByKey(String key) {
        return lambdaQuery()
                .eq(AiModelConfig::getKey, key)
                .one();
    }

    /**
     * 根据 key 获取已启用的配置，不存在或未启用则抛出异常
     */
    public AiModelConfig requireEnabledByKey(String key) {
        AiModelConfig config = getByKey(key);
        Assert.notNull(config, "AI 模型配置不存在: " + key);
        Assert.isTrue(config.getStatus() != null && config.getStatus() == 1, "AI 模型配置未启用: " + key);
        return config;
    }

    /**
     * 保存配置后清除缓存
     */
    public boolean saveAndInvalidate(AiModelConfig config) {
        boolean result = save(config);
        if (result) {
            aiModelStore.invalidate(config.getKey());
        }
        return result;
    }

    /**
     * 更新配置后清除缓存
     */
    public boolean updateAndInvalidate(AiModelConfig config) {
        boolean result = updateById(config);
        if (result) {
            aiModelStore.invalidate(config.getKey());
        }
        return result;
    }

    /**
     * 更新音色 ID（TTS 音色注册完成后调用），同时清除缓存
     *
     * @param id      配置 ID
     * @param voiceId 注册得到的 voice_id
     */
    public boolean updateVoice(String id, String voiceId) {
        AiModelConfig existing = getById(id);
        AiModelConfig update = new AiModelConfig();
        update.setId(id);
        update.setVoice(voiceId);
        boolean result = updateById(update);
        if (result && existing != null) {
            aiModelStore.invalidate(existing.getKey());
        }
        return result;
    }

    /**
     * 删除配置后清除缓存
     */
    public boolean removeAndInvalidate(String id) {
        AiModelConfig config = getById(id);
        boolean result = removeById(id);
        if (result && config != null) {
            aiModelStore.invalidate(config.getKey());
        }
        return result;
    }

    /**
     * 从 OpenAI Compatible API 或 DashScope 获取可用模型列表
     *
     * @param baseUrl 基础 URL
     * @param apiKey  API Key
     * @return 模型信息列表
     */
    public List<AiModelInfoResp> fetchAvailableModels(String baseUrl, String apiKey) {
        String normalizedUrl = normalizeBaseUrl(baseUrl);

        // 优先尝试 OpenAI Compatible API
        try {
            return fetchOpenAiCompatibleModels(normalizedUrl, apiKey);
        } catch (Exception e) {
            log.warn("获取模型列表失败（疑似不兼容端点）: baseUrl={}, error={}", normalizedUrl, e.getMessage());
            throw new StrixException("不兼容的 API 端点", e);
        }
    }

    /**
     * 标准化 Base URL（移除尾部斜杠）
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return baseUrl;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 根据模型名称推测模型类型
     */
    private short inferModelType(String modelId) {
        String lower = modelId.toLowerCase();

        // 图片生成模型（优先级最高）
        if (lower.contains("dall-e") || lower.contains("dalle") ||
                lower.contains("stable-diffusion") || lower.contains("midjourney") ||
                lower.matches(".*\\b(qwen|wan|z)-?image.*") || lower.contains("wanx")) {
            return AiModelType.IMAGE_GEN;
        }

        // TTS 模型
        if (lower.contains("tts-instruct") || lower.contains("tts-flash") ||
                lower.contains("speech-") || lower.matches(".*\\btts\\b.*") ||
                (lower.contains("speech") && !lower.contains("speechless"))) {
            return AiModelType.TTS;
        }

        // 实时语音识别 ASR（流式：paraformer-realtime / gummy-realtime 等）—— 须先于离线 STT 判断
        if (lower.contains("realtime") || lower.contains("gummy")) {
            return AiModelType.ASR;
        }

        // STT 模型（离线 / 批量转写）
        if (lower.contains("whisper") || lower.contains("stt") ||
                lower.contains("transcribe") || lower.contains("-asr-")) {
            return AiModelType.STT;
        }

        // VISION 模型（需要精确匹配，避免误判）
        // 1. 明确的 vision 标记
        if (lower.contains("vision") || lower.contains("-vl-") || lower.contains("qwen-vl") ||
                lower.contains("qwen3-vl") || lower.contains("-ocr")) {
            return AiModelType.VISION;
        }

        // 2. TTS 的 voice duplication/conversion 变体
        if (lower.contains("tts-vd") || lower.contains("tts-vc")) {
            return AiModelType.VISION;  // 按数据标注保持一致
        }

        // 默认为文本模型
        return AiModelType.TEXT;
    }

    /**
     * 从 OpenAI Compatible API 获取模型列表
     */
    private List<AiModelInfoResp> fetchOpenAiCompatibleModels(String baseUrl, String apiKey) throws IOException {
        // 尝试多个可能的路径
        String[] possiblePaths = {"/v1/models", "/models", "/api/v1/models"};
        IOException lastException = null;

        for (String path : possiblePaths) {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + path)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .get()
                        .build();

                try (Response response = modelListHttpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        JSONObject json = JSONUtil.parseObj(body);
                        JSONArray data = json.getJSONArray("data");

                        // 校验响应是否为合法的模型列表（OpenAI /models 规范：data 为对象数组且含 id 字段）
                        if (data == null || data.isEmpty()) {
                            throw new IOException("响应不含 data 模型数组，疑似不兼容端点");
                        }

                        List<AiModelInfoResp> result = new ArrayList<>();
                        for (int i = 0; i < data.size(); i++) {
                            JSONObject model = data.getJSONObject(i);
                            String modelId = model != null ? model.getStr("id") : null;
                            if (modelId == null || modelId.isBlank()) {
                                continue; // 跳过无 id 的非法条目
                            }

                            AiModelInfoResp info = new AiModelInfoResp();
                            info.setId(modelId);
                            info.setName(modelId); // 默认使用 id 作为 name
                            info.setOwnedBy(model.getStr("owned_by", "unknown"));
                            info.setCreated(model.getLong("created", System.currentTimeMillis() / 1000));
                            info.setType((int) inferModelType(info.getId()));

                            result.add(info);
                        }

                        if (result.isEmpty()) {
                            throw new IOException("响应 data 数组中无合法模型条目，疑似不兼容端点");
                        }

                        log.info("成功从 {} 获取 {} 个模型", baseUrl + path, result.size());
                        return result;
                    } else {
                        lastException = new IOException("HTTP " + response.code() + ": " + response.message());
                    }
                }
            } catch (IOException e) {
                lastException = e;
                log.debug("尝试路径 {} 失败: {}", baseUrl + path, e.getMessage());
            }
        }

        throw lastException != null ? lastException : new IOException("所有路径尝试均失败");
    }

    /**
     * 从 DashScope 获取模型列表（预定义）
     */
    private List<AiModelInfoResp> fetchDashScopeModels(String apiKey) {
        // DashScope 目前不提供动态模型列表 API，返回预定义列表
        List<AiModelInfoResp> models = new ArrayList<>();

        // 文本模型
        addDashScopeModel(models, "qwen-turbo", "通义千问 Turbo", "alibaba", AiModelType.TEXT);
        addDashScopeModel(models, "qwen-plus", "通义千问 Plus", "alibaba", AiModelType.TEXT);
        addDashScopeModel(models, "qwen-max", "通义千问 Max", "alibaba", AiModelType.TEXT);
        addDashScopeModel(models, "qwen-long", "通义千问 Long", "alibaba", AiModelType.TEXT);

        // 视觉模型
        addDashScopeModel(models, "qwen-vl-plus", "通义千问 VL Plus", "alibaba", AiModelType.VISION);
        addDashScopeModel(models, "qwen-vl-max", "通义千问 VL Max", "alibaba", AiModelType.VISION);

        // 图片生成模型
        addDashScopeModel(models, "wanx-v1", "通义万相 V1", "alibaba", AiModelType.IMAGE_GEN);

        log.info("返回 DashScope 预定义模型列表，共 {} 个模型", models.size());
        return models;
    }

    /**
     * 添加 DashScope 模型到列表
     */
    private void addDashScopeModel(List<AiModelInfoResp> models, String id, String name, String ownedBy, short type) {
        AiModelInfoResp info = new AiModelInfoResp();
        info.setId(id);
        info.setName(name);
        info.setOwnedBy(ownedBy);
        info.setCreated(System.currentTimeMillis() / 1000);
        info.setType((int) type);
        models.add(info);
    }

}
