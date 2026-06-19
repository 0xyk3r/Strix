package cn.projectan.strix.service.system;

import cn.projectan.strix.core.validation.annotation.DynamicDictValue;
import cn.projectan.strix.mapper.system.DictUsageStatMapper;
import cn.projectan.strix.model.db.system.DictUsageStat;
import cn.projectan.strix.model.response.system.dict.DictUsageStatsResp;
import cn.projectan.strix.util.common.RedisUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字典使用统计服务
 * <p>
 * 静态扫描: 启动时扫描 @DynamicDictValue 注解使用位置
 * 运行时: Redis HINCRBY 计数每次字典查询
 *
 * @author ProjectAn
 * @since 2026-04-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictUsageStatService extends ServiceImpl<DictUsageStatMapper, DictUsageStat> {

    private static final String REDIS_ACCESS_COUNT_KEY = "strix:dict:accessCount";

    private final RedisUtil redisUtil;

    /**
     * 扫描并同步静态使用统计
     * 可由 StrixDictSyncInitializer 在启动时调用
     */
    public void scanAndSync() {
        log.info("开始扫描字典静态使用...");
        List<DictUsageStat> stats = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        scanDynamicDictValueAnnotations(stats, now);

        if (!stats.isEmpty()) {
            lambdaUpdate().remove();
            saveBatch(stats, 200);
        }

        log.info("字典静态使用扫描完成，发现 {} 处引用", stats.size());
    }

    /**
     * 获取指定字典的使用统计
     */
    public DictUsageStatsResp getStatsByDictKey(String dictKey) {
        List<DictUsageStat> usages = lambdaQuery()
                .eq(DictUsageStat::getDictKey, dictKey)
                .list();

        List<DictUsageStatsResp.UsageItem> staticUsages = usages.stream()
                .map(u -> new DictUsageStatsResp.UsageItem(u.getUsageType(), u.getUsageLocation()))
                .collect(Collectors.toList());

        long accessCount = getAccessCount(dictKey);

        return new DictUsageStatsResp(staticUsages, accessCount);
    }

    /**
     * 递增运行时访问计数
     */
    public void incrementAccess(String dictKey) {
        try {
            redisUtil.hIncr(REDIS_ACCESS_COUNT_KEY, dictKey, 1);
        } catch (Exception e) {
            log.debug("递增字典访问计数失败: {}", dictKey);
        }
    }

    /**
     * 获取运行时访问计数
     */
    public long getAccessCount(String dictKey) {
        try {
            Object val = redisUtil.hGet(REDIS_ACCESS_COUNT_KEY, dictKey);
            if (val instanceof Number n) {
                return n.longValue();
            }
            if (val instanceof String s) {
                return Long.parseLong(s);
            }
        } catch (Exception e) {
            log.debug("字典值转换为 Long 失败，返回默认值 0: {}", e.getMessage());
        }
        return 0;
    }

    private void scanDynamicDictValueAnnotations(List<DictUsageStat> stats, LocalDateTime now) {
        try {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

            Set<BeanDefinition> candidates = scanner.findCandidateComponents("cn.projectan.strix.model.request");
            for (BeanDefinition bd : candidates) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    for (Field field : clazz.getDeclaredFields()) {
                        DynamicDictValue ddv = field.getAnnotation(DynamicDictValue.class);
                        if (ddv != null) {
                            stats.add(new DictUsageStat()
                                    .setDictKey(ddv.dictName())
                                    .setUsageType("VALIDATION")
                                    .setUsageLocation(clazz.getSimpleName() + "." + field.getName())
                                    .setScannedAt(now));
                        }
                    }
                } catch (Exception e) {
                    log.debug("扫描类失败: {}", bd.getBeanClassName(), e);
                }
            }
        } catch (Exception e) {
            log.warn("扫描 @DynamicDictValue 注解失败", e);
        }
    }

}
