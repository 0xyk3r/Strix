package cn.projectan.strix.model.response.system.workflow.task;

import cn.projectan.strix.model.db.WorkflowInstance;
import cn.projectan.strix.model.db.WorkflowTask;
import cn.projectan.strix.model.db.WorkflowTaskAssign;
import cn.projectan.strix.model.response.base.BasePageResp;
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
 * @since 2024-11-13 06:25:07
 */
@Getter
public class WorkflowTaskUnfinishedListResp extends BasePageResp {

    private final List<Item> items;

    public WorkflowTaskUnfinishedListResp(Collection<WorkflowTaskAssign> data, Long total, Collection<WorkflowTask> taskData, Collection<WorkflowInstance> instanceData) {
        items = taskData.stream()
                .map(d -> new Item(
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
                        d.getStartTime()
                ))
                .collect(Collectors.toList());

        items.forEach(item -> {
            data.forEach(d -> {
                if (item.getId().equals(d.getTaskId())) {
                    item.setTaskAssignStartTime(d.getStartTime());
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
    public static class Item {

        private String id;

        private String workflowId;

        private String instanceId;

        private String workflowConfigId;

        private String nodeId;

        private String nodeType;

        private String operatorId;

        private Byte operationType;

        private String instanceName;

        private Short instanceCreatedByType;

        private String instanceCreatedBy;

        private LocalDateTime instanceCreatedTime;

        private LocalDateTime taskAssignStartTime;

    }

}
