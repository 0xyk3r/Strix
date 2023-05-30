package cn.projectan.strix.core.ramcache;

import cn.projectan.strix.model.db.SystemConfig;
import cn.projectan.strix.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统设置缓存类
 *
 * @author 安炯奕
 * @date 2021/5/13 14:18
 */
@Slf4j
@Component
public class SystemConfigCache {

    @Autowired
    private SystemConfigService systemConfigService;

    private final Map<String, String> instance = new HashMap<>();

    @PostConstruct
    private void init() {
        List<SystemConfig> systemConfigList = systemConfigService.list();
        systemConfigList.forEach(ss -> instance.put(ss.getKey(), ss.getValue()));
        log.info(String.format("Strix Config: 系统配置项加载完成, 加载了 %d 个配置项.", systemConfigList.size()));
    }

    public void update(String key) {
        SystemConfig systemConfig = systemConfigService.getByKey(key);
        if (systemConfig != null) {
            instance.put(key, systemConfig.getValue());
        } else {
            instance.remove(key);
        }
    }

    public String get(String key) {
        return instance.get(key);
    }

    public String get(String key, boolean strict) {
        String result = instance.get(key);

        if (result == null || strict) {
            return updateAndGet(key);
        } else {
            return result;
        }
    }

    public String updateAndGet(String key) {
        update(key);
        return get(key);
    }

}
