package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.NotificationMapper;
import cn.projectan.strix.model.db.Notification;
import cn.projectan.strix.model.db.NotificationReceiver;
import cn.projectan.strix.model.dict.NotificationReadStatus;
import cn.projectan.strix.model.dict.NotificationStatus;
import cn.projectan.strix.service.NotificationReceiverService;
import cn.projectan.strix.service.NotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2026-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final NotificationReceiverService notificationReceiverService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String sendNotification(String bizType, String bizId, String title, String content,
                                   String jumpType, String jumpTarget, String jumpParams,
                                   String senderId, List<String> receiverIds) {
        Assert.hasText(title, "通知标题不能为空");
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
                .setStatus(NotificationStatus.VALID);

        Assert.isTrue(save(notification), "发送通知失败，请稍后重试");

        // 创建通知接收人记录
        List<NotificationReceiver> receivers = receiverIds.stream()
                .map(receiverId -> new NotificationReceiver()
                        .setNotificationId(notification.getId())
                        .setReceiverId(receiverId)
                        .setReadStatus(NotificationReadStatus.UNREAD)
                        .setValidStatus(NotificationStatus.VALID))
                .collect(Collectors.toList());

        Assert.isTrue(notificationReceiverService.saveBatch(receivers), "创建接收人记录失败，请稍后重试");

        log.info("通知发送成功，通知ID: {}, 业务类型: {}, 接收人数量: {}", notification.getId(), bizType, receiverIds.size());
        return notification.getId();
    }

    @Override
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
                    .set(Notification::getStatus, NotificationStatus.TERMINATED)
                    .set(Notification::getEndBy, terminatedBy)
                    .set(Notification::getEndReason, reason)
                    .update();

            List<String> notificationIds = notifications.stream()
                    .map(Notification::getId)
                    .toList();

            notificationReceiverService.lambdaUpdate()
                    .in(NotificationReceiver::getNotificationId, notificationIds)
                    .set(NotificationReceiver::getValidStatus, NotificationStatus.TERMINATED)
                    .set(NotificationReceiver::getInvalidAt, LocalDateTime.now())
                    .set(NotificationReceiver::getInvalidBy, terminatedBy)
                    .update();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateNotification(String notificationId, String terminatedBy, String reason) {
        Notification notification = getById(notificationId);
        Assert.notNull(notification, "通知不存在");

        // 更新通知状态
        notification.setStatus(NotificationStatus.TERMINATED)
                .setEndBy(terminatedBy)
                .setEndReason(reason);
        Assert.isTrue(updateById(notification), "终止通知失败，请稍后重试");

        // 更新通知接收人记录为无效
        LocalDateTime now = LocalDateTime.now();
        notificationReceiverService.lambdaUpdate()
                .eq(NotificationReceiver::getNotificationId, notificationId)
                .set(NotificationReceiver::getValidStatus, NotificationStatus.TERMINATED)
                .set(NotificationReceiver::getInvalidAt, now)
                .set(NotificationReceiver::getInvalidBy, terminatedBy)
                .update();

        log.info("通知已终止，通知ID: {}, 终止人: {}, 终止原因: {}", notificationId, terminatedBy, reason);
    }

}
