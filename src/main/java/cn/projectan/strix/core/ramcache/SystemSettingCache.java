package cn.projectan.strix.core.ramcache;

import cn.projectan.strix.model.db.SystemSetting;
import cn.projectan.strix.service.SystemSettingService;
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
public class SystemSettingCache {

    @Autowired
    private SystemSettingService systemSettingService;

    private final Map<String, String> instance = new HashMap<>();

    @PostConstruct
    private void init() {
        List<SystemSetting> systemSettingList = systemSettingService.list();
        systemSettingList.forEach(ss -> instance.put(ss.getSettingKey(), ss.getSettingValue()));
        log.info(String.format("Strix Cache: 系统配置项加载完成, 加载了 %d 个配置项.", systemSettingList.size()));
    }

    public void update(String key) {
        SystemSetting systemSetting = systemSettingService.selectByKey(key);
        if (systemSetting != null) {
            instance.put(key, systemSetting.getSettingValue());
        } else {
            instance.remove(key);
        }
    }

    public String get(String key) {
        return instance.get(key);
    }

    public String get(String key, boolean secondConfirmation) {
        String result = instance.get(key);
        return result != null || !secondConfirmation ? result : updateAndGet(key);
    }

    public String updateAndGet(String key) {
        update(key);
        return get(key);
    }

}
