package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 对话消息发送请求
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Schema(description = "AI 对话消息请求")
@Data
public class AiChatMessageReq {

    @Schema(description = "用户输入文本", example = "你好！")
    @NotEmpty(message = "消息内容不能为空")
    @Size(max = 32000, message = "消息内容过长")
    private String content;

    /**
     * 附件列表（视觉模型使用）
     * <p>每个元素包含：
     * <ul>
     *   <li>{@code type} - 附件类型（image/video）</li>
     *   <li>{@code url} - 已通过 OSS 上传的文件 URL</li>
     *   <li>{@code mimeType} - MIME 类型（如 image/jpeg, video/mp4）</li>
     *   <li>{@code name} - 文件名称</li>
     * </ul>
     */
    @Schema(description = "附件列表（视觉模型，包含 type/url/mimeType/name 字段）")
    private List<Map<String, String>> attachments;

}
