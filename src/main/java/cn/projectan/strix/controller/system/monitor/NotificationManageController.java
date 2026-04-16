package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.system.Notification;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.dict.system.NotificationJumpType;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.request.system.notification.SendNotificationReq;
import cn.projectan.strix.model.response.system.notification.NotificationDetailResp;
import cn.projectan.strix.model.response.system.notification.NotificationManageListResp;
import cn.projectan.strix.service.system.NotificationService;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.util.system.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知管理 (管理员)
 *
 * @author ProjectAn
 * @since 2026-03-26
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/notification")
@RequiredArgsConstructor
@Tag(name = "系统监控 - 通知管理")
public class NotificationManageController extends BaseSystemController {

    private final NotificationService notificationService;
    private final SystemManagerService systemManagerService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:monitor:notification')")
    @Operation(summary = "通知管理列表")
    public RetResult<NotificationManageListResp> list(
            BasePageReq<Notification> pageReq,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short status) {
        return RetBuilder.success(notificationService.getManageList(pageReq, keyword, status));
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:monitor:notification')")
    @Operation(summary = "通知详情")
    public RetResult<NotificationDetailResp> detail(@PathVariable String id) {
        return RetBuilder.success(notificationService.getDetail(id));
    }

    @PostMapping("send")
    @PreAuthorize("@ss.hasPermission('system:monitor:notification:send')")
    @Operation(summary = "发送通知")
    public RetResult<Void> send(@RequestBody @Valid SendNotificationReq req) {
        String senderId = SecurityUtil.getOperatorId();

        // 确定接收人列表
        List<String> receiverIds;
        String bizType;

        if ("BROADCAST".equals(req.getSendMode())) {
            bizType = "SYSTEM_BROADCAST";
            receiverIds = systemManagerService.lambdaQuery()
                    .select(SystemManager::getId)
                    .list()
                    .stream()
                    .map(SystemManager::getId)
                    .toList();
            Assert.isTrue(!receiverIds.isEmpty(), "没有可用的接收人");
        } else if ("TARGETED".equals(req.getSendMode())) {
            bizType = "TARGETED";
            Assert.isTrue(!CollectionUtils.isEmpty(req.getReceiverIds()), "定向通知必须指定接收人");
            receiverIds = req.getReceiverIds();
        } else {
            return RetBuilder.error("无效的发送方式: " + req.getSendMode());
        }

        String jumpType = req.getJumpType();
        if (jumpType == null || jumpType.isBlank()) {
            jumpType = NotificationJumpType.NONE;
        }

        notificationService.sendNotification(
                bizType, null, req.getTitle(), req.getContent(),
                jumpType, req.getJumpTarget(), req.getJumpParams(),
                senderId, receiverIds
        );

        log.info("管理员 {} 发送通知: sendMode={}, receiverCount={}", senderId, req.getSendMode(), receiverIds.size());
        return RetBuilder.success();
    }

    @PostMapping("{id}/terminate")
    @PreAuthorize("@ss.hasPermission('system:monitor:notification:terminate')")
    @Operation(summary = "终止通知")
    public RetResult<Void> terminate(@PathVariable String id, @RequestBody(required = false) TerminateReq req) {
        String operatorId = SecurityUtil.getOperatorId();
        String reason = req != null ? req.reason() : "管理员手动终止";
        notificationService.terminateNotification(id, operatorId, reason);
        return RetBuilder.success();
    }

    @PostMapping("batch-terminate")
    @PreAuthorize("@ss.hasPermission('system:monitor:notification:terminate')")
    @Operation(summary = "批量终止通知")
    public RetResult<Void> batchTerminate(@RequestBody BatchTerminateReq req) {
        String operatorId = SecurityUtil.getOperatorId();
        for (String id : req.ids()) {
            notificationService.terminateNotification(id, operatorId, "批量终止");
        }
        return RetBuilder.success();
    }

    record TerminateReq(String reason) {}
    record BatchTerminateReq(List<String> ids) {}
}
