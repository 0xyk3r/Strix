package cn.projectan.strix.model.response.system.workflow;

import cn.projectan.strix.model.db.system.WorkflowConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午1:04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工作流配置详情响应")
public class WorkflowConfigResp {

    @Schema(description = "配置ID")
    private String id;

    @Schema(description = "工作流ID")
    private String workflowId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "配置内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    public WorkflowConfigResp(WorkflowConfig data) {
        this.id = data.getId();
        this.workflowId = data.getWorkflowId();
        this.version = data.getVersion();
        this.content = data.getContent();
        this.createdTime = data.getCreatedTime();
    }

}
