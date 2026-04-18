package cn.projectan.strix.model.request.system.module.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "文件浏览请求")
@Data
public class OssFileBrowseReq {

    @Schema(description = "文件组 Key", example = "avatar")
    @NotBlank(message = "{validation.required:field.oss.groupKey}")
    private String groupKey;

    @Schema(description = "路径前缀", example = "2025/January/")
    @Size(max = 512)
    private String prefix;

    @Schema(description = "排序字段 (name/size/time/type)", example = "name")
    private String sortBy;

    @Schema(description = "排序方向 (asc/desc)", example = "asc")
    private String sortOrder;

    @Schema(description = "搜索关键词 (全局搜索)", example = "")
    @Size(max = 128)
    private String keyword;

}
