package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.NotificationMapper;
import cn.projectan.strix.model.db.system.Notification;
import cn.projectan.strix.model.db.system.NotificationReceiver;
import cn.projectan.strix.model.dict.system.NotificationReadStatus;
import cn.projectan.strix.model.dict.system.NotificationStatus;
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
 * Strix 通知 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2026-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    private final NotificationReceiverService notificationReceiverService;

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

    /**
     * 终止通知
     * 由业务逻辑调用，不暴露为 API 接口
     * 终止后，该通知的所有接收人记录将被标记为失效
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

    /**
     * 终止通知
     * 由业务逻辑调用，不暴露为 API 接口
     * 终止后，该通知的所有接收人记录将被标记为失效
     *
     * @param notificationId 通知 ID
     * @param terminatedBy   终止人 ID
     * @param reason         终止原因
     */
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
