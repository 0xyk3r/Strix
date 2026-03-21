package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemLogMapper;
import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.request.system.monitor.log.SystemLogListReq;
import cn.projectan.strix.util.math.NumUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
                .eq(StringUtils.hasText(req.getKeyword()), SystemLog::getOperationName, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getResponseCode(), NumCategory.POSITIVE), SystemLog::getResponseCode, req.getResponseCode())
                .orderByDesc(SystemLog::getOperationTime)
                .page(req.getPage());
    }

}
