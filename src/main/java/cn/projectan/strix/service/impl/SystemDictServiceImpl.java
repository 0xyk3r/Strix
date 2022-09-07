package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemDictMapper;
import cn.projectan.strix.model.db.SystemDict;
import cn.projectan.strix.service.SystemDictService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-08-31
 */
@Service
public class SystemDictServiceImpl extends ServiceImpl<SystemDictMapper, SystemDict> implements SystemDictService {

    @Override
    public String getDict(String key) {
        QueryWrapper<SystemDict> systemDictQueryWrapper = new QueryWrapper<>();
        systemDictQueryWrapper.eq("dict_key", key);
        SystemDict dict = getBaseMapper().selectOne(systemDictQueryWrapper);
        return dict != null ? dict.getDictValue() : null;
    }

    @Override
    public void putDict(String key, String value) {
        this.putDict(key, value, null);
    }

    @Override
    public void putDict(String key, String value, String updateBy) {
        if (!StringUtils.hasText(updateBy)) {
            updateBy = "unknown";
        }
        QueryWrapper<SystemDict> systemDictQueryWrapper = new QueryWrapper<>();
        systemDictQueryWrapper.eq("dict_key", key);
        SystemDict dict = getBaseMapper().selectOne(systemDictQueryWrapper);
        if (dict != null) {
            dict.setDictValue(value);
            dict.setUpdateBy(updateBy);
            getBaseMapper().updateById(dict);
        } else {
            dict = new SystemDict();
            dict.setDictKey(key);
            dict.setDictValue(value);
            dict.setCreateBy(updateBy);
            dict.setUpdateBy(updateBy);
            getBaseMapper().insert(dict);
        }
    }

}
