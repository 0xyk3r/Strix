package cn.projectan.strix.model.request.system.systemuser;

import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import cn.projectan.strix.model.request.base.BaseReq;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author 安炯奕
 * @date 2021/8/27 14:36
 */
@Data
public class SystemUserUpdateReq extends BaseReq {

    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "{user_name_not_empty}")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 16, message = "{user_name_length_error}")
    @UpdateField
    private String nickname;

    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "管理人员状态未选择")
    @UpdateField
    private Integer status;

    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "用户手机号码不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 11, max = 11, message = "用户手机号码长度不符合要求")
    @UpdateField
    private String phoneNumber;

}
