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
 * Strix 工作流实例
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_workflow_instance")
public class WorkflowInstance extends BaseModel<WorkflowInstance> {

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
     * 工作流配置版本
     */
    private Integer workflowConfigVersion;

    /**
     * 当前节点ID
     */
    private String currentNodeId;

    /**
     * 当前节点类型
     */
    private String currentNodeType;

    /**
     * 当前操作人ID
     */
    private String currentOperatorId;

    /**
     * 流程开始时间
     */
    private LocalDateTime startTime;

    /**
     * 流程结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态	1-进行中	2-已完成	3-已取消
     */
    @TableField("`status`")
    private Byte status;

}
