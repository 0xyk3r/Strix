package cn.projectan.strix.model.response.common.notification;

import cn.projectan.strix.model.db.Notification;
import cn.projectan.strix.model.db.NotificationReceiver;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通知列表响应
 *
 * @author ProjectAn
 * @since 2026/1/13 16:45
 */
@Getter
@NoArgsConstructor
@Schema(description = "通知列表响应")
public class NotificationListResp extends BasePageResp {

    @Schema(description = "通知列表")
    private List<NotificationItem> items = new ArrayList<>();

    public NotificationListResp(List<NotificationReceiver> receivers, List<Notification> notifications, Long total) {
        Map<String, Notification> notificationMap = notifications.stream()
                .collect(Collectors.toMap(Notification::getId, n -> n));

        this.items = receivers.stream()
                .map(receiver -> {
                    Notification notification = notificationMap.get(receiver.getNotificationId());
                    if (notification == null) {
                        return null;
                    }
                    return new NotificationItem(
                            receiver.getId(),
                            receiver.getNotificationId(),
                            notification.getBizType(),
                            notification.getBizId(),
                            notification.getTitle(),
                            notification.getContent(),
                            notification.getJumpType(),
                            notification.getJumpTarget(),
                            notification.getJumpParams(),
                            notification.getSenderId(),
                            receiver.getReadStatus(),
                            receiver.getReadAt(),
                            receiver.getValidStatus(),
                            receiver.getCreatedTime()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "通知项")
    public static class NotificationItem {

        @Schema(description = "接收记录 ID")
        private String id;

        @Schema(description = "通知 ID")
        private String notificationId;

        @Schema(description = "业务类型")
        private String bizType;

        @Schema(description = "业务 ID")
        private String bizId;

        @Schema(description = "通知标题")
        private String title;

        @Schema(description = "通知内容")
        private String content;

        @Schema(description = "跳转类型 (PAGE / URL / NONE)")
        private String jumpType;

        @Schema(description = "跳转目标 (路由名称或 URL)")
        private String jumpTarget;

        @Schema(description = "跳转参数 (JSON)")
        private String jumpParams;

        @Schema(description = "发送人 ID")
        private String senderId;

        @Schema(description = "已读状态 (0未读 1已读)")
        private Short readStatus;

        @Schema(description = "已读时间")
        private LocalDateTime readAt;

        @Schema(description = "有效状态 (1有效 2失效)")
        private Short validStatus;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }

}
