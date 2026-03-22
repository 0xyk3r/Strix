package cn.projectan.strix.model.response.system.module.sms;

import cn.projectan.strix.model.db.system.SmsConfig;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2023/5/20 19:09
 */
@Schema(description = "短信配置列表响应")
@Getter
@NoArgsConstructor
public class SmsConfigListResp extends BasePageResp {

    @Schema(description = "配置列表")
    private List<SmsConfigItem> configs = new ArrayList<>();

    public SmsConfigListResp(List<SmsConfig> data, Long total) {
        configs = data.stream().map(d ->
                new SmsConfigItem(d.getId(), d.getKey(), d.getName(), d.getPlatform(), d.getRegionId(), d.getAccessKey(), d.getRemark(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "短信配置列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsConfigItem {

        @Schema(description = "配置ID")
        private String id;

        @Schema(description = "配置标识")
        private String key;

        @Schema(description = "配置名称")
        private String name;

        @Schema(description = "平台类型")
        private Short platform;

        @Schema(description = "区域ID")
        private String regionId;

        @Schema(description = "访问密钥")
        private String accessKey;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }

}
