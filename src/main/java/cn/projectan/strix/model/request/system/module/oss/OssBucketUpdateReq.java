package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.core.validation.group.InsertGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/27 22:45
 */
@Schema(description = "OSS Bucket 更新请求")
@Data
public class OssBucketUpdateReq {

    /**
     * 存储配置 key
     */
    @Schema(description = "存储配置 Key", example = "aliyun-oss")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.ossBucket.configKey}")
    @Size(groups = {InsertGroup.class}, min = 2, max = 32, message = "{validation.length:field.ossBucket.configKey}")
    private String configKey;

    /**
     * Bucket 名称
     */
    @Schema(description = "Bucket 名称", example = "my-bucket")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.ossBucket.name}")
    @Size(groups = {InsertGroup.class}, min = 1, max = 64, message = "{validation.length:field.ossBucket.name}")
    private String name;

}
