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

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "是否启用思考模式：0=禁用 1=启用")
    private Short enableThinking;

    @Schema(description = "思考模式 Token 预算")
    private Integer thinkingBudget;

    @Schema(description = "是否启用代码解释器：0=禁用 1=启用")
    private Short enableCodeInterpreter;

    @Schema(description = "是否启用联网搜索：0=禁用 1=启用")
    private Short enableSearch;

    @Schema(description = "搜索策略：auto/standard/max/agent")
    private String searchStrategy;

    @Schema(description = "是否在响应中附带搜索来源引用：0=禁用 1=启用")
    private Short enableSource;

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
        resp.setBaseUrl(config.getBaseUrl());
        resp.setModelName(config.getModelName());
        resp.setTemperature(config.getTemperature());
        resp.setTopP(config.getTopP());
        resp.setMaxTokens(config.getMaxTokens());
        resp.setSystemPrompt(config.getSystemPrompt());
        resp.setEnableThinking(config.getEnableThinking());
        resp.setThinkingBudget(config.getThinkingBudget());
        resp.setEnableCodeInterpreter(config.getEnableCodeInterpreter());
        resp.setEnableSearch(config.getEnableSearch());
        resp.setSearchStrategy(config.getSearchStrategy());
        resp.setEnableSource(config.getEnableSource());
        resp.setVoice(config.getVoice());
        resp.setSpeed(config.getSpeed());
        resp.setResponseFormat(config.getResponseFormat());
        resp.setLanguage(config.getLanguage());
        resp.setAsrParams(config.getAsrParams());
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
