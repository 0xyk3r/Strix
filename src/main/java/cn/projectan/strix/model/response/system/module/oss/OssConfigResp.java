package cn.projectan.strix.model.response.system.module.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2023/5/23 11:59
 */
@Schema(description = "OSS 配置详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssConfigResp {

    @Schema(description = "配置ID")
    private String id;

    @Schema(description = "配置标识")
    private String key;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "平台类型")
    private Short platform;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "公网端点")
    private String publicEndpoint;

    @Schema(description = "内网端点")
    private String privateEndpoint;

    @Schema(description = "访问密钥")
    private String accessKey;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "Bucket 列表")
    private List<OssBucketListResp.OssBucketItem> buckets;

    @Schema(description = "文件分组列表")
    private List<OssFileGroupListResp.OssFileGroupItem> fileGroups;

}
