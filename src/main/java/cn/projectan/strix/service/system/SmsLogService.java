package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SmsLogMapper;
import cn.projectan.strix.model.db.system.SmsLog;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.request.system.module.sms.SmsLogListReq;
import cn.projectan.strix.util.math.NumUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Strix SMS 日志 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsLogService extends ServiceImpl<SmsLogMapper, SmsLog> {

    /**
     * 分页查询短信日志列表
     *
     * @param req 查询请求
     * @return 分页数据
     */
    public Page<SmsLog> listPage(SmsLogListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SmsLog::getPhoneNumber, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.POSITIVE), SmsLog::getStatus, req.getStatus())
                .eq(StringUtils.hasText(req.getConfigKey()), SmsLog::getConfigKey, req.getConfigKey())
                .page(req.getPage());
    }

}
