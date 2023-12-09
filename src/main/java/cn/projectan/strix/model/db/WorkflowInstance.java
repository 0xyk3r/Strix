package cn.projectan.strix.model.db;

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
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2023-11-29
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_workflow_instance")
public class WorkflowInstance extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 步骤ID
     */
    private String stepId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态	1-进行中	2-已完成	3-已取消
     */
    @TableField("`status`")
    private Byte status;

    public WorkflowInstance(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public WorkflowInstance(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
