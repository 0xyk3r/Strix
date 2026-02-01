package cn.projectan.strix.model.request.srv.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 拉取消息请求
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@Schema(description = "聊天 - 拉取消息请求")
public class PullMessageReq {

    @NotBlank(message = "会话 ID 不能为空")
    @Schema(description = "会话 ID", example = "1234567890")
    private String sessionId;

    @Schema(description = "最后一条消息 ID（拉取新消息时使用，拉取 id > lastMessageId 的消息）", example = "100")
    private String lastMessageId;

    @Schema(description = "第一条消息 ID（拉取历史消息时使用，拉取 id < firstMessageId 的消息）", example = "50")
    private String firstMessageId;

    @Min(value = 1, message = "拉取数量最小为 1")
    @Max(value = 100, message = "拉取数量最大为 100")
    @Schema(description = "拉取数量", example = "20")
    private Integer limit = 20;

}
