package cn.projectan.strix.service;

import cn.projectan.strix.model.db.PopularityConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-09-15
 */
public interface PopularityConfigService extends IService<PopularityConfig> {

    PopularityConfig getPopularityConfig(String key);

    void clearCache(String key);

}
