package cn.projectan.strix.model.request.system.role;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2021/7/1 17:14
 */
@Data
public class SystemRoleUpdateReq {

    /**
     * 角色名称
     */
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "角色名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 12, message = "角色名称长度不符合要求")
    @UpdateField
    private String name;

}
