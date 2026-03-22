package cn.projectan.strix.model.request.system.workflow.task;

import cn.projectan.strix.model.db.system.Workflow;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024-11-13 06:23:25
 */
@Schema(description = "工作流任务列表请求")
@Data
public class WorkflowTaskListReq extends BasePageReq<Workflow> {

    @Schema(description = "工作流ID")
    private String workflowId;

}
