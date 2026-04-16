package cn.projectan.strix.model.request.system.manager;

import cn.projectan.strix.core.validation.annotation.DynamicDictValue;
import cn.projectan.strix.core.validation.annotation.PasswordComplexity;
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
 * @since 2021/6/16 17:35
 */
@Schema(description = "管理员更新请求")
@FormSchema
@Data
public class SystemManagerUpdateReq {

    /**
     * 显示昵称
     */
    @Schema(description = "显示昵称", example = "张三")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.manager.nickname}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 20, message = "{validation.length:field.manager.nickname}")
    @UpdateField
    private String nickname;

    /**
     * 登录账号
     */
    @Schema(description = "登录账号", example = "admin")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.manager.loginName}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 4, max = 20, message = "{validation.length:field.manager.loginName}")
    @UpdateField
    private String loginName;

    /**
     * 登录密码
     */
    @Schema(description = "登录密码", example = "Abc12345")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.manager.password}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 8, max = 32, message = "{validation.length:field.manager.password}")
    @PasswordComplexity(groups = {InsertGroup.class, UpdateGroup.class})
    @UpdateField
    private String loginPassword;

    /**
     * 管理人员状态
     *
     * @see cn.projectan.strix.model.dict.system.SystemManagerStatus
     */
    @Schema(description = "管理员状态", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.selected:field.manager.status}")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "SystemManagerStatus", message = "{validation.invalid:field.manager.status}")
    @UpdateField
    private Short status;

    /**
     * 管理人员类型
     *
     * @see cn.projectan.strix.model.dict.system.SystemManagerType
     */
    @Schema(description = "管理员类型", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.selected:field.manager.type}")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "SystemManagerType", message = "{validation.invalid:field.manager.type}")
    @UpdateField
    private Short type;

    /**
     * 平台账户拥有的地区权限
     */
    @Schema(description = "地区权限ID")
    @UpdateField(allowEmpty = true)
    private String regionId;

}
