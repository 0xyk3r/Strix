package cn.projectan.strix.model.request.system.user;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2021/8/27 14:36
 */
@Data
public class SystemUserUpdateReq {

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{user_name_not_empty}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 16, message = "{user_name_length_error}")
    @UpdateField
    private String nickname;

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "管理人员状态未选择")
    @UpdateField
    private Integer status;

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "用户手机号码不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 11, max = 11, message = "用户手机号码长度不符合要求")
    @UpdateField
    private String phoneNumber;

}
