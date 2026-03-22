package cn.projectan.strix.core.module.workflow;

import cn.projectan.strix.model.dict.system.WorkflowPropsAssignType;
import cn.projectan.strix.model.dict.system.WorkflowPropsTimeLimitUnit;
import cn.projectan.strix.model.other.system.workflow.WorkflowNode;
import cn.projectan.strix.model.other.system.workflow.WorkflowProps;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.util.common.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

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

    public Long getTimeLimitMinute() {
        WorkflowProps.TimeLimit timeLimit = node.getProps().getTimeLimit();
        if (timeLimit == null) {
            log.warn("节点[{}]不支持获取超时时限", node.getId());
            return null;
        }
        return convertTimeLimitUnit(timeLimit.getValue(), timeLimit.getUnit());
    }

    public String getTimeLimitHandler() {
        WorkflowProps.TimeLimit timeLimit = node.getProps().getTimeLimit();
        if (timeLimit == null) {
            log.warn("节点[{}]不支持获取超时处理方式", node.getId());
            return null;
        }
        return timeLimit.getHandler();
    }

    /**
     * 获取拒绝后操作配置
     */
    public WorkflowProps.Reject getRejectConfig() {
        WorkflowProps.Reject reject = node.getProps().getReject();
        if (reject == null) {
            log.warn("节点[{}]不是审批节点, 不支持获取审批拒绝后操作", node.getId());
        }
        return reject;
    }

    /**
     * 获取任务审批人类型
     */
    public String getAssignType() {
        WorkflowProps.Assign assign = node.getProps().getAssign();
        if (assign == null) {
            log.warn("节点[{}]不支持获取审批人类型", node.getId());
            return null;
        }
        return assign.getType();
    }

    /**
     * 获取任务指派模式
     */
    public String getAssignMode() {
        WorkflowProps.Assign assign = node.getProps().getAssign();
        if (assign == null) {
            log.warn("节点[{}]不支持获取审批人模式", node.getId());
            return null;
        }
        return assign.getMode();
    }

    /**
     * 判断是否为任务分配的操作人员
     *
     * @param operatorId 操作人员ID
     */
    public boolean isAssignOperator(String operatorId) {
        if ("TimeLimit".equals(operatorId)) {
            return true;
        }
        List<String> assignList = getAssignList();
        return assignList != null && assignList.contains(operatorId);
    }

    /**
     * 获取任务指派的人员ID列表
     */
    public List<String> getAssignList() {
        WorkflowProps.Assign assign = node.getProps().getAssign();
        if (assign == null) {
            log.warn("节点[{}]不支持获取审批人列表", node.getId());
            return null;
        }
        return resolveAssignList(assign);
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
        log.warn("节点[{}]无可用条件分支配置", node.getId());
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
            return systemManagerService.getManagerIdListByRoleIds(assign.getId());
        }
        // TODO: 将来通过动态参数 {nodeId}:{assign} 获取
        // WorkflowPropsAssignType.SELECT
        // WorkflowPropsAssignType.SELF
        return List.of();
    }

    private Long convertTimeLimitUnit(Long timeLimit, String unit) {
        // 为 0 不生效
        if (timeLimit == null || unit == null || timeLimit <= 0) {
            return null;
        }
        return switch (unit) {
            case WorkflowPropsTimeLimitUnit.MINUTE -> timeLimit;
            case WorkflowPropsTimeLimitUnit.HOUR -> timeLimit * 60;
            case WorkflowPropsTimeLimitUnit.DAY -> timeLimit * 60 * 24;
            default -> null;
        };
    }

}
