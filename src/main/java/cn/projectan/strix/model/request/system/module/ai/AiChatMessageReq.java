package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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
    @Size(max = 32000, message = "消息内容过长")
    private String content;

    /**
     * 附件列表（多模态输入）
     * <p>每个元素包含 fileId（OssFile ID）、type（image/video/audio）、mimeType、name
     */
    @Schema(description = "附件列表（包含 fileId/type/mimeType/name 字段）")
    @Valid
    @Size(max = 10, message = "附件数量不能超过 10 个")
    private List<AiAttachment> attachments;

}
