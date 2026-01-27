package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author ProjectAn
 * @since 2026-01-27
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_notification_receiver")
public class NotificationReceiver extends BaseModel<NotificationReceiver> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知 ID
     */
    private String notificationId;

    /**
     * 接收人 ID
     */
    private String receiverId;

    /**
     * 是否已读 (0未读 1已读)
     */
    private Short readStatus;

    /**
     * 已读时间
     */
    private LocalDateTime readAt;

    /**
     * 是否有效 (1有效 2失效)
     */
    private Short validStatus;

    /**
     * 失效时间
     */
    private LocalDateTime invalidAt;

    /**
     * 导致失效的处理人 ID
     */
    private String invalidBy;

}
