package cn.projectan.strix.model.request.system.user;

import cn.projectan.strix.core.validation.annotation.DynamicDictValue;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import cn.projectan.strix.model.annotation.FormSchema;

/**
 * @author ProjectAn
 * @since 2021/8/27 14:36
 */
@Schema(description = "用户更新请求")
@FormSchema
@Data
public class SystemUserUpdateReq {

    @Schema(description = "用户昵称")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.user.nickname}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 16, message = "{validation.length:field.user.nickname}")
    @UpdateField
    private String nickname;

    @Schema(description = "用户状态")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.selected:field.user.status}")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "SystemUserStatus", message = "{validation.invalid:field.user.status}")
    @UpdateField
    private Short status;

    @Schema(description = "用户手机号码")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.user.phone}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 11, max = 11, message = "{validation.length:field.user.phone}")
    @UpdateField
    private String phoneNumber;

}
