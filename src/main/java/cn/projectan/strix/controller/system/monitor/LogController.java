package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.SystemLog;
import cn.projectan.strix.model.enums.NumCategory;
import cn.projectan.strix.model.request.system.monitor.log.SystemLogListReq;
import cn.projectan.strix.model.response.system.monitor.log.SystemLogListResp;
import cn.projectan.strix.service.SystemLogService;
import cn.projectan.strix.utils.NumUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统操作日志
 *
 * @author ProjectAn
 * @date 2023/6/17 22:21
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/log")
@RequiredArgsConstructor
public class LogController extends BaseSystemController {

    private final SystemLogService systemLogService;

    /**
     * 查询系统操作日志
     */
    @GetMapping()
    @PreAuthorize("@ss.hasPermission('system:monitor:log')")
    @StrixLog(operationGroup = "系统操作日志", operationName = "查询系统操作日志")
    public RetResult<Object> list(SystemLogListReq req) {
        try {
            Page<SystemLog> page = systemLogService.lambdaQuery()
                    .eq(StringUtils.hasText(req.getOperationType()), SystemLog::getOperationType, req.getOperationType())
                    .eq(StringUtils.hasText(req.getOperationGroup()), SystemLog::getOperationGroup, req.getOperationGroup())
                    .eq(StringUtils.hasText(req.getKeyword()), SystemLog::getOperationName, req.getKeyword())
                    .eq(NumUtil.checkCategory(req.getResponseCode(), NumCategory.POSITIVE), SystemLog::getResponseCode, req.getResponseCode())
                    .orderByDesc(SystemLog::getOperationTime)
                    .page(req.getPage());
            return RetBuilder.success(new SystemLogListResp(page.getRecords(), page.getTotal()));
        } catch (Exception e) {
            return RetBuilder.error("Strix 日志服务未开启，无法查询日志");
        }
    }

}
