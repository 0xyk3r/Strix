package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.PopularityConfigMapper;
import cn.projectan.strix.model.db.system.PopularityConfig;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * Strix 热度工具 配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-09-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularityConfigService extends ServiceImpl<PopularityConfigMapper, PopularityConfig> {

    /**
     * 获取缓存
     *
     * @param key 缓存key
     * @return 缓存
     */
    @Cacheable(value = "strix:popularity:config", key = "#key")
    public PopularityConfig getCacheByKey(String key) {
        return lambdaQuery()
                .eq(PopularityConfig::getConfigKey, key)
                .one();
    }

    /**
     * 查询配置列表（仅 id 和 name）
     *
     * @return 配置列表
     */
    public List<PopularityConfig> listAll() {
        return lambdaQuery()
                .select(PopularityConfig::getId, PopularityConfig::getName)
                .list();
    }

    /**
     * 清除缓存
     *
     * @param key 缓存key
     */
    @SuppressWarnings("EmptyMethod")
    @CacheEvict(value = "strix:popularity:config", key = "#key")
    public void clearCache(String key) {
    }

}
