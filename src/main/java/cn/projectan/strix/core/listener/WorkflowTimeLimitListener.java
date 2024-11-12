package cn.projectan.strix.core.listener;

import cn.projectan.strix.core.cache.WorkflowConfigCache;
import cn.projectan.strix.core.module.workflow.WorkflowHandler;
import cn.projectan.strix.core.module.workflow.WorkflowTool;
import cn.projectan.strix.model.constant.DelayedQueueConst;
import cn.projectan.strix.model.db.WorkflowTask;
import cn.projectan.strix.model.dict.WorkflowPropsTimeLimitHandler;
import cn.projectan.strix.model.other.system.workflow.WorkflowNode;
import cn.projectan.strix.service.WorkflowTaskService;
import cn.projectan.strix.util.DelayedQueueUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ProjectAn
 * @since 2024-10-15 15:07:10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTimeLimitListener {

    private final WorkflowTaskService workflowTaskService;
    private final WorkflowConfigCache workflowConfigCache;
    private final DelayedQueueUtil delayedQueueUtil;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void listener() {
        executor.execute(() -> {
            RBlockingDeque<String> queue = delayedQueueUtil.getQueue(DelayedQueueConst.WORKFLOW_TASK_EXPIRE);
            while (true) {
                try {
                    String taskId = queue.take();
                    WorkflowTask task = workflowTaskService.getById(taskId);
                    List<WorkflowNode> nodes = workflowConfigCache.getConfigById(task.getWorkflowConfigId());
                    WorkflowNode currNode = WorkflowTool.findNodeById(nodes, task.getNodeId());
                    WorkflowHandler handler = new WorkflowHandler(currNode);
                    String timeLimitHandler = handler.getTimeLimitHandler();
                    switch (timeLimitHandler) {
                        case WorkflowPropsTimeLimitHandler.NOTIFY -> {
                            // todo
                        }
                        case WorkflowPropsTimeLimitHandler.AUTO_PASS -> {
                            workflowTaskService.completeTask(taskId, "TimeLimit", (byte) 2, "超时自动通过");
                        }
                        case WorkflowPropsTimeLimitHandler.AUTO_REJECT -> {
                            workflowTaskService.completeTask(taskId, "TimeLimit", (byte) 3, "超时自动拒绝");
                        }
                    }
                } catch (InterruptedException e) {
                    log.error("订单超时处理监听器异常", e);
                }
            }
        });
    }

    @PreDestroy
    public void destroy() {
        executor.shutdown();
    }

}
