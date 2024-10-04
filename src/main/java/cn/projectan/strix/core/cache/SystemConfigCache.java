package cn.projectan.strix.core.cache;

import cn.projectan.strix.model.db.SystemConfig;
import cn.projectan.strix.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 系统设置缓存
 *
 * @author ProjectAn
 * @since 2021/5/13 14:18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemConfigCache {

    private final SystemConfigService systemConfigService;

    private final Map<String, String> instance = new HashMap<>();

    @PostConstruct
    private void init() {
        List<SystemConfig> systemConfigList = systemConfigService.list();
        systemConfigList.forEach(ss -> instance.put(ss.getKey(), ss.getValue()));
        log.info("Strix Cache: 系统配置项加载完成, 加载了 {} 个配置项.", systemConfigList.size());
    }

    /**
     * 更新缓存
     *
     * @param key key
     */
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

    public String get(String key, String defaultValue) {
        return instance.getOrDefault(key, defaultValue);

    }

    private <T> T get(String key, Function<String, T> parser, T defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return parser.apply(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String key) {
        return get(key, Boolean::parseBoolean, null);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return get(key, Boolean::parseBoolean, defaultValue);
    }

    public Long getLong(String key) {
        return get(key, Long::parseLong, null);
    }

    public Long getLong(String key, Long defaultValue) {
        return get(key, Long::parseLong, defaultValue);
    }

}
