package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemConfigMapper;
import cn.projectan.strix.model.db.SystemConfig;
import cn.projectan.strix.service.SystemConfigService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-13
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    @Override
    public SystemConfig getByKey(String key) {
        QueryWrapper<SystemConfig> systemSettingQueryWrapper = new QueryWrapper<>();
        systemSettingQueryWrapper.eq("key", key);
        return getBaseMapper().selectOne(systemSettingQueryWrapper);
    }
}
