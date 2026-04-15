package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemLogMapper;
import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.request.system.monitor.log.SystemLogListReq;
import cn.projectan.strix.model.response.system.monitor.dashboard.*;
import cn.projectan.strix.model.response.system.monitor.log.SystemLogStatsResp;
import cn.projectan.strix.util.math.NumUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Strix 系统日志 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-06-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogService extends ServiceImpl<SystemLogMapper, SystemLog> {

    /**
     * 分页查询系统操作日志
     *
     * @param req 查询请求
     * @return 分页结果
     */
    public Page<SystemLog> listPage(SystemLogListReq req) {
        return lambdaQuery()
                .eq(StringUtils.hasText(req.getOperationType()), SystemLog::getOperationType, req.getOperationType())
                .eq(StringUtils.hasText(req.getOperationGroup()), SystemLog::getOperationGroup, req.getOperationGroup())
                .like(StringUtils.hasText(req.getKeyword()), SystemLog::getOperationName, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getResponseCode(), NumCategory.POSITIVE), SystemLog::getResponseCode, req.getResponseCode())
                .like(StringUtils.hasText(req.getClientUsername()), SystemLog::getClientUsername, req.getClientUsername())
                .like(StringUtils.hasText(req.getClientIp()), SystemLog::getClientIp, req.getClientIp())
                .ge(req.getStartTime() != null, SystemLog::getOperationTime, req.getStartTime())
                .le(req.getEndTime() != null, SystemLog::getOperationTime, req.getEndTime())
                .orderByDesc(SystemLog::getOperationTime)
                .page(req.getPage());
    }

    /**
     * 获取今日操作日志统计
     *
     * @return 统计结果
     */
    public SystemLogStatsResp getTodayStats() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<SystemLog> todayLogs = lambdaQuery()
                .ge(SystemLog::getOperationTime, todayStart)
                .le(SystemLog::getOperationTime, todayEnd)
                .select(SystemLog::getResponseCode, SystemLog::getOperationSpend, SystemLog::getClientUser)
                .list();

        long totalCount = todayLogs.size();
        long errorCount = todayLogs.stream()
                .filter(l -> l.getResponseCode() != null && l.getResponseCode() != 200)
                .count();
        double avgResponseTime = todayLogs.stream()
                .filter(l -> l.getOperationSpend() != null)
                .mapToLong(SystemLog::getOperationSpend)
                .average()
                .orElse(0);
        long activeUserCount = todayLogs.stream()
                .map(SystemLog::getClientUser)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        double errorRate = totalCount > 0 ? (double) errorCount / totalCount * 100 : 0;

        return new SystemLogStatsResp(totalCount, errorCount,
                Math.round(avgResponseTime), activeUserCount,
                Math.round(errorRate * 100.0) / 100.0);
    }

    /**
     * 获取所有操作分组
     *
     * @return 操作分组列表
     */
    public List<String> getOperationGroups() {
        return lambdaQuery()
                .select(SystemLog::getOperationGroup)
                .isNotNull(SystemLog::getOperationGroup)
                .groupBy(SystemLog::getOperationGroup)
                .list()
                .stream()
                .map(SystemLog::getOperationGroup)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 清理指定时间范围内的日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 删除条数
     */
    public long cleanup(LocalDateTime startTime, LocalDateTime endTime) {
        var wrapper = lambdaQuery()
                .ge(startTime != null, SystemLog::getOperationTime, startTime)
                .le(endTime != null, SystemLog::getOperationTime, endTime);
        long count = wrapper.count();
        if (count > 0) {
            lambdaUpdate()
                    .ge(startTime != null, SystemLog::getOperationTime, startTime)
                    .le(endTime != null, SystemLog::getOperationTime, endTime)
                    .remove();
        }
        return count;
    }

    /**
     * 获取日趋势数据
     *
     * @param days 天数 (最大90)
     * @return 日趋势列表
     */
    public List<DashboardTrendItem> getDashboardTrends(int days) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        LocalDateTime start = LocalDateTime.of(LocalDate.now().minusDays(safeDays - 1), LocalTime.MIN);

        List<SystemLog> logs = lambdaQuery()
                .ge(SystemLog::getOperationTime, start)
                .select(SystemLog::getOperationTime, SystemLog::getResponseCode,
                        SystemLog::getOperationSpend, SystemLog::getClientUser)
                .list();

        Map<LocalDate, List<SystemLog>> grouped = logs.stream()
                .filter(l -> l != null && l.getOperationTime() != null)
                .collect(Collectors.groupingBy(l -> l.getOperationTime().toLocalDate()));

        List<DashboardTrendItem> result = new ArrayList<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = LocalDate.now().minusDays(safeDays - 1 - i);
            List<SystemLog> dayLogs = grouped.getOrDefault(date, List.of());

            long total = dayLogs.size();
            long errors = dayLogs.stream()
                    .filter(l -> l.getResponseCode() != null && l.getResponseCode() != 200)
                    .count();
            long activeUsers = dayLogs.stream()
                    .map(SystemLog::getClientUser)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .count();
            long avgResp = Math.round(dayLogs.stream()
                    .filter(l -> l.getOperationSpend() != null)
                    .mapToLong(SystemLog::getOperationSpend)
                    .average()
                    .orElse(0));

            result.add(new DashboardTrendItem(
                    date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    total, errors, activeUsers, avgResp));
        }
        return result;
    }

    /**
     * 获取今日小时分布
     *
     * @return 24小时操作分布
     */
    public List<DashboardHourlyItem> getHourlyDistribution() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<SystemLog> logs = lambdaQuery()
                .ge(SystemLog::getOperationTime, todayStart)
                .le(SystemLog::getOperationTime, todayEnd)
                .select(SystemLog::getOperationTime)
                .list();

        Map<Integer, Long> hourCounts = logs.stream()
                .filter(l -> l != null && l.getOperationTime() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getOperationTime().getHour(),
                        Collectors.counting()));

        List<DashboardHourlyItem> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(new DashboardHourlyItem(h, hourCounts.getOrDefault(h, 0L)));
        }
        return result;
    }

    /**
     * 获取用户活跃排名
     *
     * @param days  天数
     * @param limit 排名数量 (最大20)
     * @return 用户排名列表
     */
    public List<DashboardRankItem> getUserRanks(int days, int limit) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        LocalDateTime start = LocalDateTime.of(LocalDate.now().minusDays(safeDays - 1), LocalTime.MIN);

        List<SystemLog> logs = lambdaQuery()
                .ge(SystemLog::getOperationTime, start)
                .isNotNull(SystemLog::getClientUsername)
                .select(SystemLog::getClientUsername)
                .list();

        return logs.stream()
                .filter(Objects::nonNull)
                .filter(l -> StringUtils.hasText(l.getClientUsername()))
                .collect(Collectors.groupingBy(SystemLog::getClientUsername, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(safeLimit)
                .map(e -> new DashboardRankItem(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * 获取模块操作排名
     *
     * @param days  天数
     * @param limit 排名数量 (最大20)
     * @return 模块排名列表
     */
    public List<DashboardRankItem> getModuleRanks(int days, int limit) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        LocalDateTime start = LocalDateTime.of(LocalDate.now().minusDays(safeDays - 1), LocalTime.MIN);

        List<SystemLog> logs = lambdaQuery()
                .ge(SystemLog::getOperationTime, start)
                .isNotNull(SystemLog::getOperationGroup)
                .select(SystemLog::getOperationGroup)
                .list();

        return logs.stream()
                .filter(Objects::nonNull)
                .filter(l -> StringUtils.hasText(l.getOperationGroup()))
                .collect(Collectors.groupingBy(SystemLog::getOperationGroup, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(safeLimit)
                .map(e -> new DashboardRankItem(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * 获取最近操作
     *
     * @param limit 数量 (最大50)
     * @return 最近操作列表
     */
    public List<DashboardRecentItem> getRecentActivities(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        return lambdaQuery()
                .orderByDesc(SystemLog::getOperationTime)
                .select(SystemLog::getClientUsername, SystemLog::getOperationName,
                        SystemLog::getOperationGroup, SystemLog::getOperationTime,
                        SystemLog::getResponseCode, SystemLog::getOperationSpend)
                .last("LIMIT " + safeLimit)
                .list()
                .stream()
                .filter(Objects::nonNull)
                .map(l -> new DashboardRecentItem(
                        l.getClientUsername(), l.getOperationName(),
                        l.getOperationGroup(), l.getOperationTime(),
                        l.getResponseCode(), l.getOperationSpend()))
                .toList();
    }

    /**
     * 获取仪表板概览（聚合所有数据）
     *
     * @param days        趋势天数
     * @param rankLimit   排名数量
     * @param recentLimit 最近操作数量
     * @return 概览数据
     */
    public DashboardOverviewResp getOverview(int days, int rankLimit, int recentLimit) {
        return new DashboardOverviewResp(
                getTodayStats(),
                getDashboardTrends(days),
                getHourlyDistribution(),
                getUserRanks(days, rankLimit),
                getModuleRanks(days, rankLimit),
                getRecentActivities(recentLimit)
        );
    }

}
