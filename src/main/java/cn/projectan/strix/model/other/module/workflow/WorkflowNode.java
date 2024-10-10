package cn.projectan.strix.model.other.module.workflow;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.util.List;

/**
 * @author ProjectAn
 * @date 2024/9/24 05:11
 */
@Data
public class WorkflowNode {

    /**
     * 节点ID
     */
    private String id;
    /**
     * 节点名称
     */
    private String name;
    /**
     * 节点描述
     */
    private String desc;
    /**
     * 节点类型
     *
     * @see cn.projectan.strix.model.dict.WorkflowNodeType
     */
    private String type;
    /**
     * 节点属性
     *
     * @see WorkflowProps
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", defaultImpl = WorkflowProps.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = WorkflowProps.ApprovalWorkflowProps.class, name = "approval"),
            @JsonSubTypes.Type(value = WorkflowProps.TaskWorkflowProps.class, name = "task"),
            @JsonSubTypes.Type(value = WorkflowProps.CcWorkflowProps.class, name = "cc"),
            @JsonSubTypes.Type(value = WorkflowProps.ConditionWorkflowProps.class, name = "condition"),
    })
    private WorkflowProps props;
    /**
     * 父节点ID
     */
    private String parentId;
    /**
     * 父节点类型
     */
    private String parentType;
    /**
     * 所属条件分支节点ID
     */
    private String conditionsId;
    /**
     * 条件分支
     */
    private List<WorkflowNode> branches;

}
