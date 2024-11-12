package cn.projectan.strix.model.request.system.workflow;

import cn.projectan.strix.model.db.Workflow;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午12:53
 */
@Data
public class WorkflowListReq extends BasePageReq<Workflow> {

    private String keyword;

}
