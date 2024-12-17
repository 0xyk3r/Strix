package cn.projectan.strix.model.response.system.workflow.task;

import cn.projectan.strix.model.db.WorkflowInstance;
import cn.projectan.strix.model.db.WorkflowTask;
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

    public WorkflowTaskUnfinishedListResp(Collection<WorkflowTask> data, Long total, Collection<WorkflowInstance> instanceData) {
        items = data.stream()
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
                        d.getStartTime(),
                        d.getEndTime()
                ))
                .collect(Collectors.toList());

        items.forEach(item -> {
            instanceData.forEach(instance -> {
                if (item.getInstanceId().equals(instance.getId())) {
                    item.setInstanceName(instance.getName());
                    item.setInstanceCreateBy(instance.getCreateBy());
                    item.setInstanceCreateTime(instance.getCreateTime());
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

        private String instanceCreateBy;

        private LocalDateTime instanceCreateTime;

        private LocalDateTime startTime;

        private LocalDateTime endTime;

    }

}
