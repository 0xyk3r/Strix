package cn.projectan.strix.model.response.system.notification;

import cn.projectan.strix.model.db.system.Notification;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知管理列表响应 (管理员视角)
 *
 * @author ProjectAn
 * @since 2026-03-26
 */
@Schema(description = "通知管理列表响应")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationManageListResp extends BasePageResp {

    @Schema(description = "通知列表")
    private List<NotificationManageItem> items;

    @Schema(description = "总通知数")
    private long totalCount;

    @Schema(description = "有效通知数")
    private long activeCount;

    @Schema(description = "已终止通知数")
    private long terminatedCount;

    @Schema(description = "通知管理列表项")
    @Data
    @NoArgsConstructor
    public static class NotificationManageItem {

        @Schema(description = "通知 ID")
        private String id;

        @Schema(description = "通知标题")
        private String title;

        @Schema(description = "业务类型")
        private String bizType;

        @Schema(description = "通知状态: 1=有效, 0=已终止")
        private Short status;

        @Schema(description = "接收人数")
        private int receiverCount;

        @Schema(description = "已读人数")
        private int readCount;

        @Schema(description = "已读率 (0.0~1.0)")
        private double readRate;

        @Schema(description = "发送人 ID")
        private String senderId;

        @Schema(description = "发送人昵称")
        private String senderName;

        @Schema(description = "跳转类型")
        private String jumpType;

        @Schema(description = "发送时间")
        private LocalDateTime createdTime;

        @Schema(description = "终止原因")
        private String endReason;

        public NotificationManageItem(Notification n) {
            this.id = n.getId();
            this.title = n.getTitle();
            this.bizType = n.getBizType();
            this.status = n.getStatus();
            this.senderId = n.getSenderId();
            this.jumpType = n.getJumpType();
            this.createdTime = n.getCreatedTime();
            this.endReason = n.getEndReason();
        }
    }
}
