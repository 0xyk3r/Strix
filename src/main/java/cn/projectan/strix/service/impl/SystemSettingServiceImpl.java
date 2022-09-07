package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemSettingMapper;
import cn.projectan.strix.model.db.SystemSetting;
import cn.projectan.strix.service.SystemSettingService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-13
 */
@Service
public class SystemSettingServiceImpl extends ServiceImpl<SystemSettingMapper, SystemSetting> implements SystemSettingService {

    @Override
    public SystemSetting selectByKey(String key) {
        QueryWrapper<SystemSetting> systemSettingQueryWrapper = new QueryWrapper<>();
        systemSettingQueryWrapper.eq("setting_key", key);
        return getBaseMapper().selectOne(systemSettingQueryWrapper);
    }
}
