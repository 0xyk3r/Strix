package cn.projectan.strix.model.response.system.monitor.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "仪表板最近活动数据项")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRecentItem {

    @Schema(description = "操作用户名称")
    private String username;

    @Schema(description = "操作名称")
    private String operationName;

    @Schema(description = "操作分组")
    private String operationGroup;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;

    @Schema(description = "响应状态码")
    private Integer responseCode;

    @Schema(description = "响应时间 (ms)")
    private Long operationSpend;
}
