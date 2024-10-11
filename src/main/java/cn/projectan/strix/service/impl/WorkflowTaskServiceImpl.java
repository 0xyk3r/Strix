package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.cache.WorkflowConfigCache;
import cn.projectan.strix.core.module.workflow.WorkflowHandler;
import cn.projectan.strix.core.module.workflow.WorkflowTool;
import cn.projectan.strix.mapper.WorkflowTaskMapper;
import cn.projectan.strix.model.db.WorkflowInstance;
import cn.projectan.strix.model.db.WorkflowTask;
import cn.projectan.strix.model.db.WorkflowTaskAssign;
import cn.projectan.strix.model.dict.*;
import cn.projectan.strix.model.other.module.workflow.WorkflowNode;
import cn.projectan.strix.model.other.module.workflow.WorkflowProps;
import cn.projectan.strix.service.WorkflowTaskAssignService;
import cn.projectan.strix.service.WorkflowTaskService;
import cn.projectan.strix.util.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Strix 工作流任务 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-10-09
 */
@Service
@RequiredArgsConstructor
public class WorkflowTaskServiceImpl extends ServiceImpl<WorkflowTaskMapper, WorkflowTask> implements WorkflowTaskService {

    private final WorkflowTaskAssignService workflowTaskAssignService;
    private final WorkflowConfigCache workflowConfigCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTask(WorkflowInstance instance) {
        boolean isRoot = WorkflowNodeType.ROOT.equals(instance.getCurrentNodeType());
        boolean isDone = WorkflowInstanceStatus.DONE == instance.getStatus();
        boolean isAutoComplete = WorkflowNodeType.CONDITIONS.equals(instance.getCurrentNodeType()) ||
                WorkflowNodeType.CC.equals(instance.getCurrentNodeType());
        Byte operationType = isRoot ? Byte.valueOf(WorkflowOperationType.INITIATE) :
                WorkflowNodeType.CC.equals(instance.getCurrentNodeType()) ? Byte.valueOf(WorkflowOperationType.CC) :
                        isAutoComplete ? WorkflowOperationType.AUTO : null;

        WorkflowTask task = new WorkflowTask()
                .setWorkflowId(instance.getWorkflowId())
                .setWorkflowConfigId(instance.getWorkflowConfigId())
                .setWorkflowConfigVersion(instance.getWorkflowConfigVersion())
                .setWorkflowInstanceId(instance.getId())
                .setNodeId(instance.getCurrentNodeId())
                .setNodeType(instance.getCurrentNodeType())
                .setOperatorId(isRoot ? instance.getCreateBy() : null)
                .setOperationType(operationType)
                .setStartTime(isRoot ? instance.getStartTime() : LocalDateTime.now())
                .setEndTime(isDone ? instance.getEndTime() : (isRoot || isAutoComplete ? LocalDateTime.now() : null));
        SpringUtil.getAopProxy(this).save(task);

        if (isRoot || isDone) {
            return;
        }

        List<WorkflowNode> nodes = workflowConfigCache.getConfigById(instance.getWorkflowConfigId());
        WorkflowNode currentNode = WorkflowTool.findNodeById(nodes, instance.getCurrentNodeId());
        WorkflowHandler handler = new WorkflowHandler(currentNode);
        if (WorkflowNodeType.APPROVAL.equals(currentNode.getType()) ||
                WorkflowNodeType.TASK.equals(currentNode.getType()) ||
                WorkflowNodeType.CC.equals(currentNode.getType())
        ) {
            // 创建任务指派
            List<WorkflowTaskAssign> assignList = handler.getAssignList().stream()
                    .map(operatorId ->
                            new WorkflowTaskAssign()
                                    .setWorkflowId(instance.getWorkflowId())
                                    .setInstanceId(instance.getId())
                                    .setTaskId(task.getId())
                                    .setOperatorId(operatorId)
                                    .setOperationType(operationType))
                    .collect(Collectors.toList());
            if (WorkflowPropsAssignMode.SEQ.equals(handler.getAssignMode())) {
                // 顺序审核模式 只创建第一个
                workflowTaskAssignService.save(assignList.getFirst());
            } else {
                workflowTaskAssignService.saveBatch(assignList);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, String operatorId, Byte operationType, String comment) {
        WorkflowTask task = getById(taskId);
        Assert.notNull(task, "任务不存在");
        WorkflowTaskAssign assign = workflowTaskAssignService.lambdaQuery()
                .eq(WorkflowTaskAssign::getTaskId, taskId)
                .eq(WorkflowTaskAssign::getOperatorId, operatorId)
                .isNull(WorkflowTaskAssign::getOperationType)
                .one();
        Assert.notNull(assign, "任务不存在");

        List<WorkflowNode> nodes = workflowConfigCache.getConfigById(task.getWorkflowConfigId());
        Assert.notEmpty(nodes, "工作流配置为空");
        WorkflowNode currentNode = WorkflowTool.findNodeById(nodes, task.getNodeId());
        Assert.notNull(currentNode, "工作流配置当前节点不存在");
        WorkflowHandler handler = new WorkflowHandler(currentNode);
        Assert.isTrue(handler.isAssignOperator(operatorId), "任务不存在");

        assign.setOperationType(operationType);
        assign.setComment(comment);
        workflowTaskAssignService.updateById(assign);

        WorkflowInstanceServiceImpl workflowInstanceService = SpringUtil.getBean(WorkflowInstanceServiceImpl.class);
        WorkflowInstance instance = workflowInstanceService.getById(task.getWorkflowInstanceId());

        boolean isFinish = false;
        switch (operationType) {
            case WorkflowOperationType.APPROVED -> {
                String assignMode = handler.getAssignMode();
                switch (handler.getAssignMode()) {
                    // 顺序审核模式
                    case WorkflowPropsAssignMode.SEQ -> {
                        List<String> assignList = handler.getAssignList();
                        int index = assignList.indexOf(operatorId);
                        if (index == assignList.size() - 1) {
                            // 最后一个操作人员
                            isFinish = true;
                            workflowInstanceService.toNext(instance);
                        } else {
                            String nextOperatorId = assignList.get(index + 1);
                            workflowTaskAssignService.save(
                                    new WorkflowTaskAssign()
                                            .setWorkflowId(assign.getWorkflowId())
                                            .setInstanceId(assign.getInstanceId())
                                            .setTaskId(assign.getTaskId())
                                            .setOperatorId(nextOperatorId)
                            );
                        }
                    }
                    // 并行审核模式
                    case WorkflowPropsAssignMode.ALL -> {
                        boolean isAllDone = workflowTaskAssignService.lambdaQuery()
                                .eq(WorkflowTaskAssign::getTaskId, taskId)
                                .isNull(WorkflowTaskAssign::getOperationType)
                                .count() == 0;
                        if (isAllDone) {
                            isFinish = true;
                            workflowInstanceService.toNext(instance);
                        }
                    }
                    // 任一审核模式
                    case WorkflowPropsAssignMode.ANY -> {
                        isFinish = true;
                        workflowInstanceService.toNext(instance);
                    }
                }
            }
            case WorkflowOperationType.REJECT -> {
                WorkflowProps.Reject rejectConfig = handler.getRejectConfig();
                switch (rejectConfig.getType()) {
                    case WorkflowPropsRejectType.END -> {
                        // 结束流程
                        isFinish = true;
                        workflowInstanceService.lambdaUpdate()
                                .set(WorkflowInstance::getStatus, WorkflowInstanceStatus.CANCEL)
                                .set(WorkflowInstance::getEndTime, LocalDateTime.now())
                                .eq(WorkflowInstance::getId, task.getWorkflowInstanceId())
                                .update();
                    }
                    case WorkflowPropsRejectType.NODE -> {
                        // 返回指定节点
                        isFinish = true;
                        workflowInstanceService.toNode(instance, rejectConfig.getNodeId(), true);
                    }
                }
            }
            default -> throw new IllegalArgumentException("不支持的操作");
        }

        // 任务完成
        if (isFinish) {
            task.setOperatorId(operatorId);
            task.setOperationType(operationType);
            task.setEndTime(LocalDateTime.now());
            SpringUtil.getAopProxy(this).updateById(task);
        }

    }

}
