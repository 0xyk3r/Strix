package cn.projectan.strix.model.response.system.monitor.dashboard;

import cn.projectan.strix.model.response.system.monitor.log.SystemLogStatsResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "仪表板概览响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResp {

    @Schema(description = "今日统计")
    private SystemLogStatsResp stats;

    @Schema(description = "日趋势数据")
    private List<DashboardTrendItem> trends;

    @Schema(description = "今日小时分布")
    private List<DashboardHourlyItem> hourlyDistribution;

    @Schema(description = "用户活跃排名")
    private List<DashboardRankItem> userRanks;

    @Schema(description = "模块操作排名")
    private List<DashboardRankItem> moduleRanks;

    @Schema(description = "最近操作")
    private List<DashboardRecentItem> recentActivities;
}
