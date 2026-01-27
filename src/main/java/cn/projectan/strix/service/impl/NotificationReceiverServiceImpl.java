package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.NotificationMapper;
import cn.projectan.strix.mapper.NotificationReceiverMapper;
import cn.projectan.strix.model.db.Notification;
import cn.projectan.strix.model.db.NotificationReceiver;
import cn.projectan.strix.model.dict.NotificationReadStatus;
import cn.projectan.strix.model.dict.NotificationStatus;
import cn.projectan.strix.model.request.common.notification.ListNotificationReq;
import cn.projectan.strix.model.response.common.notification.NotificationListResp;
import cn.projectan.strix.service.NotificationReceiverService;
import cn.projectan.strix.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
public class NotificationReceiverServiceImpl extends ServiceImpl<NotificationReceiverMapper, NotificationReceiver> implements NotificationReceiverService {

    private final NotificationMapper notificationMapper;

    @Override
    public NotificationListResp getMyNotifications(ListNotificationReq req) {
        String receiverId = SecurityUtils.getOperatorId();
        Assert.hasText(receiverId, "接收人 ID 不能为空");

        Page<NotificationReceiver> page = lambdaQuery()
                .eq(NotificationReceiver::getReceiverId, receiverId)
                .eq(req.getReadStatus() != null, NotificationReceiver::getReadStatus, req.getReadStatus())
                .eq(req.getValidStatus() != null, NotificationReceiver::getValidStatus, req.getValidStatus())
                .orderByDesc(NotificationReceiver::getCreatedTime)
                .page(req.getPage());

        List<NotificationReceiver> receivers = page.getRecords();
        if (CollectionUtils.isEmpty(receivers)) {
            return new NotificationListResp();
        }

        // 查询通知详情
        List<String> notificationIds = receivers.stream()
                .map(NotificationReceiver::getNotificationId)
                .distinct()
                .collect(Collectors.toList());

        List<Notification> notifications = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .in(Notification::getId, notificationIds)
        );

        return new NotificationListResp(receivers, notifications, page.getTotal());
    }

    @Override
    public void markAsRead(String notificationId) {
        String receiverId = SecurityUtils.getOperatorId();
        Assert.hasText(notificationId, "通知 ID 不能为空");
        Assert.hasText(receiverId, "接收人 ID 不能为空");

        // 更新为已读
        boolean success = lambdaUpdate()
                .eq(NotificationReceiver::getNotificationId, notificationId)
                .eq(NotificationReceiver::getReceiverId, receiverId)
                .eq(NotificationReceiver::getReadStatus, NotificationReadStatus.UNREAD)
                .eq(NotificationReceiver::getValidStatus, NotificationStatus.VALID)
                .set(NotificationReceiver::getReadStatus, NotificationReadStatus.READ)
                .set(NotificationReceiver::getReadAt, LocalDateTime.now())
                .update();
    }

    @Override
    public void markAllAsRead() {
        String receiverId = SecurityUtils.getOperatorId();
        Assert.hasText(receiverId, "接收人 ID 不能为空");

        // 更新为已读
        boolean success = lambdaUpdate()
                .eq(NotificationReceiver::getReceiverId, receiverId)
                .eq(NotificationReceiver::getReadStatus, NotificationReadStatus.UNREAD)
                .eq(NotificationReceiver::getValidStatus, NotificationStatus.VALID)
                .set(NotificationReceiver::getReadStatus, NotificationReadStatus.READ)
                .set(NotificationReceiver::getReadAt, LocalDateTime.now())
                .update();
    }

    @Override
    public Long getUnreadCount() {
        String receiverId = SecurityUtils.getOperatorId();
        Assert.hasText(receiverId, "接收人 ID 不能为空");

        return lambdaQuery()
                .eq(NotificationReceiver::getReceiverId, receiverId)
                .eq(NotificationReceiver::getReadStatus, NotificationReadStatus.UNREAD)
                .eq(NotificationReceiver::getValidStatus, NotificationStatus.VALID)
                .count();
    }

}
