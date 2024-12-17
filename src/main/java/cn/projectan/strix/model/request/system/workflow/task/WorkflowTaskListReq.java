package cn.projectan.strix.model.request.system.workflow.task;

import cn.projectan.strix.model.db.Workflow;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024-11-13 06:23:25
 */
@Data
public class WorkflowTaskListReq extends BasePageReq<Workflow> {

    private String workflowId;

}
