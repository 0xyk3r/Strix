package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemLogMapper;
import cn.projectan.strix.model.db.SystemLog;
import cn.projectan.strix.service.SystemLogService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * sys_system_log 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-06-16
 */
@Service
@DS("clickhouse")
public class SystemLogServiceImpl extends ServiceImpl<SystemLogMapper, SystemLog> implements SystemLogService {

}
