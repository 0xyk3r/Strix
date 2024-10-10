package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix 工作流任务分配
 * </p>
 *
 * @author ProjectAn
 * @since 2024-10-09
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_workflow_task_assign")
public class WorkflowTaskAssign extends BaseModel<WorkflowTaskAssign> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 工作流实例ID
     */
    private String instanceId;

    /**
     * 工作流任务ID
     */
    private String taskId;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作类型
     */
    private Byte operationType;

    /**
     * 附加留言
     */
    private String comment;

}
