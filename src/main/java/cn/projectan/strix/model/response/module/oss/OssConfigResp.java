package cn.projectan.strix.model.response.module.oss;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2023/5/23 11:59
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssConfigResp {

    private String id;

    private String key;

    private String name;

    private Integer platform;

    private String publicEndpoint;

    private String privateEndpoint;

    private String accessKey;

    private String remark;

    private LocalDateTime createTime;

    private List<OssBucketListResp.OssBucketItem> buckets;

    private List<OssFileGroupListResp.OssFileGroupItem> fileGroups;

}
