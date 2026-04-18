package cn.projectan.strix.model.request.system.module.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "创建目录请求")
@Data
public class OssFileMkdirReq {

    @Schema(description = "文件组 Key")
    @NotBlank(message = "{validation.required:field.oss.groupKey}")
    private String groupKey;

    @Schema(description = "父路径前缀", example = "2025/")
    @Size(max = 512)
    private String parentPrefix;

    @Schema(description = "目录名称", example = "January")
    @NotBlank(message = "{validation.required:field.oss.dirName}")
    @Size(max = 128)
    @Pattern(regexp = "^[^/\\\\:*?\"<>|]+$", message = "{validation.oss.invalidDirName}")
    private String dirName;

}
