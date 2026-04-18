package cn.projectan.strix.model.request.system.module.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "批量删除文件请求")
@Data
public class OssFileBatchRemoveReq {

    @Schema(description = "文件 ID 列表")
    @NotEmpty(message = "{validation.required:field.oss.fileIds}")
    private List<String> fileIds;

}
