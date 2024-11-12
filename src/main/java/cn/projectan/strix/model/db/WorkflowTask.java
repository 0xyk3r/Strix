package cn.projectan.strix.model.db;

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
 * Strix 工作流任务
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
@TableName("sys_workflow_task")
public class WorkflowTask extends BaseModel<WorkflowTask> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 工作流配置ID
     */
    private String workflowConfigId;

    /**
     * 工作流实例ID
     */
    private String workflowInstanceId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作类型
     */
    private Byte operationType;

    /**
     * 任务开始时间
     */
    private LocalDateTime startTime;

    /**
     * 任务结束时间
     */
    private LocalDateTime endTime;

}
