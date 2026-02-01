package cn.projectan.strix.model.request.srv.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建/获取会话请求
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@Schema(description = "聊天 - 创建/获取会话请求")
public class CreateSessionReq {

    @NotBlank(message = "配置 Key 不能为空")
    @Schema(description = "配置 Key", example = "CUSTOMER_SERVICE")
    private String configKey;

    @Schema(description = "业务类型", example = "ORDER")
    private String bizType;

    @Schema(description = "业务 ID", example = "123456")
    private String bizId;

    @Schema(description = "对方用户 ID（单聊时使用）", example = "user_123")
    private String otherUserId;

    @Schema(description = "群聊名称（群聊时使用）", example = "项目讨论组")
    private String groupName;

    @Size(min = 1, message = "成员列表不能为空")
    @Schema(description = "成员用户 ID 列表（群聊时使用）", example = "[\"user_1\", \"user_2\", \"user_3\"]")
    private List<String> memberUserIds;

}
