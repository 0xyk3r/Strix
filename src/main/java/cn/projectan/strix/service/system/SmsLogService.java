package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SmsLogMapper;
import cn.projectan.strix.model.db.system.SmsLog;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

}
