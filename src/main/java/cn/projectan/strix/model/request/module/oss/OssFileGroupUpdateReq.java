package cn.projectan.strix.model.request.module.oss;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2023/5/27 22:16
 */
@Data
public class OssFileGroupUpdateReq {

    /**
     * 文件组配置 key
     */
    @NotEmpty(groups = {InsertGroup.class}, message = "文件组配置 key 不可为空")
    @Size(groups = {InsertGroup.class}, min = 2, max = 32, message = "文件组配置 key 长度不符合要求")
    private String key;

    /**
     * 存储配置 key
     */
    @NotEmpty(groups = {InsertGroup.class}, message = "存储配置 key 不可为空")
    @Size(groups = {InsertGroup.class}, min = 2, max = 32, message = "存储配置 key 长度不符合要求")
    private String configKey;

    /**
     * 存储服务名称
     */
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "存储服务名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 32, message = "存储服务名称长度不符合要求")
    @UpdateField
    private String name;

    /**
     * Bucket 名称
     */
    @NotEmpty(groups = {InsertGroup.class}, message = "Bucket 名称不可为空")
    @Size(groups = {InsertGroup.class}, min = 1, max = 64, message = "存储服务名称长度不符合要求")
    private String bucketName;

    /**
     * Bucket 自定义域名
     */
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64, message = "Bucket 自定义域名长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String bucketDomain;

    /**
     * 基础路径
     */
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 64, message = "基础路径长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String baseDir;

    /**
     * 允许的文件扩展名
     */
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "允许的文件扩展名不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 1024, message = "允许的文件扩展名长度不符合要求")
    @UpdateField
    private String allowExtension;

    /**
     * 查看权限类型 1管理端文件 2用户端文件
     */
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "查看权限类型不可为空")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 1, message = "查看权限类型超出可用范围")
    @Max(groups = {InsertGroup.class, UpdateGroup.class}, value = 2, message = "查看权限类型超出可用范围")
    @UpdateField
    private Integer secretType;

    /**
     * 查看权限等级 越大等级越高
     */
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "查看权限等级不可为空")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "查看权限等级超出可用范围")
    @Max(groups = {InsertGroup.class, UpdateGroup.class}, value = 10, message = "查看权限等级超出可用范围")
    @UpdateField
    private Integer secretLevel;

    /**
     * 备注
     */
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 255, message = "备注长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String remark;

}
