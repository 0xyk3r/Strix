package cn.projectan.strix.model.request.system.module.ai;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * AI 聊天附件（发送时提交的结构）
 */
@Data
public class AiAttachment {

    @NotEmpty
    private String fileId;

    @NotEmpty
    private String type;

    private String mimeType;

    private String name;
}
