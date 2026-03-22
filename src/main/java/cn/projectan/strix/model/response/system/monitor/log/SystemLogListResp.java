package cn.projectan.strix.model.response.system.monitor.log;

import cn.projectan.strix.model.db.system.SystemLog;
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
 * @since 2023/6/17 22:29
 */
@Schema(description = "操作日志列表响应")
@Getter
@NoArgsConstructor
public class SystemLogListResp extends BasePageResp {

    @Schema(description = "日志列表")
    private List<SystemLogItem> items = new ArrayList<>();

    public SystemLogListResp(List<SystemLog> data, Long total) {
        items = data.stream().map(d ->
                new SystemLogItem(d.getAppId(), d.getAppVersion(), d.getOperationType(), d.getOperationGroup(), d.getOperationName(), d.getOperationSpend(), d.getOperationMethod(), d.getOperationUrl(), d.getOperationParam(),
                        d.getOperationTime(), d.getClientIp(), d.getClientDevice(), d.getClientUser(), d.getClientUsername(), d.getResponseCode(), d.getResponseMsg(), d.getResponseData())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "操作日志列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemLogItem {

        @Schema(description = "应用ID")
        private String appId;

        @Schema(description = "应用版本")
        private String appVersion;

        @Schema(description = "操作类型")
        private String operationType;

        @Schema(description = "操作分组")
        private String operationGroup;

        @Schema(description = "操作名称")
        private String operationName;

        @Schema(description = "操作耗时（毫秒）")
        private Long operationSpend;

        @Schema(description = "请求方法")
        private String operationMethod;

        @Schema(description = "请求URL")
        private String operationUrl;

        @Schema(description = "请求参数")
        private String operationParam;

        @Schema(description = "操作时间")
        private LocalDateTime operationTime;

        @Schema(description = "客户端IP")
        private String clientIp;

        @Schema(description = "客户端设备")
        private String clientDevice;

        @Schema(description = "操作用户ID")
        private String clientUser;

        @Schema(description = "操作用户名")
        private String clientUsername;

        @Schema(description = "响应状态码")
        private Integer responseCode;

        @Schema(description = "响应消息")
        private String responseMsg;

        @Schema(description = "响应数据")
        private String responseData;

    }

}
