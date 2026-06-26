package cn.projectan.strix.model.response.system.profile;

import cn.projectan.strix.model.db.system.SystemLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人登录记录响应
 *
 * @author ProjectAn
 */
@Schema(description = "个人登录记录响应")
@Getter
@NoArgsConstructor
public class ProfileLoginLogResp {

    @Schema(description = "登录记录列表")
    private List<LoginLogItem> loginLogList;

    @Schema(description = "总数")
    private long total;

    public ProfileLoginLogResp(List<SystemLog> records, long total) {
        this.loginLogList = records.stream().map(LoginLogItem::new).toList();
        this.total = total;
    }

    @Schema(description = "登录记录项")
    @Data
    @NoArgsConstructor
    public static class LoginLogItem {

        @Schema(description = "记录ID")
        private String id;

        @Schema(description = "登录时间")
        private LocalDateTime operationTime;

        @Schema(description = "登录 IP")
        private String clientIp;

        @Schema(description = "登录设备/操作系统")
        private String clientDevice;

        @Schema(description = "响应状态码")
        private Integer responseCode;

        @Schema(description = "响应消息")
        private String responseMsg;

        public LoginLogItem(SystemLog log) {
            this.id = log.getId();
            this.operationTime = log.getOperationTime();
            this.clientIp = log.getClientIp();
            this.clientDevice = log.getClientDevice();
            this.responseCode = log.getResponseCode();
            this.responseMsg = log.getResponseMsg();
        }
    }

}
