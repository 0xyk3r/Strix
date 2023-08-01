package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.SystemLog;
import cn.projectan.strix.model.request.system.monitor.log.SystemLogListReq;
import cn.projectan.strix.model.response.system.monitor.log.SystemLogListResp;
import cn.projectan.strix.service.SystemLogService;
import cn.projectan.strix.utils.NumUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 安炯奕
 * @date 2023/6/17 22:21
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/log")
@RequiredArgsConstructor
public class LogController extends BaseSystemController {

    private final SystemLogService systemLogService;

    @GetMapping()
    @PreAuthorize("@ss.hasPermission('system:monitor:log')")
    @StrixLog(operationGroup = "系统操作日志", operationName = "查询系统操作日志")
    public RetResult<Object> list(SystemLogListReq req) {
        LambdaQueryWrapper<SystemLog> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(req.getOperationType())) {
            queryWrapper.eq(SystemLog::getOperationType, req.getOperationType());
        }
        if (StringUtils.hasText(req.getOperationGroup())) {
            queryWrapper.eq(SystemLog::getOperationGroup, req.getOperationGroup());
        }
        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.eq(SystemLog::getOperationName, req.getKeyword());
        }
        if (NumUtils.isPositiveNumber(req.getResponseCode())) {
            queryWrapper.eq(SystemLog::getResponseCode, req.getResponseCode());
        }

        queryWrapper.orderByDesc(SystemLog::getOperationTime);
        Page<SystemLog> page = systemLogService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(new SystemLogListResp(page.getRecords(), page.getTotal()));
    }

}
