package cn.projectan.strix.model.request.system.config;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.FormSchema;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统配置更新请求
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Schema(description = "系统配置更新请求")
@FormSchema
@Data
public class SystemConfigUpdateReq {

    /**
     * 配置项标识
     */
    @Schema(description = "配置项标识", example = "system.login.captcha")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.config.key}")
    @Size(groups = {InsertGroup.class}, min = 2, max = 64, message = "{validation.length:field.config.key}")
    private String key;

    /**
     * 配置项名称
     */
    @Schema(description = "配置项名称", example = "登录验证码开关")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.config.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 32, message = "{validation.length:field.config.name}")
    @UpdateField
    private String name;

    /**
     * 配置类型 1开关 2内容
     */
    @Schema(description = "配置类型: 1=开关, 2=内容", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.selected:field.config.type}")
    @UpdateField
    private Short type;

    /**
     * 配置值
     */
    @Schema(description = "配置值", example = "true")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 2048, message = "{validation.length:field.config.value}")
    @UpdateField(allowEmpty = true)
    private String value;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "控制登录页是否显示验证码")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 255, message = "{validation.length:field.config.remark}")
    @UpdateField(allowEmpty = true)
    private String remark;

}
