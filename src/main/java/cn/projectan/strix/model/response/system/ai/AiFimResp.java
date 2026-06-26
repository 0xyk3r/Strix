package cn.projectan.strix.model.response.system.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI FIM 续写响应
 *
 * @author ProjectAn
 * @since 2026-06-26
 */
@Schema(description = "AI FIM 续写响应")
@Data
public class AiFimResp {

    @Schema(description = "生成的文本内容")
    private String text;

    @Schema(description = "停止原因（stop=正常结束 / length=达到最大 Token 数限制）")
    private String finishReason;

    @Schema(description = "消耗的 prompt tokens")
    private Integer promptTokens;

    @Schema(description = "生成的 completion tokens")
    private Integer completionTokens;
}
