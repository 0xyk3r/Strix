package cn.projectan.strix.model.response.module.sms;

import cn.projectan.strix.model.db.SmsLog;
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
 * @date 2023/5/22 13:25
 */
@Getter
@NoArgsConstructor
public class SmsLogListResp extends BasePageResp {

    private List<SmsLogItem> logs = new ArrayList<>();

    public SmsLogListResp(List<SmsLog> data, Long total) {
        logs = data.stream().map(d ->
                new SmsLogItem(d.getId(), d.getConfigKey(), d.getPlatform(), d.getPhoneNumber(), d.getRequesterIp(), d.getSignName(), d.getTemplateCode(), d.getTemplateParam(), d.getStatus(), d.getPlatformResponse(), d.getCreateTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsLogItem {

        private String id;

        private String configKey;

        private Integer platform;

        private String phoneNumber;

        private String requesterIp;

        private String signName;

        private String templateCode;

        private String templateParam;

        private Integer status;

        private String platformResponse;

        private LocalDateTime createTime;

    }

}
