package cn.projectan.strix.model.response.system.module.oss;

import cn.projectan.strix.model.db.system.OssFile;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2023/5/26 20:45
 */
@Schema(description = "OSS 文件列表响应")
@Getter
public class OssFileListResp extends BasePageResp {

    @Schema(description = "文件列表")
    private final List<OssFileItem> files;

    public OssFileListResp(List<OssFile> data, Long total) {
        files = data.stream().map(d ->
                new OssFileItem(d.getId(), d.getConfigKey(), d.getGroupKey(), d.getPath(),
                        d.getSize(), d.getExt(), d.getOriginalName(), d.getContentType(),
                        d.getCreatedBy(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "OSS 文件列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssFileItem {

        @Schema(description = "文件ID")
        private String id;

        @Schema(description = "配置标识")
        private String configKey;

        @Schema(description = "分组标识")
        private String groupKey;

        @Schema(description = "文件路径")
        private String path;

        @Schema(description = "文件大小")
        private Long size;

        @Schema(description = "文件扩展名")
        private String ext;

        @Schema(description = "文件原始名称")
        private String originalName;

        @Schema(description = "MIME 类型")
        private String contentType;

        @Schema(description = "上传者ID")
        private String uploaderId;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }

}
