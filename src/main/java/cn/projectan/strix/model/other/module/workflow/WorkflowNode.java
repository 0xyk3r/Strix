package cn.projectan.strix.model.other.module.workflow;

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
     */
    private String type;
    /**
     * 节点属性
     */
    private Object props;
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
