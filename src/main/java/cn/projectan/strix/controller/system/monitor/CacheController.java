package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.constant.monitor.CacheConstants;
import cn.projectan.strix.model.other.monitor.cache.SystemCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author 安炯奕
 * @date 2022/9/30 22:13
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/cache")
public class CacheController extends BaseSystemController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final static List<SystemCache> caches = new ArrayList<>();

    static {
        caches.add(new SystemCache(CacheConstants.LOGIN_TOKEN_KEY, "用户信息"));
        caches.add(new SystemCache(CacheConstants.SYS_CONFIG_KEY, "配置信息"));
        caches.add(new SystemCache(CacheConstants.SYS_DICT_KEY, "数据字典"));
        caches.add(new SystemCache(CacheConstants.CAPTCHA_CODE_KEY, "验证码"));
        caches.add(new SystemCache(CacheConstants.REPEAT_SUBMIT_KEY, "防重提交"));
        caches.add(new SystemCache(CacheConstants.RATE_LIMIT_KEY, "限流处理"));
        caches.add(new SystemCache(CacheConstants.PWD_ERR_CNT_KEY, "密码错误次数"));
    }

    @GetMapping()
    @PreAuthorize("@ss.hasRead('System_Monitor_Cache')")
    public RetResult<Object> getCacheInfo() {
        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) RedisServerCommands::info);
        Properties commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.serverCommands().info("commandstats"));
        Object dbSize = redisTemplate.execute((RedisCallback<Object>) RedisServerCommands::dbSize);

        Map<String, Object> result = new HashMap<>(3);
        result.put("info", info);
        result.put("dbSize", dbSize);

        List<Map<String, String>> pieList = new ArrayList<>();
        commandStats.stringPropertyNames().forEach(key -> {
            Map<String, String> data = new HashMap<>(2);
            String property = commandStats.getProperty(key);
            data.put("name", StringUtils.removeStart(key, "cmdstat_"));
            data.put("value", StringUtils.substringBetween(property, "calls=", ",usec"));
            pieList.add(data);
        });
        result.put("commandStats", pieList);
        return RetMarker.makeSuccessRsp(result);
    }

    @GetMapping("names")
    @PreAuthorize("@ss.hasWrite('System_Monitor_Cache')")
    public RetResult<Object> getCacheNames() {
        return RetMarker.makeSuccessRsp(Collections.singletonMap("names", caches));
    }

    @GetMapping("keys/{cacheName}")
    @PreAuthorize("@ss.hasWrite('System_Monitor_Cache')")
    public RetResult<Object> getCacheKeys(@PathVariable String cacheName) {
        Set<String> cacheKeys = redisTemplate.keys(cacheName + "*");
        return RetMarker.makeSuccessRsp(Collections.singletonMap("cacheKeys", cacheKeys));
    }

    @GetMapping("value/{cacheName}/{cacheKey}")
    @PreAuthorize("@ss.hasWrite('System_Monitor_Cache')")
    public RetResult<Object> getCacheValue(@PathVariable String cacheName, @PathVariable String cacheKey) {
        String cacheValue = (String) redisTemplate.opsForValue().get(cacheKey);
        SystemCache cache = new SystemCache(cacheName, cacheKey, cacheValue);
        return RetMarker.makeSuccessRsp(Collections.singletonMap("cache", cache));
    }

    @DeleteMapping("clear/{cacheName}")
    @PreAuthorize("@ss.hasWrite('System_Monitor_Cache')")
    public RetResult<Object> clearCacheKeys(@PathVariable String cacheName) {
        Collection<String> cacheKeys = redisTemplate.keys(cacheName + "*");
        if (cacheKeys != null && cacheKeys.size() > 0) {
            redisTemplate.delete(cacheKeys);
        }
        return RetMarker.makeSuccessRsp();
    }

    @DeleteMapping("remove/{cacheKey}")
    @PreAuthorize("@ss.hasWrite('System_Monitor_Cache')")
    public RetResult<Object> clearCacheKey(@PathVariable String cacheKey) {
        redisTemplate.delete(cacheKey);
        return RetMarker.makeSuccessRsp();
    }

    @DeleteMapping("clear")
    @PreAuthorize("@ss.hasWrite('System_Monitor_Cache')")
    public RetResult<Object> clearCacheAll() {
        Collection<String> cacheKeys = redisTemplate.keys("*");
        if (cacheKeys != null && cacheKeys.size() > 0) {
            redisTemplate.delete(cacheKeys);
        }
        return RetMarker.makeSuccessRsp();
    }

}
