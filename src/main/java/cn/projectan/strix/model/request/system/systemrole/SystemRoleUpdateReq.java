package cn.projectan.strix.model.request.system.systemrole;

import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import cn.projectan.strix.model.request.base.BaseReq;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/7/1 17:14
 */
@Data
public class SystemRoleUpdateReq extends BaseReq {


    /**
     * 角色名称
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "角色名称不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 12, message = "角色名称长度不符合要求")
    @UpdateField
    private String name;

}
