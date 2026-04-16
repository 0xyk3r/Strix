package cn.projectan.strix.model.request.system.module.sms;

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
 * @since 2023/5/22 11:59
 */
@Schema(description = "短信配置更新请求")
@FormSchema
@Data
public class SmsConfigUpdateReq {

    /**
     * 配置 key
     */
    @Schema(description = "配置 key")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.sms.configKey}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 32, message = "{validation.length:field.sms.configKey}")
    @UpdateField
    private String key;

    /**
     * 短信服务名称
     */
    @Schema(description = "短信服务名称")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.sms.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 32, message = "{validation.length:field.sms.name}")
    @UpdateField
    private String name;

    /**
     * 短信服务平台
     *
     * @see cn.projectan.strix.model.dict.system.SmsPlatform
     */
    @Schema(description = "短信服务平台")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.sms.platform}")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "SmsPlatform", message = "{validation.invalid:field.sms.platform}")
    @UpdateField
    private Short platform;

    /**
     * 短信服务地区ID
     */
    @Schema(description = "短信服务地区ID")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.sms.region}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 32, message = "{validation.length:field.sms.region}")
    @UpdateField
    private String regionId;

    /**
     * 授权令牌key
     */
    @Schema(description = "授权令牌key")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.sms.accessKey}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64, message = "{validation.length:field.sms.accessKey}")
    @UpdateField
    private String accessKey;

    /**
     * 授权令牌秘钥
     */
    @Schema(description = "授权令牌秘钥")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.sms.accessSecret}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64, message = "{validation.length:field.sms.accessSecret}")
    @UpdateField
    private String accessSecret;

    /**
     * 备注
     */
    @Schema(description = "备注")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 255, message = "{validation.length:field.sms.remark}")
    @UpdateField(allowEmpty = true)
    private String remark;

}
