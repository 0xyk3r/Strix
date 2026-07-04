package cn.projectan.strix.model.request.system.tool.document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文档 AI 分析请求 DTO（Multipart Form 字段）
 *
 * @author ProjectAn
 * @since 2026/6/29
 */
@Data
public class DocumentAiAnalyzeReq {

    /**
     * 分析提示词
     */
    @NotBlank(message = "提示词不能为空")
    @Size(max = 2000, message = "提示词最多 2000 字符")
    private String prompt;

    /**
     * 视觉模型配置 Key
     * <p>文档类（转图片）分析必填；纯文本文件分析可空（改用 textModelKey / 文本模型）。
     * 具体必填校验由 Service 按输入类型判定。</p>
     */
    private String visionModelKey;

    /**
     * 每批图片数量（默认 10，最大 20）
     */
    @Min(value = 1, message = "每批图片数最少为 1")
    @Max(value = 20, message = "每批图片数最多为 20")
    private int batchSize = 10;

    /**
     * 是否合并批次结果
     */
    private boolean merge;

    /**
     * 合并用文本模型配置 Key（merge=true 时必填）
     */
    private String textModelKey;

}
