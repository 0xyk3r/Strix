package cn.projectan.strix.core.module.workflow;

import cn.projectan.strix.model.dict.WorkflowPropsAssignType;
import cn.projectan.strix.model.other.module.workflow.WorkflowNode;
import cn.projectan.strix.model.other.module.workflow.WorkflowProps;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Strix 工作流处理器
 *
 * @author ProjectAn
 * @since 2024-10-09 12:57:35
 */
@Slf4j
public class WorkflowHandler {

    private final WorkflowNode node;

    public WorkflowHandler(WorkflowNode node) {
        this.node = node;
    }

    /**
     * 获取拒绝后操作配置
     */
    public WorkflowProps.Reject getRejectConfig() {
        if (node.getProps() instanceof WorkflowProps.ApprovalWorkflowProps approvalProps) {
            return approvalProps.getReject();
        }
        return null;
    }

    /**
     * 获取任务指派模式
     */
    public String getAssignMode() {
        if (node.getProps() instanceof WorkflowProps.ApprovalWorkflowProps approvalProps) {
            return approvalProps.getAssign().getMode();
        } else if (node.getProps() instanceof WorkflowProps.TaskWorkflowProps taskProps) {
            return taskProps.getAssign().getMode();
        } else if (node.getProps() instanceof WorkflowProps.CcWorkflowProps ccProps) {
            return ccProps.getAssign().getMode();
        }
        return null;
    }

    /**
     * 判断是否为任务分配的操作人员
     *
     * @param operatorId 操作人员ID
     */
    public boolean isAssignOperator(String operatorId) {
        List<String> assignList = getAssignList();
        return assignList != null && assignList.contains(operatorId);
    }

    /**
     * 获取任务指派的人员ID列表
     */
    public List<String> getAssignList() {
        if (node.getProps() instanceof WorkflowProps.ApprovalWorkflowProps approvalProps) {
            return resolveAssignList(approvalProps.getAssign());
        } else if (node.getProps() instanceof WorkflowProps.TaskWorkflowProps taskProps) {
            return resolveAssignList(taskProps.getAssign());
        } else if (node.getProps() instanceof WorkflowProps.CcWorkflowProps ccProps) {
            return resolveAssignList(ccProps.getAssign());
        }
        return null;
    }

    /**
     * 解析任务指派配置
     */
    private List<String> resolveAssignList(WorkflowProps.Assign assign) {
        if (WorkflowPropsAssignType.USER.equals(assign.getType())) {
            return assign.getId();
        } else if (WorkflowPropsAssignType.ROLE.equals(assign.getType())) {
            SystemManagerService systemManagerService = SpringUtil.getBean(SystemManagerService.class);
            return assign.getId().stream()
                    .map(systemManagerService::getManagerIdListByRoleId)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
//            TODO: 将来通过动态参数 {nodeId}:{assign} 获取
//            WorkflowPropsAssignType.SELECT
//            WorkflowPropsAssignType.SELF
        return List.of();
    }

    /**
     * 处理条件分支, 返回目标节点ID
     */
    public String getConditionsBranchNodeId() {
        if (CollectionUtils.isEmpty(node.getBranches())) {
            return null;
        }
        for (WorkflowNode branch : node.getBranches()) {
            if (branch.getProps() instanceof WorkflowProps.ConditionWorkflowProps props) {
                // TODO 判断条件
                return branch.getId();
            }
        }
        return null;
    }

}
