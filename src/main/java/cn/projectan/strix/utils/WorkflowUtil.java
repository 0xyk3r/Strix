package cn.projectan.strix.utils;

import cn.projectan.strix.model.db.*;
import cn.projectan.strix.model.dict.WorkflowInstanceStatus;
import cn.projectan.strix.model.dict.WorkflowStepOperatorType;
import cn.projectan.strix.model.dict.WorkflowStepType;
import cn.projectan.strix.service.*;
import cn.projectan.strix.utils.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

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

    /**
     * 创建工作流实例
     *
     * @param configId 工作流配置ID
     */
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

    /**
     * 进入下一步骤
     *
     * @param instanceId 工作流实例ID
     * @param nextType   下一步骤类型
     */
    public void nextStep(String instanceId, Byte nextType) {
        // 获取工作流实例
        WorkflowInstance instance = workflowInstanceService.getById(instanceId);
        Assert.notNull(instance, "工作流实例不存在");
        Assert.isTrue(instance.getStatus().equals(WorkflowInstanceStatus.ACTIVE), "工作流实例状态不正确");
        // 获取当前工作流步骤
        WorkflowStep currentStep = workflowStepService.getById(instance.getStepId());
        Assert.notNull(currentStep, "工作流当前步骤不存在");
        // 获取下一工作流步骤路由
        WorkflowStepRoute currentStepRoute = workflowStepRouteService.lambdaQuery()
                .eq(WorkflowStepRoute::getStepId, currentStep.getId())
                .eq(WorkflowStepRoute::getType, nextType)
                .one();
        Assert.notNull(currentStepRoute, "工作流当前步骤下一步骤路由不存在");
        // 检查操作人权限
        boolean operatorPermissionCheckResult = checkOperatorPermission(instanceId, currentStepRoute.getOperatorType(), currentStepRoute.getOperatorCheckInvokeId());
        Assert.isTrue(operatorPermissionCheckResult, "当前操作人无权限");
        // 开启事务
        transactionTemplate.executeWithoutResult(status -> {
            // 调用当前步骤的离开方法
            invokeByInvokeId(instanceId, currentStep.getLeaveInvokeId());
            WorkflowStep nextStep = workflowStepService.getById(currentStepRoute.getNextStepId());
            Assert.notNull(nextStep, "工作流下一步骤不存在");
            // 调用下一步骤的进入方法
            invokeByInvokeId(instanceId, nextStep.getEnterInvokeId());
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

    /**
     * 获取工作流实例参数
     *
     * @param instanceId 工作流实例ID
     * @param paramName  参数名
     * @return 参数值
     */
    public String getParam(String instanceId, String paramName) {
        WorkflowParam param = workflowParamService.lambdaQuery()
                .eq(WorkflowParam::getWorkflowInstanceId, instanceId)
                .eq(WorkflowParam::getName, paramName)
                .one();
        Assert.notNull(param, "工作流实例或参数不存在");
        return param.getValue();
    }

    /**
     * 设置工作流实例参数
     *
     * @param instanceId 工作流实例ID
     * @param paramName  参数名
     * @param paramValue 参数值
     */
    public void setParam(String instanceId, String paramName, String paramValue) {
        WorkflowParam param = workflowParamService.lambdaQuery()
                .eq(WorkflowParam::getWorkflowInstanceId, instanceId)
                .eq(WorkflowParam::getName, paramName)
                .one();
        if (param == null) {
            WorkflowInstance instance = workflowInstanceService.getById(instanceId);
            Assert.notNull(instance, "工作流实例不存在");
            param = new WorkflowParam();
            param.setWorkflowId(instance.getWorkflowId());
            param.setWorkflowInstanceId(instanceId);
            param.setName(paramName);
            param.setValue(paramValue);
            workflowParamService.save(param);
        } else {
            param.setValue(paramValue);
            workflowParamService.updateById(param);
        }
    }

    /**
     * 检查操作人权限
     *
     * @param instanceId            工作流实例ID
     * @param operatorType          操作人类型
     * @param operatorCheckInvokeId 操作人检查方法ID
     * @return true是 false否
     */
    private boolean checkOperatorPermission(String instanceId, Byte operatorType, String operatorCheckInvokeId) {
        // 检查操作人类型
        if (operatorType != null) {
            if (!WorkflowStepOperatorType.valid(operatorType)) {
                return false;
            }
            if (WorkflowStepOperatorType.MANAGER == operatorType && !SecurityUtils.isSystemManager()) {
                return false;
            }
            if (WorkflowStepOperatorType.USER == operatorType && !SecurityUtils.isSystemUser()) {
                return false;
            }
        }
        // 调用检查权限方法
        if (StringUtils.hasText(operatorCheckInvokeId)) {
            Boolean checkResult = invokeByInvokeId(instanceId, operatorCheckInvokeId, Boolean.class);
            return Optional.ofNullable(checkResult).orElse(false);
        }
        return true;
    }

    /**
     * 调用方法
     *
     * @param instanceId 工作流实例ID
     * @param invokeId   调用ID
     */
    private void invokeByInvokeId(String instanceId, String invokeId) {
        ContextHolder.set("STRIX_WORKFLOW_INSTANCE_ID", instanceId);
        if (StringUtils.hasText(invokeId)) {
            WorkflowInvoke invoke = workflowInvokeService.getById(invokeId);
            if (invoke != null) {
                InvokeUtil.invokeMethod(invoke.getFullInvokeStr());
            }
        }
    }

    /**
     * 调用方法
     *
     * @param instanceId 工作流实例ID
     * @param invokeId   调用ID
     * @param returnType 返回值类型
     * @param <T>        返回值类型
     * @return 返回值
     */
    private <T> T invokeByInvokeId(String instanceId, String invokeId, Class<T> returnType) {
        ContextHolder.set("STRIX_WORKFLOW_INSTANCE_ID", instanceId);
        if (StringUtils.hasText(invokeId)) {
            WorkflowInvoke invoke = workflowInvokeService.getById(invokeId);
            if (invoke != null) {
                try {
                    Object result = InvokeUtil.invokeMethodWithReturn(invoke.getFullInvokeStr());
                    return returnType.cast(result);
                } catch (Exception e) {
                    log.error("调用方法失败", e);
                }
            }
        }
        return null;
    }

}
