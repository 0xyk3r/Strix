package cn.projectan.strix.model.response.system.ai;

import cn.projectan.strix.model.db.system.AiModelConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 模型配置响应（脱敏：不返回 apiKey）
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Schema(description = "AI 模型配置响应")
@Data
public class AiModelConfigResp {

    @Schema(description = "配置 ID")
    private String id;

    @Schema(description = "配置唯一标识 Key")
    private String key;

    @Schema(description = "配置显示名称")
    private String name;

    @Schema(description = "模型类型：1=TEXT 2=VISION 3=TTS 4=STT(离线) 5=IMAGE_GEN 6=ASR(实时)")
    private Short type;

    @Schema(description = "云提供商类型：0=自动识别 1=DashScope 2=DeepSeek 3=OpenAI 9=其他")
    private Short providerType;

    @Schema(description = "Base URL")
    private String baseUrl;

    @Schema(description = "模型标识")
    private String modelName;

    @Schema(description = "温度")
    private BigDecimal temperature;

    @Schema(description = "TopP")
    private BigDecimal topP;

    @Schema(description = "最大 Token 数")
    private Integer maxTokens;

    @Schema(description = "最大输出 Token 数（含思考链）")
    private Integer maxCompletionTokens;

    @Schema(description = "存在惩罚")
    private BigDecimal presencePenalty;

    @Schema(description = "频率惩罚")
    private BigDecimal frequencyPenalty;

    @Schema(description = "重复惩罚")
    private BigDecimal repetitionPenalty;

    @Schema(description = "候选 Token 数")
    private Integer topK;

    @Schema(description = "随机种子")
    private Long seed;

    @Schema(description = "生成响应数量")
    private Short n;

    @Schema(description = "停止词 JSON 数组")
    private String stopSequences;

    @Schema(description = "是否返回 Token 对数概率")
    private Short logprobs;

    @Schema(description = "候选 Token 概率数")
    private Short topLogprobs;

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "支持的多模态输入 JSON 数组")
    private String supportedModalities;

    @Schema(description = "是否启用思考模式：0=禁用 1=启用")
    private Short enableThinking;

    @Schema(description = "思考模式 Token 预算")
    private Integer thinkingBudget;

    @Schema(description = "是否传递历史思考过程")
    private Short preserveThinking;

    @Schema(description = "推理力度")
    private String reasoningEffort;

    @Schema(description = "是否启用代码解释器：0=禁用 1=启用")
    private Short enableCodeInterpreter;

    @Schema(description = "是否启用联网搜索：0=禁用 1=启用")
    private Short enableSearch;

    @Schema(description = "搜索策略")
    private String searchStrategy;

    @Schema(description = "是否在响应中附带搜索来源引用：0=禁用 1=启用")
    private Short enableSource;

    @Schema(description = "强制联网搜索")
    private Short forcedSearch;

    @Schema(description = "搜索时效")
    private Integer searchFreshness;

    @Schema(description = "垂域搜索")
    private Short enableSearchExtension;

    @Schema(description = "高分辨率图像")
    private Short vlHighResolutionImages;

    @Schema(description = "图像最小像素阈值")
    private Integer minPixels;

    @Schema(description = "图像最大像素阈值")
    private Integer maxPixels;

    @Schema(description = "视频抽帧频率")
    private BigDecimal videoFps;

    @Schema(description = "图文混合输出")
    private Short enableTextImageMixed;

    @Schema(description = "语音名称（TTS）")
    private String voice;

    @Schema(description = "语速（TTS）")
    private BigDecimal speed;

    @Schema(description = "响应格式（TTS/STT）")
    private String responseFormat;

    @Schema(description = "识别语言（STT）")
    private String language;

    @Schema(description = "ASR run-task 默认参数（JSON 文本，ASR 专用）")
    private String asrParams;

    @Schema(description = "STT 离线默认参数（JSON 文本，STT 专用）")
    private String sttParams;

    @Schema(description = "TTS 合成默认参数（JSON 文本，TTS 专用）")
    private String ttsParams;

    @Schema(description = "状态：0=禁用 1=启用")
    private Short status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "TTS 克隆参考音频 URL")
    private String promptAudioUrl;

    @Schema(description = "OSS 配置 Key（STT 专用）")
    private String ossConfigKey;

    @Schema(description = "OSS 桶名称（STT 专用）")
    private String ossBucketName;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    public static AiModelConfigResp from(AiModelConfig config) {
        if (config == null) return null;
        AiModelConfigResp resp = new AiModelConfigResp();
        resp.setId(config.getId());
        resp.setKey(config.getKey());
        resp.setName(config.getName());
        resp.setType(config.getType());
        resp.setProviderType(config.getProviderType());
        resp.setBaseUrl(config.getBaseUrl());
        resp.setModelName(config.getModelName());
        resp.setTemperature(config.getTemperature());
        resp.setTopP(config.getTopP());
        resp.setMaxTokens(config.getMaxTokens());
        resp.setMaxCompletionTokens(config.getMaxCompletionTokens());
        resp.setPresencePenalty(config.getPresencePenalty());
        resp.setFrequencyPenalty(config.getFrequencyPenalty());
        resp.setRepetitionPenalty(config.getRepetitionPenalty());
        resp.setTopK(config.getTopK());
        resp.setSeed(config.getSeed());
        resp.setN(config.getN());
        resp.setStopSequences(config.getStopSequences());
        resp.setLogprobs(config.getLogprobs());
        resp.setTopLogprobs(config.getTopLogprobs());
        resp.setSystemPrompt(config.getSystemPrompt());
        resp.setSupportedModalities(config.getSupportedModalities());
        resp.setEnableThinking(config.getEnableThinking());
        resp.setThinkingBudget(config.getThinkingBudget());
        resp.setPreserveThinking(config.getPreserveThinking());
        resp.setReasoningEffort(config.getReasoningEffort());
        resp.setEnableCodeInterpreter(config.getEnableCodeInterpreter());
        resp.setEnableSearch(config.getEnableSearch());
        resp.setSearchStrategy(config.getSearchStrategy());
        resp.setEnableSource(config.getEnableSource());
        resp.setForcedSearch(config.getForcedSearch());
        resp.setSearchFreshness(config.getSearchFreshness());
        resp.setEnableSearchExtension(config.getEnableSearchExtension());
        resp.setVlHighResolutionImages(config.getVlHighResolutionImages());
        resp.setMinPixels(config.getMinPixels());
        resp.setMaxPixels(config.getMaxPixels());
        resp.setVideoFps(config.getVideoFps());
        resp.setEnableTextImageMixed(config.getEnableTextImageMixed());
        resp.setVoice(config.getVoice());
        resp.setSpeed(config.getSpeed());
        resp.setResponseFormat(config.getResponseFormat());
        resp.setLanguage(config.getLanguage());
        resp.setAsrParams(config.getAsrParams());
        resp.setSttParams(config.getSttParams());
        resp.setTtsParams(config.getTtsParams());
        resp.setStatus(config.getStatus());
        resp.setRemark(config.getRemark());
        resp.setPromptAudioUrl(config.getPromptAudioUrl());
        resp.setOssConfigKey(config.getOssConfigKey());
        resp.setOssBucketName(config.getOssBucketName());
        resp.setCreatedTime(config.getCreatedTime());
        resp.setUpdatedTime(config.getUpdatedTime());
        return resp;
    }
}
