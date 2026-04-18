package cn.projectan.strix.model.response.system.module.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "压缩包内容列表响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssFileArchiveResp {

    @Schema(description = "文件条目列表")
    private List<ArchiveEntry> entries;

    @Schema(description = "文件总数")
    private int totalFiles;

    @Schema(description = "总大小 (字节)")
    private long totalSize;

    @Schema(description = "压缩包条目")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchiveEntry {

        @Schema(description = "路径")
        private String path;

        @Schema(description = "大小 (字节)")
        private long size;

        @Schema(description = "压缩后大小 (字节)")
        private long compressed;

        @Schema(description = "是否为目录")
        private boolean isDirectory;

    }

}
