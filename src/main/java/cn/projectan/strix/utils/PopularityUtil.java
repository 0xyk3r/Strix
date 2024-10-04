package cn.projectan.strix.utils;

import cn.projectan.strix.model.constant.RedisKeyConstants;
import cn.projectan.strix.model.db.PopularityConfig;
import cn.projectan.strix.model.db.PopularityData;
import cn.projectan.strix.service.PopularityConfigService;
import cn.projectan.strix.service.PopularityDataService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 热度工具类
 *
 * @author ProjectAn
 * @since 2023/9/15 16:59
 */
@Slf4j
@Component
public class PopularityUtil {

    private final PopularityConfigService popularityConfigService;
    private final PopularityDataService popularityDataService;
    private final RedisUtil redisUtil;

    public PopularityUtil(PopularityConfigService popularityConfigService, PopularityDataService popularityDataService, RedisUtil redisUtil) {
        this.popularityConfigService = popularityConfigService;
        this.popularityDataService = popularityDataService;
        this.redisUtil = redisUtil;

        // 从数据库同步数据到redis
        // 构造方法中初始化数据, 会影响启动时间, 但可以保证数据一定被加载
        syncFormDB(false);
        log.info("Strix PopularityUtil: 载入数据完成.");
    }

    @PreDestroy
    public void destroy() {
        // 关机前保存热度数据到数据库
        log.info("Strix PopularityUtil: 持久化数据中, 强制关闭程序会导致数据丢失.");
        syncToDB();
    }

    /**
     * 获取原始值
     *
     * @param configKey 配置key
     * @param dataId    数据id
     * @return 原始值
     */
    private long getOriginalValue(String configKey, String dataId) {
        Long value = redisUtil.zGet(RedisKeyConstants.HASH_POPULARITY_DATA_PREFIX + configKey, dataId);
        return value == null ? 0L : value;
    }

    /**
     * 获取热度值
     *
     * @param configKey 配置key
     * @param dataId    数据id
     * @return 热度值
     */
    public long get(String configKey, String dataId) {
        PopularityConfig config = popularityConfigService.getCacheByKey(configKey);

        long originalValue = getOriginalValue(configKey, dataId);
        if (config != null) {
            BigDecimal originalCalc = new BigDecimal(Double.toString(originalValue));
            // 加算初始数值
            BigDecimal initialCalc = new BigDecimal(config.getInitialValue().toString()).add(originalCalc);
            // 乘算倍数
            BigDecimal magCalc = initialCalc.multiply(config.getMagValue());
            // 加算额外值
            BigDecimal extraCalc = magCalc.add(new BigDecimal(config.getExtraValue().toString()));
            // 只保留整数
            return extraCalc.setScale(0, RoundingMode.UP).longValue();
        } else {
            // 未配置的情况下直接返回原始值
            return originalValue;
        }
    }

    /**
     * 热度 + 1
     *
     * @param configKey 配置key
     * @param dataId    数据id
     */
    public void incr(String configKey, String dataId) {
        redisUtil.zIncr(RedisKeyConstants.HASH_POPULARITY_DATA_PREFIX + configKey, dataId);
    }

    /**
     * 从redis同步数据到数据库
     * <br>性能在数据量大时可能存在问题, 待优化
     */
    public void syncToDB() {
        List<PopularityData> addDataList = new ArrayList<>();
        List<PopularityData> updateDataList = new ArrayList<>();
        // 从 redis 中扫描所有 ConfigKey (为了支持未配置的数据)
        Set<String> redisConfigKeys = redisUtil.scan(RedisKeyConstants.HASH_POPULARITY_DATA_PREFIX + "*");
        List<String> configKeyList = redisConfigKeys.stream()
                .map(key -> key.replace(RedisKeyConstants.HASH_POPULARITY_DATA_PREFIX, ""))
                .toList();
        // 从数据库加载配置列表
        List<String> dbConfigKeyList = popularityConfigService.lambdaQuery()
                .select(PopularityConfig::getConfigKey)
                .list().stream().map(PopularityConfig::getConfigKey).toList();
        // 配置差异处理
        KeyDiffUtil.handle(dbConfigKeyList, configKeyList,
                (removeKeys) -> {
                    // do nothing
                },
                (addKeys) -> {
                    List<PopularityConfig> addConfigList = addKeys.stream().map(key ->
                            new PopularityConfig()
                                    .setConfigKey(key)
                                    .setName(key)
                                    .setInitialValue(0)
                                    .setMagValue(BigDecimal.ONE)
                                    .setExtraValue(0)
                                    .setCreateBy("SYNC")
                                    .setUpdateBy("SYNC")
                    ).toList();
                    popularityConfigService.saveBatch(addConfigList);
                }
        );
        // 数据检索 & 差异处理
        for (String key : configKeyList) {
            List<PopularityData> dbDataList = popularityDataService.lambdaQuery()
                    .eq(PopularityData::getConfigKey, key)
                    .select(PopularityData::getId, PopularityData::getDataId, PopularityData::getOriginalValue)
                    .list();
            List<PopularityData> redisDataList = redisUtil.zGet(RedisKeyConstants.HASH_POPULARITY_DATA_PREFIX + key).stream()
                    .map(typedTuple -> {
                        if (typedTuple == null || typedTuple.getValue() == null || typedTuple.getScore() == null) {
                            return null;
                        }
                        return new PopularityData()
                                .setConfigKey(key)
                                .setDataId(typedTuple.getValue().toString())
                                .setOriginalValue(typedTuple.getScore().longValue())
                                .setCreateBy("SYNC")
                                .setUpdateBy("SYNC");
                    }).toList();
            // 数据差异处理
            addDataList.addAll(
                    redisDataList.stream()
                            .filter(redisData -> dbDataList.stream().noneMatch(dbData -> dbData.getDataId().equals(redisData.getDataId())))
                            .toList()
            );
            updateDataList.addAll(
                    redisDataList.stream()
                            .filter(redisData -> dbDataList.stream().anyMatch(dbData -> dbData.getDataId().equals(redisData.getDataId())))
                            .peek(data -> data.setId(dbDataList.stream().filter(dbData -> dbData.getDataId().equals(data.getDataId())).findFirst().get().getId()))
                            .toList()
            );
        }
        // 数据库操作
        popularityDataService.saveBatch(addDataList);
        popularityDataService.updateBatchById(updateDataList);
    }

    /**
     * 从数据库同步数据到redis
     *
     * @param force 是否强制同步
     */
    public void syncFormDB(boolean force) {
        // 加载配置列表
        List<String> configKeyList = popularityConfigService.lambdaQuery()
                .select(PopularityConfig::getConfigKey)
                .list().stream().map(PopularityConfig::getConfigKey).toList();
        for (String key : configKeyList) {
            boolean isExistInRedis = redisUtil.isType(RedisKeyConstants.HASH_POPULARITY_DATA_PREFIX + key, DataType.HASH);
            if (isExistInRedis && !force) {
                // 如果redis中已经有数据了，就不从数据库中加载了
                continue;
            }
            List<PopularityData> dataList = popularityDataService.lambdaQuery()
                    .eq(PopularityData::getConfigKey, key)
                    .select(PopularityData::getDataId, PopularityData::getOriginalValue)
                    .list();
            Set<ZSetOperations.TypedTuple<Object>> tuples = dataList.stream()
                    .map(data ->
                            new DefaultTypedTuple<Object>(data.getDataId(), data.getOriginalValue().doubleValue())
                    ).collect(Collectors.toSet());
            redisUtil.zSet(RedisKeyConstants.HASH_POPULARITY_DATA_PREFIX + key, tuples);
        }
    }

}
