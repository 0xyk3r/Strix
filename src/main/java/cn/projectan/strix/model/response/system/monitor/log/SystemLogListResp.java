package cn.projectan.strix.model.response.system.monitor.log;

import cn.projectan.strix.model.db.SystemLog;
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
 * @since 2023/6/17 22:29
 */
@Getter
@NoArgsConstructor
public class SystemLogListResp extends BasePageResp {

    private List<SystemLogItem> items = new ArrayList<>();

    public SystemLogListResp(List<SystemLog> data, Long total) {
        items = data.stream().map(d ->
                new SystemLogItem(d.getAppId(), d.getAppVersion(), d.getOperationType(), d.getOperationGroup(), d.getOperationName(), d.getOperationSpend(), d.getOperationMethod(), d.getOperationUrl(), d.getOperationParam(),
                        d.getOperationTime(), d.getClientIp(), d.getClientDevice(), d.getClientLocation(), d.getClientUser(), d.getClientUsername(), d.getResponseCode(), d.getResponseMsg(), d.getResponseData())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemLogItem {

        private String appId;

        private String appVersion;

        private String operationType;

        private String operationGroup;

        private String operationName;

        private Long operationSpend;

        private String operationMethod;

        private String operationUrl;

        private String operationParam;

        private LocalDateTime operationTime;

        private String clientIp;

        private String clientDevice;

        private String clientLocation;

        private String clientUser;

        private String clientUsername;

        private Integer responseCode;

        private String responseMsg;

        private String responseData;

    }

}
