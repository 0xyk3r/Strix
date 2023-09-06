package cn.projectan.strix.model.request.system.manager;

import cn.projectan.strix.core.validation.annotation.DynamicDictValue;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/6/16 17:35
 */
@Data
public class SystemManagerUpdateReq {

    /**
     * 显示昵称
     */
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "管理人员昵称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 16, message = "管理人员昵称长度不符合要求")
    @UpdateField
    private String nickname;

    /**
     * 登录账号
     */
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "管理人员登录账号不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 4, max = 16, message = "管理人员登录账号长度不符合要求")
    @UpdateField
    private String loginName;

    /**
     * 登录密码
     */
    @NotEmpty(groups = {InsertGroup.class}, message = "管理人员登录密码不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 4, max = 16, message = "管理人员登录密码长度不符合要求")
    @UpdateField
    private String loginPassword;

    /**
     * 管理人员状态
     *
     * @see cn.projectan.strix.model.dict.SystemManagerStatus
     */
    @NotNull(groups = {InsertGroup.class}, message = "管理人员状态未选择")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "SystemManagerStatus", message = "管理人员状态不合法")
    @UpdateField
    private Integer status;

    /**
     * 管理人员类型
     *
     * @see cn.projectan.strix.model.dict.SystemManagerType
     */
    @NotNull(groups = {InsertGroup.class}, message = "管理人员类型未选择")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "SystemManagerType", message = "管理人员类型不合法")
    @UpdateField
    private Integer type;

    /**
     * 平台账户拥有的地区权限
     */
    @UpdateField
    private String regionId;

}
