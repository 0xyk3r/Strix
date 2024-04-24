package cn.projectan.strix.model.response.module.workflow;

import cn.projectan.strix.model.db.Workflow;
import cn.projectan.strix.model.db.WorkflowConfig;
import cn.projectan.strix.model.response.base.BasePageResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @date 2024/4/24 下午12:59
 */
@Getter
public class WorkflowListResp extends BasePageResp {

    private final List<WorkflowItem> items;

    public WorkflowListResp(List<Workflow> data, Long total, List<WorkflowConfig> extraData) {
        items = data.stream()
                .map(d -> new WorkflowItem(
                        d.getId(),
                        d.getName(),
                        extraData.stream()
                                .filter(e -> e.getWorkflowId().equals(d.getId()))
                                .map(WorkflowConfigResp::new)
                                .collect(Collectors.toList()),
                        d.getCreateTime()
                ))
                .collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowItem {

        private String id;

        private String name;

        private List<WorkflowConfigResp> configs;

        private LocalDateTime createTime;

    }

}
