package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * AI 会话切换模型请求
 *
 * @author ProjectAn
 * @since 2026-06-16
 */
@Schema(description = "AI 会话切换模型请求")
@Data
public class AiSessionSwitchModelReq {

    @Schema(description = "目标模型配置 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "{validation.required:field.modelConfigId}")
    private String modelConfigId;

}
