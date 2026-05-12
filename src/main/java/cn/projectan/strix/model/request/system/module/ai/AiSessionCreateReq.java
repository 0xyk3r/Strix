package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 会话创建请求
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Schema(description = "AI 会话创建请求")
@Data
public class AiSessionCreateReq {

    @Schema(description = "模型配置 ID", example = "123456789")
    @NotEmpty(message = "模型配置 ID 不能为空")
    private String modelConfigId;

    @Schema(description = "会话标题", example = "新对话")
    @NotEmpty(message = "会话标题不能为空")
    @Size(min = 1, max = 256, message = "会话标题长度为 1~256")
    private String title;

}
