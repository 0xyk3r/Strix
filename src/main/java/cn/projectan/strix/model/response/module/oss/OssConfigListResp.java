package cn.projectan.strix.model.response.module.oss;

import cn.projectan.strix.model.db.OssConfig;
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
 * @date 2023/5/23 11:48
 */
@Getter
public class OssConfigListResp extends BasePageResp {

    private final List<OssConfigItem> configs;

    public OssConfigListResp(List<OssConfig> data, Long total) {
        configs = data.stream().map(d -> new OssConfigItem(d.getId(), d.getKey(), d.getName(), d.getPlatform(), d.getPublicEndpoint(), d.getPrivateEndpoint(), d.getAccessKey(), d.getRemark(), d.getCreateTime())).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssConfigItem {

        private String id;

        private String key;

        private String name;

        private Integer platform;

        private String publicEndpoint;

        private String privateEndpoint;

        private String accessKey;

        private String remark;

        private LocalDateTime createTime;

    }

}
