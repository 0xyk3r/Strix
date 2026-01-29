package cn.projectan.strix.model.response.system.module.oss;

import cn.projectan.strix.model.db.system.OssBucket;
import cn.projectan.strix.model.response.base.BasePageResp;
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
@Getter
public class OssBucketListResp extends BasePageResp {

    private final List<OssBucketItem> buckets;

    public OssBucketListResp(List<OssBucket> data, Long total) {
        buckets = data.stream().map(d ->
                new OssBucketItem(d.getId(), d.getConfigKey(), d.getName(), d.getRemark(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssBucketItem {

        private String id;

        private String configKey;

        private String name;

        private String remark;

        private LocalDateTime createdTime;

    }

}
