package cn.projectan.strix.utils;

import cn.projectan.strix.model.constant.PopularityConstants;
import cn.projectan.strix.model.db.PopularityConfig;
import cn.projectan.strix.model.db.PopularityData;
import cn.projectan.strix.service.PopularityConfigService;
import cn.projectan.strix.service.PopularityDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 热度工具类
 *
 * @author ProjectAn
 * @date 2023/9/15 16:59
 */
@Component
@RequiredArgsConstructor
public class PopularityUtil {

    private final PopularityConfigService popularityConfigService;
    private final PopularityDataService popularityDataService;
    private final RedisUtil redisUtil;

    public void addPopularity(String popularityDataType, String popularityDataId) {
        redisUtil.incr(PopularityConstants.POPULARITY_DATA_REDIS_KEY_PREFIX + popularityDataType + "::" + popularityDataId);
    }

    public String getPopularity(String popularityDataType, String popularityDataId) {
        PopularityConfig config = popularityConfigService.getPopularityConfig(popularityDataType);

        String popularity = getPopularityFormRedis(popularityDataType, popularityDataId);
        if (config != null) {
            // 计算初始数值 + 原始值
            BigDecimal initialCalc = new BigDecimal(config.getInitialValue().toString()).add(new BigDecimal(popularity));
            // 乘算倍数
            BigDecimal magCalc = initialCalc.multiply(config.getMagValue());
            // 加算额外值
            BigDecimal extraCalc = magCalc.add(new BigDecimal(config.getExtraValue().toString()));
            // 只保留整数
            return extraCalc.setScale(0, RoundingMode.UP).toString();
        } else {
            return popularity;
        }
    }

    public void saveToDatabase() {
        List<PopularityData> list = redisUtil.scan(PopularityConstants.POPULARITY_DATA_REDIS_KEY_PREFIX + "*").stream().map(key -> {
            String[] split = key.replace(PopularityConstants.POPULARITY_DATA_REDIS_KEY_PREFIX, "").split("::");
            Object o = redisUtil.get(key);
            int value = o == null ? 0 : Integer.parseInt(o.toString());
            return new PopularityData(split[0], split[1], value);
        }).toList();
        List<PopularityData> dbList = popularityDataService.lambdaQuery()
                .select(PopularityData::getId, PopularityData::getConfigKey, PopularityData::getDataId, PopularityData::getOriginalValue)
                .list();
        // 找出需要新增的数据
        List<PopularityData> insertList = list.stream().filter(popularityData -> {
            for (PopularityData data : dbList) {
                if (data.getConfigKey().equals(popularityData.getConfigKey()) && data.getDataId().equals(popularityData.getDataId())) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        // 找出需要更新的数据
        List<PopularityData> updateList = list.stream().filter(popularityData -> {
            for (PopularityData data : dbList) {
                if (data.getConfigKey().equals(popularityData.getConfigKey()) && data.getDataId().equals(popularityData.getDataId())) {
                    popularityData.setId(data.getId());
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());

        popularityDataService.saveBatch(insertList);
        popularityDataService.updateBatchById(updateList);
    }

    public void loadFromDatabase(boolean force) {
        Set<String> redisList = redisUtil.scan(PopularityConstants.POPULARITY_DATA_REDIS_KEY_PREFIX + "*");
        if (!redisList.isEmpty() && !force) {
            // 如果redis中已经有数据了，就不从数据库中加载了
            return;
        }
        List<PopularityData> dbList = popularityDataService.list();
        dbList.forEach(popularityData -> redisUtil.set(PopularityConstants.POPULARITY_DATA_REDIS_KEY_PREFIX + popularityData.getConfigKey() + "::" + popularityData.getDataId(), popularityData.getOriginalValue()));
    }

    private String getPopularityFormRedis(String popularityDataType, String popularityDataId) {
        Object o = redisUtil.get(PopularityConstants.POPULARITY_DATA_REDIS_KEY_PREFIX + popularityDataType + "::" + popularityDataId);
        return o == null ? "0" : o.toString();
    }

}
