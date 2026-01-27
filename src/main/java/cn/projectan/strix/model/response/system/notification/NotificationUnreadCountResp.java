package cn.projectan.strix.model.response.system.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读通知数量响应
 *
 * @author ProjectAn
 * @since 2026/1/13 16:45
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "未读通知数量响应")
public class NotificationUnreadCountResp {

    @Schema(description = "未读通知数量", example = "5")
    private Long unreadCount;

}
