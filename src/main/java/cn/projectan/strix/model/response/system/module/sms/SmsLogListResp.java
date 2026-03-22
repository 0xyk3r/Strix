package cn.projectan.strix.model.response.system.module.sms;

import cn.projectan.strix.model.db.system.SmsLog;
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
 * @since 2023/5/22 13:25
 */
@Schema(description = "短信日志列表响应")
@Getter
@NoArgsConstructor
public class SmsLogListResp extends BasePageResp {

    @Schema(description = "日志列表")
    private List<SmsLogItem> logs = new ArrayList<>();

    public SmsLogListResp(List<SmsLog> data, Long total) {
        logs = data.stream().map(d ->
                new SmsLogItem(d.getId(), d.getConfigKey(), d.getPlatform(), d.getPhoneNumber(), d.getRequesterIp(), d.getSignName(), d.getTemplateCode(), d.getTemplateParam(), d.getStatus(), d.getPlatformResponse(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "短信日志列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsLogItem {

        @Schema(description = "日志ID")
        private String id;

        @Schema(description = "配置标识")
        private String configKey;

        @Schema(description = "平台类型")
        private Short platform;

        @Schema(description = "手机号码")
        private String phoneNumber;

        @Schema(description = "请求者IP")
        private String requesterIp;

        @Schema(description = "签名名称")
        private String signName;

        @Schema(description = "模板编码")
        private String templateCode;

        @Schema(description = "模板参数")
        private String templateParam;

        @Schema(description = "发送状态")
        private Short status;

        @Schema(description = "平台响应")
        private String platformResponse;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }

}
