package cn.projectan.strix.model.request.system.systemmanager;

import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import cn.projectan.strix.model.request.base.BaseReq;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/6/16 17:35
 */
@Data
public class SystemManagerUpdateReq extends BaseReq {

    /**
     * 显示昵称
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "管理人员昵称不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 16, message = "管理人员昵称长度不符合要求")
    @UpdateField
    private String nickname;

    /**
     * 登录账号
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "管理人员登录账号不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 4, max = 16, message = "管理人员登录账号长度不符合要求")
    @UpdateField
    private String loginName;

    /**
     * 登录密码
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class}, message = "管理人员登录密码不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 4, max = 16, message = "管理人员登录密码长度不符合要求")
    @UpdateField
    private String loginPassword;

    /**
     * 管理人员状态 0禁止登录 1正常
     */
    @NotNull(groups = {ValidationGroup.Insert.class}, message = "管理人员状态未选择")
    @UpdateField
    private Integer managerStatus;

    /**
     * 管理人员类型 1超级账户 2子系统账户
     */
    @NotNull(groups = {ValidationGroup.Insert.class}, message = "管理人员类型未选择")
    @UpdateField
    private Integer managerType;

    /**
     * 平台账户拥有的地区权限
     */
    @UpdateField
    private String regionId;

}
