package cn.projectan.strix.service;

import cn.projectan.strix.model.db.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2026-01-27
 */
public interface NotificationService extends IService<Notification> {

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
    String sendNotification(String bizType, String bizId, String title, String content,
                            String jumpType, String jumpTarget, String jumpParams,
                            String senderId, List<String> receiverIds);

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
    void terminateNotification(String bizType, String bizId, String terminatedBy, String reason);

    /**
     * 终止通知
     * 由业务逻辑调用，不暴露为 API 接口
     * 终止后，该通知的所有接收人记录将被标记为失效
     *
     * @param notificationId 通知 ID
     * @param terminatedBy   终止人 ID
     * @param reason         终止原因
     */
    void terminateNotification(String notificationId, String terminatedBy, String reason);

}
