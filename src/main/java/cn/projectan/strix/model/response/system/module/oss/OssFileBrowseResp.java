package cn.projectan.strix.model.response.system.module.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "文件浏览响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssFileBrowseResp {

    @Schema(description = "文件组 Key")
    private String groupKey;

    @Schema(description = "当前路径前缀")
    private String prefix;

    @Schema(description = "目录列表")
    private List<DirectoryItem> directories;

    @Schema(description = "文件列表")
    private List<FileItem> files;

    @Schema(description = "面包屑路径段")
    private List<String> breadcrumb;

    @Schema(description = "目录项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectoryItem {

        @Schema(description = "目录名称")
        private String name;

        @Schema(description = "目录路径前缀")
        private String path;

        @Schema(description = "目录内文件数")
        private long fileCount;

    }

    @Schema(description = "文件项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileItem {

        @Schema(description = "文件 ID")
        private String id;

        @Schema(description = "文件原始名称")
        private String originalName;

        @Schema(description = "文件路径")
        private String path;

        @Schema(description = "文件大小 (字节)")
        private Long size;

        @Schema(description = "文件扩展名")
        private String ext;

        @Schema(description = "MIME 类型")
        private String contentType;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

        @Schema(description = "上传者 ID")
        private String createdBy;

    }

}
