package cn.projectan.strix.model.response.system.workflow;

import cn.projectan.strix.model.db.WorkflowConfig;
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
public class WorkflowConfigResp {

    private String id;

    private String workflowId;

    private Integer version;

    private String content;

    private LocalDateTime createdTime;

    public WorkflowConfigResp(WorkflowConfig data) {
        this.id = data.getId();
        this.workflowId = data.getWorkflowId();
        this.version = data.getVersion();
        this.content = data.getContent();
        this.createdTime = data.getCreatedTime();
    }

}
