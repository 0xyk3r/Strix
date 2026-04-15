package cn.projectan.strix.model.response.system.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知详情响应 (管理员视角, 含接收人列表)
 *
 * @author ProjectAn
 * @since 2026-03-26
 */
@Schema(description = "通知详情响应")
@Data
@NoArgsConstructor
public class NotificationDetailResp {

    @Schema(description = "通知 ID")
    private String id;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "通知状态: 1=有效, 0=已终止")
    private Short status;

    @Schema(description = "跳转类型")
    private String jumpType;

    @Schema(description = "跳转目标")
    private String jumpTarget;

    @Schema(description = "跳转参数 (JSON)")
    private String jumpParams;

    @Schema(description = "发送人昵称")
    private String senderName;

    @Schema(description = "发送时间")
    private LocalDateTime createdTime;

    @Schema(description = "终止原因")
    private String endReason;

    @Schema(description = "终止人昵称")
    private String endByName;

    @Schema(description = "接收人列表")
    private List<ReceiverItem> receivers;

    @Schema(description = "接收人项")
    @Data
    @NoArgsConstructor
    public static class ReceiverItem {

        @Schema(description = "接收人 ID")
        private String receiverId;

        @Schema(description = "接收人昵称")
        private String nickname;

        @Schema(description = "已读状态: 0=未读, 1=已读")
        private Short readStatus;

        @Schema(description = "已读时间")
        private LocalDateTime readAt;

        @Schema(description = "有效状态: 0=无效, 1=有效")
        private Short validStatus;
    }
}
