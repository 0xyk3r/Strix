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
import java.time.LocalDateTime;

/**
 * 系统公告
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_system_announcement")
public class SystemAnnouncement extends BaseModel<SystemAnnouncement> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 级别: INFO / WARNING / URGENT
     */
    @TableField("`level`")
    private String level;

    /**
     * 展示方式: BANNER / MODAL
     */
    private String displayType;

    /**
     * 公告状态 (0=已终止, 1=有效)
     */
    @TableField("`status`")
    private Short status;

    /**
     * 生效时间 (null = 立即生效)
     */
    private LocalDateTime startTime;

    /**
     * 失效时间 (null = 不自动失效)
     */
    private LocalDateTime endTime;

    /**
     * 终止人 ID
     */
    private String endBy;

    /**
     * 终止原因
     */
    private String endReason;
}
