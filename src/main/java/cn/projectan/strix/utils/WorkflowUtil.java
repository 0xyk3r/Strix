package cn.projectan.strix.utils;

import cn.projectan.strix.model.db.*;
import cn.projectan.strix.model.dict.WorkflowInstanceStatus;
import cn.projectan.strix.model.dict.WorkflowStepType;
import cn.projectan.strix.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * @author 安炯奕
 * @date 2023/11/29 15:46
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowUtil {

    private final WorkflowConfigService workflowConfigService;
    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowStepService workflowStepService;
    private final WorkflowStepRouteService workflowStepRouteService;
    private final WorkflowInvokeService workflowInvokeService;
    private final WorkflowParamService workflowParamService;

    private final TransactionTemplate transactionTemplate;

    public void createInstance(String configId) {
        // 获取工作流配置
        WorkflowConfig config = workflowConfigService.getById(configId);
        Assert.notNull(config, "工作流配置不存在");
        // 获取工作流第一步
        WorkflowStep firstStep = workflowStepService.lambdaQuery()
                .eq(WorkflowStep::getWorkflowId, config.getId())
                .eq(WorkflowStep::getType, WorkflowStepType.START)
                .one();

        Assert.notNull(firstStep, "工作流未配置开始步骤");
        // 创建工作流实例
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowId(config.getId());
        instance.setStepId(firstStep.getId());
        instance.setStartTime(LocalDateTime.now());
        instance.setStatus(WorkflowInstanceStatus.ACTIVE);
        // 开启事务
        transactionTemplate.executeWithoutResult(status -> {
            workflowInstanceService.save(instance);
            // 调用开始步骤的进入方法
            if (StringUtils.hasText(firstStep.getEnterInvokeId())) {
                WorkflowInvoke invoke = workflowInvokeService.getById(firstStep.getEnterInvokeId());
                if (invoke != null) {
                    InvokeUtil.invokeMethod(invoke.getFullInvokeStr());
                }
            }
        });
    }

    public void nextStep(String instanceId, Byte nextType) {
        // 获取工作流实例
        WorkflowInstance instance = workflowInstanceService.getById(instanceId);
        Assert.notNull(instance, "工作流实例不存在");
        Assert.isTrue(instance.getStatus().equals(WorkflowInstanceStatus.ACTIVE), "工作流实例状态不正确");
        // 获取当前工作流步骤
        WorkflowStep currentStep = workflowStepService.getById(instance.getStepId());
        Assert.notNull(currentStep, "工作流当前步骤不存在");
        // 开启事务
        transactionTemplate.executeWithoutResult(status -> {
            // 调用当前步骤的离开方法
            invokeByInvokeId(currentStep.getLeaveInvokeId());
            // 获取下一工作流步骤
            WorkflowStepRoute currentStepRoute = workflowStepRouteService.lambdaQuery()
                    .eq(WorkflowStepRoute::getStepId, currentStep.getId())
                    .eq(WorkflowStepRoute::getType, nextType)
                    .one();
            Assert.notNull(currentStepRoute, "工作流当前步骤下一步骤不存在");
            WorkflowStep nextStep = workflowStepService.getById(currentStepRoute.getNextStepId());
            Assert.notNull(nextStep, "工作流下一步骤不存在");
            // 调用下一步骤的进入方法
            invokeByInvokeId(nextStep.getEnterInvokeId());
            // 更新工作流实例
            if (nextStep.getType().equals(WorkflowStepType.END)) {
                // 下一步骤为结束步骤
                instance.setStatus(WorkflowInstanceStatus.DONE);
                instance.setEndTime(LocalDateTime.now());
            } else {
                instance.setStatus(WorkflowInstanceStatus.ACTIVE);
            }
            instance.setStepId(nextStep.getId());
            workflowInstanceService.updateById(instance);
        });
    }

    private void invokeByInvokeId(String invokeId) {
        if (StringUtils.hasText(invokeId)) {
            WorkflowInvoke invoke = workflowInvokeService.getById(invokeId);
            if (invoke != null) {
                InvokeUtil.invokeMethod(invoke.getFullInvokeStr());
            }
        }
    }

}
