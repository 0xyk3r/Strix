package cn.projectan.strix.model.response.system.monitor.session;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在线会话列表响应
 *
 * @author ProjectAn
 */
@Schema(description = "在线会话列表响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineSessionResp {

    @Schema(description = "在线会话列表")
    private List<OnlineSessionItem> items;

    @Schema(description = "在线管理员总数")
    private int onlineManagerCount;

    @Schema(description = "在线会话总数")
    private int totalSessionCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnlineSessionItem {

        @Schema(description = "管理员 ID")
        private String managerId;

        @Schema(description = "管理员昵称")
        private String nickname;

        @Schema(description = "登录账号")
        private String loginName;

        @Schema(description = "Token (脱敏)")
        private String tokenMasked;

        @Schema(description = "登录时间")
        private LocalDateTime loginTime;

        @Schema(description = "最后活跃时间")
        private LocalDateTime lastActiveTime;

        @Schema(description = "客户端 IP")
        private String ip;

        @Schema(description = "设备/操作系统")
        private String device;

        @Schema(description = "该管理员的会话数")
        private int sessionCount;
    }
}
