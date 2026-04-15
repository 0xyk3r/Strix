package cn.projectan.strix.service.system;

import cn.projectan.strix.core.sse.SseSessionManager;
import cn.projectan.strix.mapper.system.NotificationMapper;
import cn.projectan.strix.model.db.system.Notification;
import cn.projectan.strix.model.db.system.NotificationReceiver;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.dict.common.CommonFlag;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.response.system.notification.NotificationDetailResp;
import cn.projectan.strix.model.response.system.notification.NotificationManageListResp;
import cn.projectan.strix.model.response.system.notification.NotificationManageListResp.NotificationManageItem;
import cn.projectan.strix.util.common.I18nUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Strix 通知服务
 *
 * @author ProjectAn
 * @since 2026-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    private final NotificationReceiverService notificationReceiverService;
    private final SseSessionManager sseSessionManager;
    private final SystemManagerService systemManagerService;

    /**
     * 发送通知（批量）
     * 由业务逻辑调用
     *
     * @param bizType     业务类型
     * @param bizId       业务 ID
     * @param title       通知标题
     * @param content     通知内容
     * @param jumpType    跳转类型
     * @param jumpTarget  跳转目标
     * @param jumpParams  跳转参数（JSON）
     * @param senderId    发送人 ID（系统通知可为null）
     * @param receiverIds 接收人 ID 列表
     * @return 通知 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String sendNotification(String bizType, String bizId, String title, String content,
                                   String jumpType, String jumpTarget, String jumpParams,
                                   String senderId, List<String> receiverIds) {
        Assert.hasText(title, I18nUtil.notEmpty("field.notification.title"));
        if (CollectionUtils.isEmpty(receiverIds)) {
            log.warn("发送通知失败，接收人列表为空，业务类型: {}, 业务ID: {}", bizType, bizId);
            return null;
        }

        // 创建通知
        Notification notification = new Notification()
                .setBizType(bizType)
                .setBizId(bizId)
                .setTitle(title)
                .setContent(content)
                .setJumpType(jumpType)
                .setJumpTarget(jumpTarget)
                .setJumpParams(jumpParams)
                .setSenderId(senderId)
                .setStatus(CommonFlag.YES);

        Assert.isTrue(save(notification), "发送通知失败，请稍后重试");

        // 创建通知接收人记录
        List<NotificationReceiver> receivers = receiverIds.stream()
                .map(receiverId -> new NotificationReceiver()
                        .setNotificationId(notification.getId())
                        .setReceiverId(receiverId)
                        .setReadStatus(CommonFlag.NO)
                        .setValidStatus(CommonFlag.YES))
                .collect(Collectors.toList());

        Assert.isTrue(notificationReceiverService.saveBatch(receivers), "创建接收人记录失败，请稍后重试");

        log.info("通知发送成功，通知ID: {}, 业务类型: {}, 接收人数量: {}", notification.getId(), bizType, receiverIds.size());

        // SSE 推送: 仅推送给已连接的管理员
        pushNewNotificationEvent(notification, receiverIds);

        return notification.getId();
    }

    /**
     * 终止通知 (按业务类型和业务 ID)
     *
     * @param bizType      业务类型
     * @param bizId        业务 ID
     * @param terminatedBy 终止人 ID
     * @param reason       终止原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void terminateNotification(String bizType, String bizId, String terminatedBy, String reason) {
        List<Notification> notifications = lambdaQuery()
                .eq(Notification::getBizType, bizType)
                .eq(Notification::getBizId, bizId)
                .list();

        if (!CollectionUtils.isEmpty(notifications)) {
            lambdaUpdate()
                    .eq(Notification::getBizType, bizType)
                    .eq(Notification::getBizId, bizId)
                    .set(Notification::getStatus, CommonFlag.NO)
                    .set(Notification::getEndBy, terminatedBy)
                    .set(Notification::getEndReason, reason)
                    .update();

            List<String> notificationIds = notifications.stream()
                    .map(Notification::getId)
                    .toList();

            notificationReceiverService.lambdaUpdate()
                    .in(NotificationReceiver::getNotificationId, notificationIds)
                    .set(NotificationReceiver::getValidStatus, CommonFlag.NO)
                    .set(NotificationReceiver::getInvalidAt, LocalDateTime.now())
                    .set(NotificationReceiver::getInvalidBy, terminatedBy)
                    .update();
        }
    }

    /**
     * 终止通知 (按通知 ID)
     *
     * @param notificationId 通知 ID
     * @param terminatedBy   终止人 ID
     * @param reason         终止原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void terminateNotification(String notificationId, String terminatedBy, String reason) {
        Notification notification = getById(notificationId);
        Assert.notNull(notification, I18nUtil.notFound("field.notification"));

        notification.setStatus(CommonFlag.NO)
                .setEndBy(terminatedBy)
                .setEndReason(reason);
        Assert.isTrue(updateById(notification), "终止通知失败，请稍后重试");

        LocalDateTime now = LocalDateTime.now();
        notificationReceiverService.lambdaUpdate()
                .eq(NotificationReceiver::getNotificationId, notificationId)
                .set(NotificationReceiver::getValidStatus, CommonFlag.NO)
                .set(NotificationReceiver::getInvalidAt, now)
                .set(NotificationReceiver::getInvalidBy, terminatedBy)
                .update();

        log.info("通知已终止，通知ID: {}, 终止人: {}, 终止原因: {}", notificationId, terminatedBy, reason);

        // SSE 推送: 通知被终止后, 推送更新后的未读数量给相关接收人
        pushCountUpdateForNotification(notificationId);
    }

    // ======================== Admin Query Methods ========================

    /**
     * 获取通知管理列表 (管理员视角)
     */
    public NotificationManageListResp getManageList(BasePageReq<Notification> req, String keyword, Short status) {
        Page<Notification> page = lambdaQuery()
                .like(StringUtils.hasText(keyword), Notification::getTitle, keyword)
                .eq(status != null, Notification::getStatus, status)
                .orderByDesc(Notification::getCreatedTime)
                .page(req.getPage());

        List<Notification> records = page.getRecords();
        List<NotificationManageItem> items;

        if (records.isEmpty()) {
            items = List.of();
        } else {
            // 批量获取接收人统计 (避免 N+1)
            List<String> notificationIds = records.stream().map(Notification::getId).toList();
            List<NotificationReceiver> allReceivers = notificationReceiverService.lambdaQuery()
                    .select(NotificationReceiver::getNotificationId, NotificationReceiver::getReadStatus)
                    .in(NotificationReceiver::getNotificationId, notificationIds)
                    .eq(NotificationReceiver::getValidStatus, CommonFlag.YES)
                    .list();

            Map<String, Long> receiverCounts = allReceivers.stream()
                    .collect(Collectors.groupingBy(NotificationReceiver::getNotificationId, Collectors.counting()));
            Map<String, Long> readCounts = allReceivers.stream()
                    .filter(r -> r.getReadStatus() == CommonFlag.YES)
                    .collect(Collectors.groupingBy(NotificationReceiver::getNotificationId, Collectors.counting()));

            // 批量获取发送人昵称
            Set<String> senderIds = records.stream()
                    .map(Notification::getSenderId)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
            Map<String, String> senderNames = resolveManagerNames(senderIds);

            items = records.stream().map(n -> {
                NotificationManageItem item = new NotificationManageItem(n);
                long rc = receiverCounts.getOrDefault(n.getId(), 0L);
                long readC = readCounts.getOrDefault(n.getId(), 0L);
                item.setReceiverCount((int) rc);
                item.setReadCount((int) readC);
                item.setReadRate(rc > 0 ? (double) readC / rc : 0);
                item.setSenderName(senderNames.getOrDefault(n.getSenderId(), "系统"));
                return item;
            }).toList();
        }

        // 统计数据
        long totalCount = count();
        long activeCount = lambdaQuery().eq(Notification::getStatus, CommonFlag.YES).count();
        long terminatedCount = totalCount - activeCount;

        NotificationManageListResp resp = new NotificationManageListResp();
        resp.setItems(items);
        resp.setTotal(page.getTotal());
        resp.setTotalCount(totalCount);
        resp.setActiveCount(activeCount);
        resp.setTerminatedCount(terminatedCount);

        return resp;
    }

    /**
     * 获取通知详情 (管理员视角, 含接收人列表)
     */
    public NotificationDetailResp getDetail(String notificationId) {
        Notification notification = getById(notificationId);
        Assert.notNull(notification, I18nUtil.notFound("field.notification"));

        List<NotificationReceiver> receivers = notificationReceiverService.lambdaQuery()
                .eq(NotificationReceiver::getNotificationId, notificationId)
                .list();

        // 收集需要查询昵称的 ID
        Set<String> managerIds = receivers.stream()
                .map(NotificationReceiver::getReceiverId)
                .collect(Collectors.toCollection(HashSet::new));
        if (StringUtils.hasText(notification.getSenderId())) {
            managerIds.add(notification.getSenderId());
        }
        if (StringUtils.hasText(notification.getEndBy())) {
            managerIds.add(notification.getEndBy());
        }

        Map<String, String> nameMap = resolveManagerNames(managerIds);

        NotificationDetailResp resp = new NotificationDetailResp();
        resp.setId(notification.getId());
        resp.setTitle(notification.getTitle());
        resp.setContent(notification.getContent());
        resp.setBizType(notification.getBizType());
        resp.setStatus(notification.getStatus());
        resp.setJumpType(notification.getJumpType());
        resp.setJumpTarget(notification.getJumpTarget());
        resp.setJumpParams(notification.getJumpParams());
        resp.setSenderName(nameMap.getOrDefault(notification.getSenderId(), "系统"));
        resp.setCreatedTime(notification.getCreatedTime());
        resp.setEndReason(notification.getEndReason());
        resp.setEndByName(nameMap.getOrDefault(notification.getEndBy(), null));

        List<NotificationDetailResp.ReceiverItem> receiverItems = receivers.stream().map(r -> {
            NotificationDetailResp.ReceiverItem item = new NotificationDetailResp.ReceiverItem();
            item.setReceiverId(r.getReceiverId());
            item.setNickname(nameMap.getOrDefault(r.getReceiverId(), "Unknown"));
            item.setReadStatus(r.getReadStatus());
            item.setReadAt(r.getReadAt());
            item.setValidStatus(r.getValidStatus());
            return item;
        }).toList();
        resp.setReceivers(receiverItems);

        return resp;
    }

    // ======================== SSE Push Helpers ========================

    private void pushNewNotificationEvent(Notification notification, List<String> receiverIds) {
        Map<String, Object> eventData = Map.of(
                "id", notification.getId(),
                "title", notification.getTitle(),
                "content", Optional.ofNullable(notification.getContent()).orElse(""),
                "jumpType", Optional.ofNullable(notification.getJumpType()).orElse("NONE"),
                "jumpTarget", Optional.ofNullable(notification.getJumpTarget()).orElse(""),
                "bizType", Optional.ofNullable(notification.getBizType()).orElse(""),
                "createdTime", Optional.ofNullable(notification.getCreatedTime()).map(Object::toString).orElse("")
        );

        for (String receiverId : receiverIds) {
            if (sseSessionManager.isConnected(receiverId)) {
                sseSessionManager.sendToManager(receiverId, "notification:new", eventData);
                long unreadCount = notificationReceiverService.getUnreadCountByReceiverId(receiverId);
                sseSessionManager.sendToManager(receiverId, "notification:count", Map.of("unreadCount", unreadCount));
            }
        }
    }

    private void pushCountUpdateForNotification(String notificationId) {
        List<NotificationReceiver> receivers = notificationReceiverService.lambdaQuery()
                .select(NotificationReceiver::getReceiverId)
                .eq(NotificationReceiver::getNotificationId, notificationId)
                .list();

        for (NotificationReceiver r : receivers) {
            String receiverId = r.getReceiverId();
            if (sseSessionManager.isConnected(receiverId)) {
                long unreadCount = notificationReceiverService.getUnreadCountByReceiverId(receiverId);
                sseSessionManager.sendToManager(receiverId, "notification:count", Map.of("unreadCount", unreadCount));
            }
        }
    }

    private Map<String, String> resolveManagerNames(Set<String> managerIds) {
        if (managerIds.isEmpty()) {
            return Map.of();
        }
        return systemManagerService.listByIds(managerIds).stream()
                .collect(Collectors.toMap(SystemManager::getId, SystemManager::getNickname, (a, b) -> a));
    }
}
