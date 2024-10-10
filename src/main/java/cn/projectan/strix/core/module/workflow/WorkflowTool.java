package cn.projectan.strix.core.module.workflow;

import cn.projectan.strix.model.dict.WorkflowNodeType;
import cn.projectan.strix.model.other.module.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Strix 工作流 工具类
 *
 * @author ProjectAn
 * @since 2024-10-07 03:12:05
 */
@Slf4j
public class WorkflowTool {

    /**
     * 查找根节点
     *
     * @param nodes 节点列表
     * @return 根节点
     */
    public static WorkflowNode findRootNode(List<WorkflowNode> nodes) {
        return nodes.stream()
                .filter(node -> WorkflowNodeType.ROOT.equals(node.getType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据节点ID查找节点
     *
     * @param nodes 节点列表
     * @param id    节点ID
     * @return 节点
     */
    public static WorkflowNode findNodeById(List<WorkflowNode> nodes, String id) {
        return nodes.stream()
                .filter(node -> node.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找下一个节点
     *
     * @param nodes 节点列表
     * @param id    节点ID
     * @return 下一个节点
     */
    public static WorkflowNode findNextNode(List<WorkflowNode> nodes, String id) {
        return nodes.stream()
                .filter(node -> id.equals(node.getParentId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找上一个节点
     *
     * @param nodes 节点列表
     * @param id    节点ID
     * @return 上一个节点
     */
    public static WorkflowNode findPrevNode(List<WorkflowNode> nodes, String id) {
        return nodes.stream()
                .filter(node -> id.equals(node.getId()))
                .findFirst()
                .map(WorkflowNode::getParentId)
                .map(parentId -> findNodeById(nodes, parentId))
                .orElse(null);
    }

}
