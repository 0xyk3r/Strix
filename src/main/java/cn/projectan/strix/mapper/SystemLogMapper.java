package cn.projectan.strix.mapper;

import cn.projectan.strix.model.db.SystemLog;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * sys_system_log Mapper 接口
 * </p>
 *
 * @author ProjectAn
 * @since 2023-06-16
 */
@DS("clickhouse")
public interface SystemLogMapper extends BaseMapper<SystemLog> {

}
