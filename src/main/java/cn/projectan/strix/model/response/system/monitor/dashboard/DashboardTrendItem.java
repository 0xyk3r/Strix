package cn.projectan.strix.model.response.system.monitor.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "仪表板日趋势数据项")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTrendItem {

    @Schema(description = "日期 (yyyy-MM-dd)")
    private String date;

    @Schema(description = "操作总数")
    private Long totalCount;

    @Schema(description = "错误数")
    private Long errorCount;

    @Schema(description = "活跃用户数")
    private Long activeUserCount;

    @Schema(description = "平均响应时间 (ms)")
    private Long avgResponseTime;
}
