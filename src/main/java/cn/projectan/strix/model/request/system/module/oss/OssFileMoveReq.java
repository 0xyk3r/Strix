package cn.projectan.strix.model.request.system.module.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "文件移动请求")
@Data
public class OssFileMoveReq {

    @Schema(description = "文件 ID 列表")
    @NotEmpty(message = "{validation.required:field.oss.fileIds}")
    private List<String> fileIds;

    @Schema(description = "目标文件组 Key")
    @NotBlank(message = "{validation.required:field.oss.targetGroupKey}")
    private String targetGroupKey;

    @Schema(description = "目标路径前缀", example = "2025/March/")
    @Size(max = 512)
    private String targetPrefix;

}
