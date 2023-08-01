package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueDetection;
import cn.projectan.strix.model.db.base.BaseModel;
import cn.projectan.strix.model.dict.CommonSwitch;
import cn.projectan.strix.model.dict.JobMisfire;
import cn.projectan.strix.model.dict.JobStatus;
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
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2023-07-30
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_job")
public class Job extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    @UniqueDetection("任务名称")
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
     * @see JobMisfire
     */
    private Integer misfirePolicy;

    /**
     * 是否并发执行
     *
     * @see CommonSwitch
     */
    @TableField("`concurrent`")
    private Integer concurrent;

    /**
     * 状态
     *
     * @see JobStatus
     */
    @TableField("`status`")
    private Integer status;

    public Job(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public Job(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
