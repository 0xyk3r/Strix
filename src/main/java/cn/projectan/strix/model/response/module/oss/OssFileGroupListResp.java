package cn.projectan.strix.model.response.module.oss;

import cn.projectan.strix.model.db.OssFileGroup;
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
 * @date 2023/5/26 19:16
 */
@Getter
public class OssFileGroupListResp extends BasePageResp {

    private final List<OssFileGroupItem> fileGroups;

    public OssFileGroupListResp(List<OssFileGroup> data, Long total) {
        fileGroups = data.stream().map(d -> new OssFileGroupItem(d.getId(), d.getKey(), d.getConfigKey(), d.getName(), d.getBucketName(), d.getBucketDomain(), d.getBaseDir(), d.getAllowExtension(), d.getSecretType(), d.getSecretLevel(), d.getRemark(), d.getCreateTime())).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssFileGroupItem {

        private String id;

        private String key;

        private String configKey;

        private String name;

        private String bucketName;

        private String bucketDomain;

        private String baseDir;

        private String allowExtension;

        private Integer secretType;

        private Integer secretLevel;

        private String remark;

        private LocalDateTime createTime;

    }

}
