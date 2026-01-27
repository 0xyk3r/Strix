package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemLogMapper;
import cn.projectan.strix.model.db.system.SystemLog;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

}
