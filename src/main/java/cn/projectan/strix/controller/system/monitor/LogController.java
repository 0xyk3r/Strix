package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.monitor.log.SystemLogListReq;
import cn.projectan.strix.model.response.system.monitor.log.LogOperationGroupsResp;
import cn.projectan.strix.model.response.system.monitor.log.SystemLogListResp;
import cn.projectan.strix.model.response.system.monitor.log.SystemLogStatsResp;
import cn.projectan.strix.service.system.SystemLogService;
import cn.projectan.strix.util.common.I18nUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 系统操作日志
 *
 * @author ProjectAn
 * @since 2023/6/17 22:21
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/log")
@RequiredArgsConstructor
@Tag(name = "系统监控 - 操作日志")
public class LogController extends BaseSystemController {

    private final SystemLogService systemLogService;

    /**
     * 查询系统操作日志
     */
    @GetMapping()
    @PreAuthorize("@ss.hasPermission('system:monitor:log')")
    @StrixLog(operationGroup = "系统操作日志", operationName = "查询系统操作日志")
    @Operation(summary = "操作日志列表")
    public RetResult<SystemLogListResp> list(SystemLogListReq req) {
        try {
            Page<SystemLog> page = systemLogService.listPage(req);
            return RetBuilder.success(new SystemLogListResp(page.getRecords(), page.getTotal()));
        } catch (Exception e) {
            throw new StrixException(I18nUtil.get("error.log.serviceDisabled"));
        }
    }

    /**
     * 操作日志统计
     */
    @GetMapping("stats")
    @PreAuthorize("@ss.hasPermission('system:monitor:log')")
    @Operation(summary = "操作日志统计")
    public RetResult<SystemLogStatsResp> stats() {
        return RetBuilder.success(systemLogService.getTodayStats());
    }

    /**
     * 获取操作分组列表
     */
    @GetMapping("groups")
    @PreAuthorize("@ss.hasPermission('system:monitor:log')")
    @Operation(summary = "操作分组列表")
    public RetResult<LogOperationGroupsResp> operationGroups() {
        return RetBuilder.success(new LogOperationGroupsResp(systemLogService.getOperationGroups()));
    }

    /**
     * 清理日志
     */
    @DeleteMapping("cleanup")
    @PreAuthorize("@ss.hasPermission('system:monitor:log:delete')")
    @StrixLog(operationGroup = "系统操作日志", operationName = "清理操作日志", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "清理操作日志")
    public RetResult<Long> cleanup(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        long count = systemLogService.cleanup(startTime, endTime);
        return RetBuilder.success(count);
    }

}
