package cn.projectan.strix.model.response.module.oss;

import cn.projectan.strix.model.db.OssBucket;
import cn.projectan.strix.model.response.base.BasePageResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2023/5/23 12:03
 */
@Getter
public class OssBucketListResp extends BasePageResp {

    private final List<OssBucketItem> buckets;

    public OssBucketListResp(List<OssBucket> data, Long total) {
        buckets = data.stream().map(d -> new OssBucketItem(d.getId(), d.getConfigKey(), d.getName(), d.getPublicEndpoint(), d.getPrivateEndpoint(), d.getRegion(), d.getStorageClass(), d.getRemark(), d.getCreateTime())).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssBucketItem {

        private String id;

        private String configKey;

        private String name;

        private String publicEndpoint;

        private String privateEndpoint;

        private String region;

        private String storageClass;

        private String remark;

        private LocalDateTime createTime;

    }

}
