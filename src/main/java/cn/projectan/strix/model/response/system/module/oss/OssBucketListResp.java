package cn.projectan.strix.model.response.system.module.oss;

import cn.projectan.strix.model.db.system.OssBucket;
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
 * @since 2023/5/23 12:03
 */
@Schema(description = "OSS Bucket 列表响应")
@Getter
public class OssBucketListResp extends BasePageResp {

    @Schema(description = "Bucket 列表")
    private final List<OssBucketItem> buckets;

    public OssBucketListResp(List<OssBucket> data, Long total) {
        buckets = data.stream().map(d ->
                new OssBucketItem(d.getId(), d.getConfigKey(), d.getName(), d.getRemark(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "OSS Bucket 列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssBucketItem {

        @Schema(description = "Bucket ID")
        private String id;

        @Schema(description = "配置标识")
        private String configKey;

        @Schema(description = "Bucket 名称")
        private String name;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }

}
