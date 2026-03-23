package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.NotificationMapper;
import cn.projectan.strix.mapper.system.NotificationReceiverMapper;
import cn.projectan.strix.model.db.system.Notification;
import cn.projectan.strix.model.db.system.NotificationReceiver;
import cn.projectan.strix.model.dict.common.CommonFlag;
import cn.projectan.strix.model.request.system.notification.ListNotificationReq;
import cn.projectan.strix.model.response.system.notification.NotificationListResp;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.system.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * Strix 通知接收人 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2026-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationReceiverService extends ServiceImpl<NotificationReceiverMapper, NotificationReceiver> {

    private final NotificationMapper notificationMapper;

    /**
     * 获取我的通知列表
     *
     * @param req 查询请求
     * @return 通知列表响应
     */
    public NotificationListResp getMyNotifications(ListNotificationReq req) {
        String receiverId = SecurityUtil.getOperatorId();
        Assert.hasText(receiverId, I18nUtil.notEmpty("field.receiverId"));

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

    /**
     * 标记单个通知为已读
     *
     * @param notificationId 通知ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(String notificationId) {
        String receiverId = SecurityUtil.getOperatorId();
        Assert.hasText(notificationId, I18nUtil.notEmpty("field.notificationId"));
        Assert.hasText(receiverId, I18nUtil.notEmpty("field.receiverId"));

        // 更新为已读
        lambdaUpdate()
                .eq(NotificationReceiver::getNotificationId, notificationId)
                .eq(NotificationReceiver::getReceiverId, receiverId)
                .eq(NotificationReceiver::getReadStatus, CommonFlag.NO)
                .eq(NotificationReceiver::getValidStatus, CommonFlag.YES)
                .set(NotificationReceiver::getReadStatus, CommonFlag.YES)
                .set(NotificationReceiver::getReadAt, LocalDateTime.now())
                .update();
    }

    /**
     * 标记全部通知为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead() {
        String receiverId = SecurityUtil.getOperatorId();
        Assert.hasText(receiverId, I18nUtil.notEmpty("field.receiverId"));

        // 更新为已读
        lambdaUpdate()
                .eq(NotificationReceiver::getReceiverId, receiverId)
                .eq(NotificationReceiver::getReadStatus, CommonFlag.NO)
                .eq(NotificationReceiver::getValidStatus, CommonFlag.YES)
                .set(NotificationReceiver::getReadStatus, CommonFlag.YES)
                .set(NotificationReceiver::getReadAt, LocalDateTime.now())
                .update();
    }

    /**
     * 获取未读通知数量
     *
     * @return 未读数量
     */
    public Long getUnreadCount() {
        String receiverId = SecurityUtil.getOperatorId();
        Assert.hasText(receiverId, I18nUtil.notEmpty("field.receiverId"));

        return lambdaQuery()
                .eq(NotificationReceiver::getReceiverId, receiverId)
                .eq(NotificationReceiver::getReadStatus, CommonFlag.NO)
                .eq(NotificationReceiver::getValidStatus, CommonFlag.YES)
                .count();
    }

}
