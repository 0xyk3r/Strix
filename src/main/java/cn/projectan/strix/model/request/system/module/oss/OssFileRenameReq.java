package cn.projectan.strix.model.request.system.module.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "文件重命名请求")
@Data
public class OssFileRenameReq {

    @Schema(description = "文件 ID")
    @NotBlank(message = "{validation.required:field.oss.fileId}")
    private String fileId;

    @Schema(description = "新文件名", example = "new_avatar.png")
    @NotBlank(message = "{validation.required:field.oss.newName}")
    @Size(max = 255)
    @Pattern(regexp = "^[^/\\\\:*?\"<>|]+$", message = "{validation.oss.invalidFileName}")
    private String newName;

}
