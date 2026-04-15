package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.response.system.monitor.dashboard.*;
import cn.projectan.strix.model.response.system.monitor.log.SystemLogStatsResp;
import cn.projectan.strix.service.system.SystemLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("system/monitor/dashboard")
@RequiredArgsConstructor
@Tag(name = "系统监控 - 活动仪表板")
public class DashboardController extends BaseSystemController {

    private final SystemLogService systemLogService;

    @GetMapping("overview")
    @PreAuthorize("@ss.hasPermission('system:monitor')")
    @Operation(summary = "仪表板概览")
    public RetResult<DashboardOverviewResp> overview(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "8") int rankLimit,
            @RequestParam(defaultValue = "10") int recentLimit) {
        return RetBuilder.success(systemLogService.getOverview(days, rankLimit, recentLimit));
    }

    @GetMapping("stats")
    @PreAuthorize("@ss.hasPermission('system:monitor')")
    @Operation(summary = "今日统计")
    public RetResult<SystemLogStatsResp> stats() {
        return RetBuilder.success(systemLogService.getTodayStats());
    }

    @GetMapping("trends")
    @PreAuthorize("@ss.hasPermission('system:monitor')")
    @Operation(summary = "日趋势数据")
    public RetResult<List<DashboardTrendItem>> trends(
            @RequestParam(defaultValue = "7") int days) {
        return RetBuilder.success(systemLogService.getDashboardTrends(days));
    }

    @GetMapping("hourly")
    @PreAuthorize("@ss.hasPermission('system:monitor')")
    @Operation(summary = "今日小时分布")
    public RetResult<List<DashboardHourlyItem>> hourly() {
        return RetBuilder.success(systemLogService.getHourlyDistribution());
    }

    @GetMapping("user-ranks")
    @PreAuthorize("@ss.hasPermission('system:monitor')")
    @Operation(summary = "用户活跃排名")
    public RetResult<List<DashboardRankItem>> userRanks(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "8") int limit) {
        return RetBuilder.success(systemLogService.getUserRanks(days, limit));
    }

    @GetMapping("module-ranks")
    @PreAuthorize("@ss.hasPermission('system:monitor')")
    @Operation(summary = "模块操作排名")
    public RetResult<List<DashboardRankItem>> moduleRanks(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "8") int limit) {
        return RetBuilder.success(systemLogService.getModuleRanks(days, limit));
    }

    @GetMapping("recent")
    @PreAuthorize("@ss.hasPermission('system:monitor')")
    @Operation(summary = "最近操作")
    public RetResult<List<DashboardRecentItem>> recent(
            @RequestParam(defaultValue = "10") int limit) {
        return RetBuilder.success(systemLogService.getRecentActivities(limit));
    }
}
