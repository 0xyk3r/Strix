package cn.projectan.strix.model.request.system.workflow;

import cn.projectan.strix.model.db.system.Workflow;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午12:53
 */
@Data
public class WorkflowListReq extends BasePageReq<Workflow> {

    @Size(max = 64)
    private String keyword;

}
