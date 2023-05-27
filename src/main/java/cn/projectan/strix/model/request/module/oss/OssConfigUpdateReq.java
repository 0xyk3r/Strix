package cn.projectan.strix.model.request.module.oss;

import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import cn.projectan.strix.model.constant.StrixSmsPlatform;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/5/23 12:20
 */
@Data
public class OssConfigUpdateReq {

    /**
     * 配置 key
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "配置 key 不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 32, message = "配置 key 长度不符合要求")
    @UpdateField
    private String key;

    /**
     * 存储服务名称
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "存储服务名称不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 32, message = "存储服务名称长度不符合要求")
    @UpdateField
    private String name;

    /**
     * 存储服务平台
     *
     * @see StrixSmsPlatform
     */
    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "存储服务平台不可为空")
    @UpdateField
    private Integer platform;

    /**
     * 公网连接域名
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "公网连接域名不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 1, max = 128, message = "公网连接域名长度不符合要求")
    @UpdateField
    private String publicEndpoint;

    /**
     * 内网连接域名
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "内网连接域名不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 1, max = 128, message = "内网连接域名长度不符合要求")
    @UpdateField
    private String privateEndpoint;

    /**
     * 授权令牌key
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "AccessKey 不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, max = 64, message = "AccessKey 长度不符合要求")
    @UpdateField
    private String accessKey;

    /**
     * 授权令牌秘钥
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class}, message = "AccessSecret 不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, max = 64, message = "AccessSecret 长度不符合要求")
    @UpdateField
    private String accessSecret;

    /**
     * 备注
     */
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, max = 255, message = "备注长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String remark;

}
