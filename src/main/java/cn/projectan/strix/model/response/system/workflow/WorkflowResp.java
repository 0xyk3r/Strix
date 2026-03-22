package cn.projectan.strix.model.response.system.workflow;

import cn.projectan.strix.model.db.system.Workflow;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午1:16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工作流详情响应")
public class WorkflowResp {

    @Schema(description = "工作流ID")
    private String id;

    @Schema(description = "工作流名称")
    private String name;

    public WorkflowResp(Workflow data) {
        this.id = data.getId();
        this.name = data.getName();
    }

}
