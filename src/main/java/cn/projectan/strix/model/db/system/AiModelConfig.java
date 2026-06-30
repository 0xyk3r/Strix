package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.annotation.UniqueField;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * AI 模型配置
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_ai_model_config")
public class AiModelConfig extends BaseModel<AiModelConfig> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置唯一标识 Key
     */
    @TableField("`key`")
    @UniqueField("配置 Key")
    private String key;

    /**
     * 配置显示名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 模型类型
     *
     * @see cn.projectan.strix.model.dict.system.AiModelType
     */
    @TableField("`type`")
    private Short type;

    /**
     * 云提供商类型（0=自动识别 1=DashScope 2=DeepSeek 3=OpenAI 9=其他兼容）
     *
     * @see cn.projectan.strix.model.dict.system.AiProviderType
     */
    private Short providerType;

    /**
     * OpenAI 兼容端点 Base URL
     */
    private String baseUrl;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型标识（如 qwen3-max）
     */
    private String modelName;

    /**
     * 温度（TEXT/VISION/STT，nullable）
     */
    private BigDecimal temperature;

    /**
     * TopP（TEXT/VISION，nullable）
     */
    private BigDecimal topP;

    /**
     * 最大输出 Token 数（nullable，即将废弃，推荐使用 maxCompletionTokens）
     */
    private Integer maxTokens;

    /**
     * 最大输出 Token 数（含思考链）
     */
    private Integer maxCompletionTokens;

    /**
     * 存在惩罚 [-2.0, 2.0]
     */
    private BigDecimal presencePenalty;

    /**
     * 频率惩罚 [-2.0, 2.0]
     */
    private BigDecimal frequencyPenalty;

    /**
     * 重复惩罚 (>0)
     */
    private BigDecimal repetitionPenalty;

    /**
     * 候选 Token 数量
     */
    private Integer topK;

    /**
     * 随机数种子
     */
    private Long seed;

    /**
     * 生成响应数量 [1-4]
     */
    private Short n;

    /**
     * 停止词 JSON 数组
     */
    private String stopSequences;

    /**
     * 是否返回 Token 对数概率（0=否 1=是）
     */
    private Short logprobs;

    /**
     * 候选 Token 概率数 [0-5]
     */
    private Short topLogprobs;

    /**
     * 系统提示词（TEXT/VISION，nullable）
     */
    private String systemPrompt;

    /**
     * 支持的多模态输入，JSON 数组（TEXT 类型适用）
     * <p>如 ["image","video","audio"]
     */
    private String supportedModalities;

    /**
     * 是否启用思考模式（TEXT 专用，0=禁用 1=启用）
     */
    private Short enableThinking;

    /**
     * 思考模式 Token 预算（nullable）
     */
    private Integer thinkingBudget;

    /**
     * 是否传递历史思考过程（0=否 1=是）
     */
    private Short preserveThinking;

    /**
     * 推理力度（DeepSeek-V4 专用：high/max）
     */
    private String reasoningEffort;

    /**
     * 是否启用代码解释器（TEXT 流式专用，需同时开启思考模式，0=禁用 1=启用）
     */
    private Short enableCodeInterpreter;

    /**
     * 是否启用联网搜索（TEXT 专用，0=禁用 1=启用）
     */
    private Short enableSearch;

    /**
     * 搜索策略（TEXT 专用，可选：turbo/max/agent/agent_max）
     */
    private String searchStrategy;

    /**
     * 是否在响应中附带搜索来源引用（TEXT 专用，0=禁用 1=启用）
     */
    private Short enableSource;

    /**
     * 强制联网搜索（0=否 1=是）
     */
    private Short forcedSearch;

    /**
     * 搜索时效：7/30/180/365 天
     */
    private Integer searchFreshness;

    /**
     * 是否开启垂域搜索（0=否 1=是）
     */
    private Short enableSearchExtension;

    /**
     * 高分辨率图像处理（0=否 1=是）
     */
    private Short vlHighResolutionImages;

    /**
     * 图像最小像素阈值
     */
    private Integer minPixels;

    /**
     * 图像最大像素阈值
     */
    private Integer maxPixels;

    /**
     * 视频抽帧频率 [0.1-10]
     */
    private BigDecimal videoFps;

    /**
     * 图文混合输出（0=否 1=是）
     */
    private Short enableTextImageMixed;

    /**
     * 语音名称（TTS 专用，cosyvoice-v2 系统音色，如 longxiaochun_v2）
     */
    private String voice;

    /**
     * 克隆音频 URL（TTS 零样本克隆，cosyvoice-v3.5-plus 专用）
     * <p>设置后使用 zero_shot 模式，voice 字段将被忽略</p>
     */
    private String promptAudioUrl;

    /**
     * 语速（TTS 专用，0.25-4.0，nullable）
     */
    private BigDecimal speed;

    /**
     * 响应格式（TTS/STT 专用，如 mp3、wav、json）
     */
    private String responseFormat;

    /**
     * 识别语言（STT 专用，如 zh、en）
     */
    private String language;

    /**
     * ASR 专用：run-task 默认参数（JSON 文本）。
     * <p>会话级参数可在前端覆盖；最终生效 = 会话覆盖 &gt; 本默认 &gt; 系统硬编码默认。
     * 形如：{@code {"semanticPunctuationEnabled":false,"maxSentenceSilence":800,"vocabularyId":"v1"}}
     */
    private String asrParams;

    /**
     * STT 专用：离线 run-task 默认参数（JSON 文本）。
     * <p>请求级参数可在前端覆盖；最终生效 = 请求覆盖 &gt; 本默认 &gt; 系统硬编码默认。
     * 形如：{@code {"diarizationEnabled":true,"speakerCount":2,"vocabularyId":"v1"}}
     */
    private String sttParams;

    /**
     * TTS 专用：语音合成默认参数（JSON 文本）。
     * <p>会话/请求级参数可在前端覆盖；最终生效 = 会话覆盖 &gt; 本默认 &gt; 系统硬编码默认。
     * 形如：{@code {"format":"mp3","sampleRate":22050,"rate":1.0,"enableSsml":false}}
     */
    private String ttsParams;

    /**
     * LiveTranslate 专用：实时语音翻译默认参数（JSON 文本）。
     * <p>会话级参数可在前端覆盖；最终生效 = 会话覆盖 &gt; 本默认 &gt; 系统硬编码默认。
     * 形如：{@code {"sourceLanguage":"zh","targetLanguage":"en","voice":"Tina","modalities":["text","audio"],"enableSourceTranscription":true}}
     */
    private String liveTranslateParams;

    /**
     * 配置状态（0=禁用 1=启用）
     */
    @TableField("`status`")
    private Short status;

    /**
     * 备注
     */
    private String remark;

    /**
     * STT 专用：OSS 配置 Key（用于上传音频文件后获取可公网访问的 URL）
     */
    private String ossConfigKey;

    /**
     * STT 专用：OSS 桶名称
     */
    private String ossBucketName;

}
