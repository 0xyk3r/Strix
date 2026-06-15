package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 会话重命名请求
 *
 * @author ProjectAn
 * @since 2026-05-17
 */
@Schema(description = "AI 会话重命名请求")
@Data
public class AiSessionRenameTitleReq {

    @Schema(description = "新会话标题")
    @NotEmpty(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过 100 个字符")
    private String title;
}
