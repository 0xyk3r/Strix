package cn.projectan.strix.controller.system.monitor;

import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 系统缓存信息
 *
 * @author ProjectAn
 * @since 2022/9/30 22:13
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/cache")
@RequiredArgsConstructor
public class CacheController extends BaseSystemController {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 查询系统缓存信息
     */
    @GetMapping()
    @PreAuthorize("@ss.hasPermission('system:monitor:cache')")
    @StrixLog(operationGroup = "系统缓存信息", operationName = "查询系统缓存信息")
    public RetResult<Object> getCacheInfo() {
        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) RedisServerCommands::info);
        Properties commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.serverCommands().info("commandstats"));
        Object dbSize = redisTemplate.execute((RedisCallback<Object>) RedisServerCommands::dbSize);

        Map<String, Object> result = Map.of(
                "info", info,
                "dbSize", dbSize,
                "commandStats", Optional.ofNullable(commandStats)
                        .map(stats -> stats.stringPropertyNames().stream()
                                .filter(key -> !key.equals("cmdstat_ping"))
                                .map(key -> Map.of(
                                        "name", StrUtil.removePrefix(key, "cmdstat_"),
                                        "value", StrUtil.subBetween(stats.getProperty(key), "calls=", ",usec")
                                ))
                                .sorted((a, b) -> Integer.parseInt(b.get("value")) - Integer.parseInt(a.get("value")))
                                .limit(10)
                                .collect(Collectors.toList()))
                        .orElse(Collections.emptyList())
        );
        return RetBuilder.success(result);
    }

}
