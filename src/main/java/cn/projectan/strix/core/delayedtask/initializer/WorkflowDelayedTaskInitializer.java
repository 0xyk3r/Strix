package cn.projectan.strix.core.delayedtask.initializer;

import cn.projectan.strix.core.cache.system.WorkflowConfigCache;
import cn.projectan.strix.core.delayedtask.DelayedTaskManager;
import cn.projectan.strix.core.module.workflow.WorkflowHandler;
import cn.projectan.strix.core.module.workflow.WorkflowTool;
import cn.projectan.strix.model.constant.DelayedTaskConst;
import cn.projectan.strix.model.db.system.WorkflowTask;
import cn.projectan.strix.model.dict.system.WorkflowOperationType;
import cn.projectan.strix.model.dict.system.WorkflowPropsTimeLimitHandler;
import cn.projectan.strix.model.other.system.workflow.WorkflowNode;
import cn.projectan.strix.service.system.WorkflowTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 工作流模块延迟任务初始化器
 * 负责注册工作流任务超时相关的延迟任务消费者
 *
 * @author ProjectAn
 * @since 2024-12-18
 */
@Slf4j
@Order(51)
@Component
@ConditionalOnProperty(prefix = "strix.delayed-task", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class WorkflowDelayedTaskInitializer implements ApplicationRunner {

    private final DelayedTaskManager delayedTaskManager;
    private final WorkflowTaskService workflowTaskService;
    private final WorkflowConfigCache workflowConfigCache;

    /**
     * 工作流任务超时扫描间隔（秒）
     * 工作流超时通常是分钟级别（如10分钟、30分钟），
     * 因此可以设置较长的扫描间隔以降低系统开销
     */
    private static final long SCAN_INTERVAL_SECONDS = 5L;

    @Override
    public void run(ApplicationArguments args) {
        registerWorkflowTaskExpireConsumer();
        log.info("Strix Workflow: 工作流任务超时检查任务初始化完成.");
    }

    /**
     * 注册工作流任务超时消费者
     */
    private void registerWorkflowTaskExpireConsumer() {
        delayedTaskManager.registerConsumer(
                DelayedTaskConst.WORKFLOW_TASK_EXPIRE,
                this::handleWorkflowTimeout,
                SCAN_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * 处理工作流任务超时
     * 根据节点配置的超时处理策略执行相应操作：
     * - NOTIFY: 发送通知
     * - AUTO_PASS: 自动通过
     * - AUTO_REJECT: 自动拒绝
     *
     * @param taskId 工作流任务ID
     */
    private void handleWorkflowTimeout(String taskId) {
        try {
            WorkflowTask task = workflowTaskService.getById(taskId);
            if (task == null) {
                log.warn("Workflow task not found: {}", taskId);
                return;
            }

            List<WorkflowNode> nodes = workflowConfigCache.getConfigById(task.getWorkflowConfigId());
            WorkflowNode currNode = WorkflowTool.findNodeById(nodes, task.getNodeId());
            WorkflowHandler handler = new WorkflowHandler(currNode);
            String timeLimitHandler = handler.getTimeLimitHandler();

            switch (timeLimitHandler) {
                case WorkflowPropsTimeLimitHandler.NOTIFY -> {
                    // TODO: 发送通知
                    log.info("Workflow task timeout notify: {}", taskId);
                }
                case WorkflowPropsTimeLimitHandler.AUTO_PASS -> {
                    workflowTaskService.completeTask(taskId, "TimeLimit", WorkflowOperationType.APPROVED, "超时自动通过");
                    log.info("Workflow task auto passed due to timeout: {}", taskId);
                }
                case WorkflowPropsTimeLimitHandler.AUTO_REJECT -> {
                    workflowTaskService.completeTask(taskId, "TimeLimit", WorkflowOperationType.REJECT, "超时自动拒绝");
                    log.info("Workflow task auto rejected due to timeout: {}", taskId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to handle workflow timeout for task: {}", taskId, e);
        }
    }

}
