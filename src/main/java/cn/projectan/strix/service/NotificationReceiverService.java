package cn.projectan.strix.service;

import cn.projectan.strix.model.db.NotificationReceiver;
import cn.projectan.strix.model.request.common.notification.ListNotificationReq;
import cn.projectan.strix.model.response.common.notification.NotificationListResp;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2026-01-27
 */
public interface NotificationReceiverService extends IService<NotificationReceiver> {

    /**
     * 获取我的通知列表
     *
     * @param req 查询请求
     * @return 通知列表响应
     */
    NotificationListResp getMyNotifications(ListNotificationReq req);

    /**
     * 标记单个通知为已读
     *
     * @param notificationId 通知ID
     */
    void markAsRead(String notificationId);

    /**
     * 标记全部通知为已读
     */
    void markAllAsRead();

    /**
     * 获取未读通知数量
     *
     * @return 未读数量
     */
    Long getUnreadCount();

}
