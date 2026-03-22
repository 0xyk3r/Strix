package cn.projectan.strix.model.request.system.workflow;

import cn.projectan.strix.model.db.system.Workflow;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午12:53
 */
@Schema(description = "工作流列表请求")
@Data
public class WorkflowListReq extends BasePageReq<Workflow> {

    @Schema(description = "搜索关键词")
    @Size(max = 64)
    private String keyword;

}
