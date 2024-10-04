package cn.projectan.strix.model.request.module.workflow;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024/4/24 下午1:18
 */
@Data
public class WorkflowConfigUpdateReq {

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "工作流配置不可为空")
    @UpdateField
    private String content;

}
