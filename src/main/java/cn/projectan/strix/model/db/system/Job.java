package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.annotation.UniqueField;
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
 * Strix 定时任务
 * </p>
 *
 * @author ProjectAn
 * @since 2023-07-30
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_job")
public class Job extends BaseModel<Job> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    @UniqueField("任务名称")
    @TableField("`name`")
    private String name;

    /**
     * 任务组名
     */
    @TableField("`group`")
    private String group;

    /**
     * 调用目标字符串
     */
    private String invokeTarget;

    /**
     * cron执行表达式
     */
    private String cronExpression;

    /**
     * 计划执行错误策略
     *
     * @see cn.projectan.strix.model.dict.system.JobMisfire
     */
    private Short misfirePolicy;

    /**
     * 是否并发执行
     *
     * @see cn.projectan.strix.model.dict.common.CommonSwitch
     */
    @TableField("`concurrent`")
    private Short concurrent;

    /**
     * 状态
     *
     * @see cn.projectan.strix.model.dict.system.JobStatus
     */
    @TableField("`status`")
    private Short status;

}
