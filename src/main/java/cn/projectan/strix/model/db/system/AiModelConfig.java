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
     * 最大输出 Token 数（nullable）
     */
    private Integer maxTokens;

    /**
     * 系统提示词（TEXT/VISION，nullable）
     */
    private String systemPrompt;

    /**
     * 是否启用思考模式（TEXT 专用，0=禁用 1=启用）
     */
    private Short enableThinking;

    /**
     * 思考模式 Token 预算（nullable）
     */
    private Integer thinkingBudget;

    /**
     * 是否启用代码解释器（TEXT 流式专用，需同时开启思考模式，0=禁用 1=启用）
     * <p>
     * 仅在流式调用（streamChat）时生效，非流式调用忽略此参数。
     */
    private Short enableCodeInterpreter;

    /**
     * 是否启用联网搜索（TEXT 专用，0=禁用 1=启用）
     */
    private Short enableSearch;

    /**
     * 搜索策略（TEXT 专用，可选：auto/standard/max/agent）
     * <ul>
     *   <li>auto - 模型自主判断是否搜索（默认）</li>
     *   <li>standard - 标准搜索</li>
     *   <li>max - 高性能搜索</li>
     *   <li>agent - 深度研究场景</li>
     * </ul>
     */
    private String searchStrategy;

    /**
     * 是否在响应中附带搜索来源引用（TEXT 专用，0=禁用 1=启用）
     */
    private Short enableSource;

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
