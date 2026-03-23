package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.core.validation.annotation.DynamicDictValue;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/27 22:16
 */
@Schema(description = "文件分组更新请求")
@Data
public class OssFileGroupUpdateReq {

    /**
     * 文件组配置 key
     */
    @Schema(description = "文件组配置 Key", example = "avatar")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.ossFileGroup.configKey}")
    @Size(groups = {InsertGroup.class}, min = 2, max = 32, message = "{validation.length:field.ossFileGroup.configKey}")
    private String key;

    /**
     * 存储配置 key
     */
    @Schema(description = "存储配置 Key", example = "aliyun-oss")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.ossFileGroup.ossConfigKey}")
    @Size(groups = {InsertGroup.class}, min = 2, max = 32, message = "{validation.length:field.ossFileGroup.ossConfigKey}")
    private String configKey;

    /**
     * 存储服务名称
     */
    @Schema(description = "文件分组名称", example = "用户头像")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.ossFileGroup.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 32, message = "{validation.length:field.ossFileGroup.name}")
    @UpdateField
    private String name;

    /**
     * Bucket 名称
     */
    @Schema(description = "Bucket 名称", example = "my-bucket")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.ossFileGroup.bucket}")
    @Size(groups = {InsertGroup.class}, min = 1, max = 64, message = "{validation.length:field.ossFileGroup.bucket}")
    private String bucketName;

    /**
     * Bucket 自定义域名
     */
    @Schema(description = "Bucket 自定义域名", example = "cdn.example.com")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64, message = "{validation.length:field.ossFileGroup.customDomain}")
    @UpdateField(allowEmpty = true)
    private String bucketDomain;

    /**
     * 基础路径
     */
    @Schema(description = "基础路径", example = "upload/avatar")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64, message = "{validation.length:field.ossFileGroup.basePath}")
    @UpdateField(allowEmpty = true)
    private String baseDir;

    /**
     * 允许的文件扩展名
     */
    @Schema(description = "允许的文件扩展名", example = "jpg,png,gif,webp")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.ossFileGroup.allowedExtensions}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 1024, message = "{validation.length:field.ossFileGroup.allowedExtensions}")
    @UpdateField
    private String allowExtension;

    /**
     * 查看权限类型 1管理端文件 2用户端文件
     *
     * @see cn.projectan.strix.model.dict.system.OssFileGroupSecretType
     */
    @Schema(description = "查看权限类型（1-管理端文件 2-用户端文件）", example = "1")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.ossFileGroup.secretType}")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "OssFileGroupSecretType", message = "{validation.invalid:field.ossFileGroup.secretType}")
    @UpdateField
    private Short secretType;

    /**
     * 查看权限等级 越大等级越高
     */
    @Schema(description = "查看权限等级，越大等级越高", example = "0")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.ossFileGroup.secretLevel}")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "{validation.outOfRange:field.ossFileGroup.secretLevel}")
    @Max(groups = {InsertGroup.class, UpdateGroup.class}, value = 10, message = "{validation.outOfRange:field.ossFileGroup.secretLevel}")
    @UpdateField
    private Short secretLevel;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "用户头像文件分组")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 255, message = "{validation.length:field.ossFileGroup.remark}")
    @UpdateField(allowEmpty = true)
    private String remark;

}
