package cn.projectan.strix.model.response.system.workflow;

import cn.projectan.strix.model.db.system.Workflow;
import cn.projectan.strix.model.db.system.WorkflowConfig;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午12:59
 */
@Getter
@Schema(description = "工作流列表响应")
public class WorkflowListResp extends BasePageResp {

    @Schema(description = "工作流列表项")
    private final List<WorkflowItem> items;

    public WorkflowListResp(List<Workflow> data, Long total, List<WorkflowConfig> extraData) {
        items = data.stream()
                .map(d -> new WorkflowItem(
                        d.getId(),
                        d.getName(),
                        extraData.stream()
                                .filter(e -> e.getWorkflowId().equals(d.getId()))
                                .sorted((a, b) -> b.getVersion().compareTo(a.getVersion()))
                                .map(WorkflowConfigResp::new)
                                .collect(Collectors.toList()),
                        d.getCreatedTime()
                ))
                .collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "工作流列表项")
    public static class WorkflowItem {

        @Schema(description = "工作流ID")
        private String id;

        @Schema(description = "工作流名称")
        private String name;

        @Schema(description = "工作流配置列表")
        private List<WorkflowConfigResp> configs;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }

}
