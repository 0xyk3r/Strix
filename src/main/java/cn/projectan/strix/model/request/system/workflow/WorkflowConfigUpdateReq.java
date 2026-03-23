package cn.projectan.strix.model.request.system.workflow;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午1:18
 */
@Schema(description = "工作流配置更新请求")
@Data
public class WorkflowConfigUpdateReq {

    @Schema(description = "工作流配置内容")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.workflow.config}")
    @UpdateField
    private String content;

}
