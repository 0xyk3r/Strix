package cn.projectan.strix.model.request.module.oss;

import cn.projectan.strix.core.validation.group.InsertGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/27 22:45
 */
@Data
public class OssBucketUpdateReq {

    /**
     * 存储配置 key
     */
    @NotEmpty(groups = {InsertGroup.class}, message = "存储配置 key 不可为空")
    @Size(groups = {InsertGroup.class}, min = 2, max = 32, message = "存储配置 key 长度不符合要求")
    private String configKey;

    /**
     * Bucket 名称
     */
    @NotEmpty(groups = {InsertGroup.class}, message = "Bucket 名称不可为空")
    @Size(groups = {InsertGroup.class}, min = 1, max = 64, message = "Bucket 名称长度不符合要求")
    private String name;

    /**
     * 存储类型
     */
    @NotEmpty(groups = {InsertGroup.class}, message = "存储类型不可为空")
    @Size(groups = {InsertGroup.class}, min = 1, max = 32, message = "存储类型不符合要求")
    private String storageClass;

}
