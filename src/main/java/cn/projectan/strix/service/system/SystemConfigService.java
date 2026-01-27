package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemConfigMapper;
import cn.projectan.strix.model.db.system.SystemConfig;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix 系统配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    /**
     * 根据key查询系统配置项
     *
     * @param key 配置项key
     * @return 系统配置项
     */
    public SystemConfig getByKey(String key) {
        return lambdaQuery()
                .eq(SystemConfig::getKey, key)
                .one();
    }

}
