package cn.projectan.strix.model.response.system.module.oss;

import cn.projectan.strix.model.db.system.OssFileGroup;
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
 * @since 2023/5/26 19:16
 */
@Schema(description = "文件分组列表响应")
@Getter
public class OssFileGroupListResp extends BasePageResp {

    @Schema(description = "文件分组列表")
    private final List<OssFileGroupItem> fileGroups;

    public OssFileGroupListResp(List<OssFileGroup> data, Long total) {
        fileGroups = data.stream().map(d ->
                new OssFileGroupItem(d.getId(), d.getKey(), d.getConfigKey(), d.getName(), d.getBucketName(), d.getBucketDomain(), d.getBaseDir(), d.getAllowExtension(), d.getSecretType(), d.getSecretLevel(), d.getRemark(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "文件分组列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssFileGroupItem {

        @Schema(description = "分组ID")
        private String id;

        @Schema(description = "分组标识")
        private String key;

        @Schema(description = "配置标识")
        private String configKey;

        @Schema(description = "分组名称")
        private String name;

        @Schema(description = "Bucket 名称")
        private String bucketName;

        @Schema(description = "Bucket 域名")
        private String bucketDomain;

        @Schema(description = "基础目录")
        private String baseDir;

        @Schema(description = "允许的文件扩展名")
        private String allowExtension;

        @Schema(description = "加密类型")
        private Short secretType;

        @Schema(description = "加密等级")
        private Short secretLevel;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }

}
