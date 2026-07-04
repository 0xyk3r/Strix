package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新 AI 会话级系统提示词请求
 *
 * @author ProjectAn
 * @since 2026-07-04
 */
@Schema(description = "更新 AI 会话级系统提示词请求")
@Data
public class AiSessionSystemPromptReq {

    /**
     * 会话级系统提示词覆盖。可为空/空串：表示清除会话覆盖，回退使用模型配置的默认 systemPrompt。
     */
    @Schema(description = "会话级系统提示词（空则清除覆盖，回退模型配置默认）")
    @Size(max = 4096, message = "{validation.size:field.systemPrompt}")
    private String systemPrompt;
}
