package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.PopularityConfigMapper;
import cn.projectan.strix.model.db.PopularityConfig;
import cn.projectan.strix.service.PopularityConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-09-15
 */
@Service
public class PopularityConfigServiceImpl extends ServiceImpl<PopularityConfigMapper, PopularityConfig> implements PopularityConfigService {

    @Override
    @Cacheable(value = "strix:popularity:config", key = "#key")
    public PopularityConfig getPopularityConfig(String key) {
        return getBaseMapper().selectOne(
                new LambdaQueryWrapper<>(PopularityConfig.class)
                        .eq(PopularityConfig::getConfigKey, key)
        );

    }

    @Override
    @CacheEvict(value = "strix:popularity:config", key = "#key")
    public void clearCache(String key) {
    }

}
