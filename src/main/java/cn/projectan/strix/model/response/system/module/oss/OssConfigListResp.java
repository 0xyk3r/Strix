package cn.projectan.strix.model.response.system.module.oss;

import cn.projectan.strix.model.db.system.OssConfig;
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
 * @since 2023/5/23 11:48
 */
@Schema(description = "OSS 配置列表响应")
@Getter
public class OssConfigListResp extends BasePageResp {

    @Schema(description = "配置列表")
    private final List<OssConfigItem> configs;

    public OssConfigListResp(List<OssConfig> data, Long total) {
        configs = data.stream().map(d ->
                new OssConfigItem(d.getId(), d.getKey(), d.getName(), d.getPlatform(), d.getRegion(), d.getPublicEndpoint(), d.getPrivateEndpoint(), d.getAccessKey(), d.getRemark(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "OSS 配置列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssConfigItem {

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

    }

}
