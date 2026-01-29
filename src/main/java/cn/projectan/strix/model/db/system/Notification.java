package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

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
@TableName("sys_notification")
public class Notification extends BaseModel<Notification> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务主键 ID
     */
    private String bizId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 跳转类型 (PAGE / URL / NONE)
     *
     * @see cn.projectan.strix.model.dict.system.NotificationJumpType
     */
    private String jumpType;

    /**
     * 跳转目标 (路由或 URL)
     */
    private String jumpTarget;

    /**
     * 跳转参数 (JSON)
     */
    private String jumpParams;

    /**
     * 发送人 ID (系统通知为空)
     */
    private String senderId;

    /**
     * 通知状态 (0无效 1有效)
     *
     * @see cn.projectan.strix.model.dict.common.CommonFlag
     */
    @TableField("`status`")
    private Short status;

    /**
     * 终止原因
     */
    private String endReason;

    /**
     * 终止人 ID
     */
    private String endBy;

}
