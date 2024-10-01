package cn.projectan.strix.service;

import cn.projectan.strix.model.db.PopularityConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-09-15
 */
public interface PopularityConfigService extends IService<PopularityConfig> {

    /**
     * 获取缓存
     *
     * @param key 缓存key
     * @return 缓存
     */
    PopularityConfig getCacheByKey(String key);

    /**
     * 清除缓存
     *
     * @param key 缓存key
     */
    void clearCache(String key);

}
