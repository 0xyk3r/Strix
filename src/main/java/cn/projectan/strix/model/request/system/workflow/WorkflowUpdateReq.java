package cn.projectan.strix.model.request.system.workflow;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午1:18
 */
@Schema(description = "工作流更新请求")
@Data
public class WorkflowUpdateReq {

    @Schema(description = "工作流名称")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.workflow.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 32, message = "{validation.length:field.workflow.name}")
    @UpdateField
    private String name;

}
