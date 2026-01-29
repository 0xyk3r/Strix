package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.notification.ListNotificationReq;
import cn.projectan.strix.model.response.system.notification.NotificationListResp;
import cn.projectan.strix.model.response.system.notification.NotificationUnreadCountResp;
import cn.projectan.strix.service.system.NotificationReceiverService;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 *
 * @author ProjectAn
 * @since 2026/1/13 16:17
 */
@Slf4j
@RestController("SystemNotificationController")
@RequestMapping("system/common/notification")
@RequiredArgsConstructor
@Tag(name = "通用 - 通知")
public class NotificationController extends BaseSystemController {

    private final NotificationReceiverService notificationReceiverService;

    /**
     * 获取我的通知列表
     */
    @PostMapping("")
    @Operation(summary = "获取我的通知列表")
    @ApiOperationSupport(order = 1)
    public RetResult<NotificationListResp> getMyNotifications(@RequestBody ListNotificationReq req) {
        NotificationListResp resp = notificationReceiverService.getMyNotifications(req);
        return RetBuilder.success(resp);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("unread-count")
    @Operation(summary = "获取未读通知数量")
    @ApiOperationSupport(order = 2)
    public RetResult<NotificationUnreadCountResp> getUnreadCount() {
        Long unreadCount = notificationReceiverService.getUnreadCount();
        return RetBuilder.success(new NotificationUnreadCountResp(unreadCount));
    }

    /**
     * 标记单个通知为已读
     */
    @PostMapping("{notificationId}/read")
    @StrixLog(operationGroup = "通知", operationName = "标记已读", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "标记单个通知为已读")
    @ApiOperationSupport(order = 3)
    public RetResult<Object> markAsRead(@PathVariable String notificationId) {
        notificationReceiverService.markAsRead(notificationId);
        return RetBuilder.success();
    }

    /**
     * 标记全部通知为已读
     */
    @PostMapping("read-all")
    @StrixLog(operationGroup = "通知", operationName = "全部已读", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "标记全部通知为已读")
    @ApiOperationSupport(order = 4)
    public RetResult<Object> markAllAsRead() {
        notificationReceiverService.markAllAsRead();
        return RetBuilder.success();
    }

}
