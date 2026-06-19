package cn.projectan.strix.model.request.system.module.ai;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 模型配置新增/更新请求
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Schema(description = "AI 模型配置请求")
@Data
public class AiModelConfigUpdateReq {

    @Schema(description = "配置唯一标识 Key", example = "qwen3-max-text")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "配置 Key 不能为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 64, message = "配置 Key 长度为 2~64")
    @UpdateField
    private String key;

    @Schema(description = "配置显示名称", example = "千问3-Max 文本模型")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "配置名称不能为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 128, message = "配置名称长度为 2~128")
    @UpdateField
    private String name;

    /**
     * 模型类型
     *
     * @see cn.projectan.strix.model.dict.system.AiModelType
     */
    @Schema(description = "模型类型：1=TEXT 2=VISION 3=TTS 4=STT(离线) 5=IMAGE_GEN 6=ASR(实时)", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "模型类型不能为空")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 1, message = "模型类型不合法")
    @Max(groups = {InsertGroup.class, UpdateGroup.class}, value = 6, message = "模型类型不合法")
    @UpdateField
    private Short type;

    @Schema(description = "OpenAI 兼容端点 Base URL", example = "https://dashscope.aliyuncs.com/compatible-mode/v1")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "Base URL 不能为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 512, message = "Base URL 过长")
    @UpdateField
    private String baseUrl;

    @Schema(description = "API Key")
    @NotEmpty(groups = {InsertGroup.class}, message = "API Key 不能为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 512, message = "API Key 过长")
    @UpdateField
    private String apiKey;

    @Schema(description = "模型标识", example = "qwen3-max")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "模型标识不能为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 128, message = "模型标识过长")
    @UpdateField
    private String modelName;

    @Schema(description = "温度（0.0-2.0，TEXT/VISION/STT 可用）", example = "0.7")
    @DecimalMin(groups = {InsertGroup.class, UpdateGroup.class}, value = "0.0", message = "温度值不合法")
    @DecimalMax(groups = {InsertGroup.class, UpdateGroup.class}, value = "2.0", message = "温度值不合法")
    @UpdateField(allowEmpty = true)
    private BigDecimal temperature;

    @Schema(description = "TopP（0.0-1.0，TEXT/VISION 可用）", example = "0.9")
    @DecimalMin(groups = {InsertGroup.class, UpdateGroup.class}, value = "0.0", message = "TopP 值不合法")
    @DecimalMax(groups = {InsertGroup.class, UpdateGroup.class}, value = "1.0", message = "TopP 值不合法")
    @UpdateField(allowEmpty = true)
    private BigDecimal topP;

    @Schema(description = "最大输出 Token 数", example = "8192")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 1, message = "最大 Token 数不合法")
    @UpdateField(allowEmpty = true)
    private Integer maxTokens;

    @Schema(description = "系统提示词（TEXT/VISION 可用）")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 4096, message = "系统提示词过长")
    @UpdateField(allowEmpty = true)
    private String systemPrompt;

    @Schema(description = "是否启用思考模式（TEXT 专用）：0=禁用 1=启用", example = "1")
    @UpdateField(allowEmpty = true)
    private Short enableThinking;

    @Schema(description = "思考模式 Token 预算（TEXT 专用）", example = "8192")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 1, message = "思考预算不合法")
    @UpdateField(allowEmpty = true)
    private Integer thinkingBudget;

    @Schema(description = "是否启用代码解释器（TEXT 流式专用，需同时开启思考模式）：0=禁用 1=启用", example = "0")
    @UpdateField(allowEmpty = true)
    private Short enableCodeInterpreter;

    @Schema(description = "是否启用联网搜索（TEXT 专用）：0=禁用 1=启用", example = "0")
    @UpdateField(allowEmpty = true)
    private Short enableSearch;

    @Schema(description = "搜索策略（TEXT 专用）：auto/standard/max/agent", example = "auto")
    @Pattern(groups = {InsertGroup.class, UpdateGroup.class}, regexp = "^(auto|standard|max|agent)$",
            message = "搜索策略不合法")
    @UpdateField(allowEmpty = true)
    private String searchStrategy;

    @Schema(description = "是否在响应中附带搜索来源引用（TEXT 专用）：0=禁用 1=启用", example = "0")
    @UpdateField(allowEmpty = true)
    private Short enableSource;

    @Schema(description = "语音名称（TTS 专用）", example = "cosyvoice-v2-longxiaochun")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64, message = "语音名称过长")
    @UpdateField(allowEmpty = true)
    private String voice;

    @Schema(description = "语速（TTS 专用，0.25-4.0）", example = "1.0")
    @DecimalMin(groups = {InsertGroup.class, UpdateGroup.class}, value = "0.25", message = "语速值不合法")
    @DecimalMax(groups = {InsertGroup.class, UpdateGroup.class}, value = "4.0", message = "语速值不合法")
    @UpdateField(allowEmpty = true)
    private BigDecimal speed;

    @Schema(description = "响应格式（TTS/STT 专用，如 mp3、wav、json）", example = "mp3")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 16, message = "响应格式过长")
    @UpdateField(allowEmpty = true)
    private String responseFormat;

    @Schema(description = "识别语言（STT 专用，如 zh、en）", example = "zh")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 16, message = "语言代码过长")
    @UpdateField(allowEmpty = true)
    private String language;

    @Schema(description = "ASR run-task 默认参数（JSON 文本，ASR 专用）")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 2048, message = "ASR 参数过长")
    @UpdateField(allowEmpty = true)
    private String asrParams;

    @Schema(description = "STT 离线默认参数（JSON 文本，STT 专用）")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 2048, message = "STT 参数过长")
    @UpdateField(allowEmpty = true)
    private String sttParams;

    @Schema(description = "配置状态：0=禁用 1=启用", example = "1")
    @UpdateField(allowEmpty = true)
    private Short status;

    @Schema(description = "备注")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 512, message = "备注过长")
    @UpdateField(allowEmpty = true)
    private String remark;

    @Schema(description = "OSS 配置 Key（STT 专用）")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64, message = "OSS 配置 Key 过长")
    @UpdateField(allowEmpty = true)
    private String ossConfigKey;

    @Schema(description = "OSS 桶名称（STT 专用）")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 128, message = "OSS 桶名称过长")
    @UpdateField(allowEmpty = true)
    private String ossBucketName;

    @Schema(description = "TTS 克隆参考音频 URL（TTS 零样本克隆专用）")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 512, message = "参考音频 URL 过长")
    @UpdateField(allowEmpty = true)
    private String promptAudioUrl;

}
