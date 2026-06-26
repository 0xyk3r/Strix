package cn.projectan.strix.model.request.system.module.ai;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI FIM / Chat Prefix 续写请求
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li><b>FIM 填充</b>（{@code chatPrefix=false}）：{@code POST /beta/completions}，
 *       使用 {@code prompt}（前缀）+ 可选 {@code suffix}（后缀）</li>
 *   <li><b>对话前缀续写</b>（{@code chatPrefix=true}）：{@code POST /beta/chat/completions}，
 *       构造 messages 数组，最后一条 assistant 消息带 {@code prefix:true}</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-06-26
 */
@Data
public class AiFimReq {

    /**
     * AI 模型配置 Key（必须为支持 FIM 的提供商，如 DeepSeek）
     */
    @NotEmpty(message = "请选择模型")
    private String modelKey;

    /**
     * FIM 模式：前缀文本（必填）
     * 对话前缀续写模式：assistant 前缀内容（必填）
     */
    @NotEmpty(message = "请输入前缀内容")
    @Size(max = 8000, message = "前缀内容不能超过 8000 字符")
    private String prompt;

    /**
     * FIM 专用：后缀文本（可选，提供时使用填充模式，否则为纯续写）
     */
    @Size(max = 8000, message = "后缀内容不能超过 8000 字符")
    private String suffix;

    /**
     * 对话前缀续写专用：系统提示词（可选）
     */
    @Size(max = 4000, message = "系统提示词不能超过 4000 字符")
    private String systemPrompt;

    /**
     * 对话前缀续写专用：用户消息内容（可选，提供背景上下文）
     */
    @Size(max = 8000, message = "用户消息不能超过 8000 字符")
    private String userContent;

    /**
     * 是否使用对话前缀续写模式（{@code true} = Chat Prefix，{@code false/null} = FIM）
     */
    private Boolean chatPrefix;

    /**
     * 最大生成 Token 数，DeepSeek FIM Beta 限制 4K。
     * 不填时：优先使用模型配置的 maxTokens，否则默认 1024。
     */
    @Min(value = 1, message = "最大生成 Token 数不能小于 1")
    @Max(value = 4096, message = "最大生成 Token 数不能超过 4096")
    private Integer maxTokens;

    /**
     * 温度（可选），覆盖模型配置中的 temperature。
     */
    @DecimalMin(value = "0.0", message = "温度不能小于 0")
    @DecimalMax(value = "2.0", message = "温度不能大于 2")
    private BigDecimal temperature;
}
