package cn.projectan.strix.model.response.module.sms;

import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.model.response.base.BasePageResp;
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
 * @date 2023/5/20 19:09
 */
@Getter
@NoArgsConstructor
public class SmsConfigListResp extends BasePageResp {

    private List<SmsConfigItem> configs = new ArrayList<>();

    public SmsConfigListResp(List<SmsConfig> data, Long total) {
        configs = data.stream().map(d ->
                new SmsConfigItem(d.getId(), d.getKey(), d.getName(), d.getPlatform(), d.getRegionId(), d.getAccessKey(), d.getRemark(), d.getCreateTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsConfigItem {

        private String id;

        private String key;

        private String name;

        private Integer platform;

        private String regionId;

        private String accessKey;

        private String remark;

        private LocalDateTime createTime;

    }

}
