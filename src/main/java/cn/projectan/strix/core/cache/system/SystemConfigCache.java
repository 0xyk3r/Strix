package cn.projectan.strix.core.cache.system;

import cn.projectan.strix.model.db.system.SystemConfig;
import cn.projectan.strix.service.system.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 系统设置缓存
 * <p>
 * 使用 Spring @Cacheable 替代 ConcurrentHashMap, 缓存 TTL 由 RedisConfig 统一管理 (1h).
 * 缓存失效通过 ConfigChangedEvent → CacheEvictionService 触发.
 *
 * @author ProjectAn
 * @since 2021/5/13 14:18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemConfigCache {

    private final SystemConfigService systemConfigService;

    /**
     * 获取配置值 (Redis 缓存, TTL 1h)
     *
     * @param key 配置键
     * @return 配置值, 不存在返回 null
     */
    @Cacheable(value = "strix:config", key = "#key", unless = "#result == null")
    public String get(String key) {
        SystemConfig config = systemConfigService.getByKey(key);
        return config != null ? config.getValue() : null;
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
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

    /**
     * 清除指定配置项缓存 (由 CacheEvictionService 调用)
     */
    @CacheEvict(value = "strix:config", key = "#key")
    public void evict(String key) {
        log.debug("配置缓存已清除: key={}", key);
    }
}
