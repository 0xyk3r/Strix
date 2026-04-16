package cn.projectan.strix.model.request.system.module.oss;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.FormSchema;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "OSS Bucket 更新请求")
@FormSchema
@Data
public class OssBucketUpdateReq {

    @Schema(description = "存储配置 Key", example = "aliyun-oss")
    @NotEmpty(groups = {InsertGroup.class}, message = "{validation.required:field.ossBucket.configKey}")
    @Size(groups = {InsertGroup.class}, min = 2, max = 32, message = "{validation.length:field.ossBucket.configKey}")
    private String configKey;

    @Schema(description = "Bucket 名称", example = "my-bucket")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.ossBucket.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 64, message = "{validation.length:field.ossBucket.name}")
    @UpdateField
    private String name;

}
