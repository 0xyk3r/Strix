package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemLogMapper;
import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.request.system.monitor.log.SystemLogListReq;
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
import java.util.List;

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

}
