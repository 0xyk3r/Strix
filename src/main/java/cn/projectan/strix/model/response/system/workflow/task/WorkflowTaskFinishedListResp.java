package cn.projectan.strix.model.response.system.workflow.task;

import cn.projectan.strix.model.db.system.WorkflowInstance;
import cn.projectan.strix.model.db.system.WorkflowTask;
import cn.projectan.strix.model.db.system.WorkflowTaskAssign;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2024-12-18 16:07:06
 */
@Getter
@Schema(description = "工作流已办任务列表响应")
public class WorkflowTaskFinishedListResp extends BasePageResp {

    @Schema(description = "已办任务列表项")
    private final List<FinishedTaskItem> items;

    public WorkflowTaskFinishedListResp(Collection<WorkflowTaskAssign> data, Long total, Collection<WorkflowTask> taskData, Collection<WorkflowInstance> instanceData) {
        items = taskData.stream()
                .map(d -> new FinishedTaskItem(
                        d.getId(),
                        d.getWorkflowId(),
                        d.getWorkflowInstanceId(),
                        d.getWorkflowConfigId(),
                        d.getNodeId(),
                        d.getNodeType(),
                        d.getOperatorId(),
                        d.getOperationType(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        d.getStartTime(),
                        d.getEndTime()
                ))
                .collect(Collectors.toList());

        items.forEach(item -> {
            data.forEach(d -> {
                if (item.getId().equals(d.getTaskId())) {
                    item.setTaskAssignStartTime(d.getStartTime());
                    item.setTaskAssignEndTime(d.getEndTime());
                }
            });
            instanceData.forEach(instance -> {
                if (item.getInstanceId().equals(instance.getId())) {
                    item.setInstanceName(instance.getName());
                    item.setInstanceCreatedByType(instance.getCreatedByType());
                    item.setInstanceCreatedBy(instance.getCreatedBy());
                    item.setInstanceCreatedTime(instance.getCreatedTime());
                }
            });
        });

        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "已办任务项")
    public static class FinishedTaskItem {

        @Schema(description = "任务ID")
        private String id;

        @Schema(description = "工作流ID")
        private String workflowId;

        @Schema(description = "工作流实例ID")
        private String instanceId;

        @Schema(description = "工作流配置ID")
        private String workflowConfigId;

        @Schema(description = "节点ID")
        private String nodeId;

        @Schema(description = "节点类型")
        private String nodeType;

        @Schema(description = "操作人ID")
        private String operatorId;

        @Schema(description = "操作类型")
        private Short operationType;

        @Schema(description = "实例名称")
        private String instanceName;

        @Schema(description = "实例创建人类型")
        private Short instanceCreatedByType;

        @Schema(description = "实例创建人")
        private String instanceCreatedBy;

        @Schema(description = "实例创建时间")
        private LocalDateTime instanceCreatedTime;

        @Schema(description = "任务分配开始时间")
        private LocalDateTime taskAssignStartTime;

        @Schema(description = "任务分配结束时间")
        private LocalDateTime taskAssignEndTime;

        @Schema(description = "任务开始时间")
        private LocalDateTime startTime;

        @Schema(description = "任务结束时间")
        private LocalDateTime endTime;

    }

}
