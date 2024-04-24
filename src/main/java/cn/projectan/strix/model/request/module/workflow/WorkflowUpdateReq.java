package cn.projectan.strix.model.request.module.workflow;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2024/4/24 下午1:18
 */
@Data
public class WorkflowUpdateReq {

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "工作流名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 32, message = "工作流名称长度不符合要求")
    @UpdateField
    private String name;

}
