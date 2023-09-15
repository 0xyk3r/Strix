package cn.projectan.strix.utils;

import cn.projectan.strix.model.constant.PopularityConstants;
import cn.projectan.strix.model.db.PopularityConfig;
import cn.projectan.strix.service.PopularityConfigService;
import cn.projectan.strix.service.PopularityDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 热度工具类
 *
 * @author 安炯奕
 * @date 2023/9/15 16:59
 */
@Component
@RequiredArgsConstructor
public class PopularityUtil {

    private final PopularityConfigService popularityConfigService;
    private final PopularityDataService popularityDataService;
    private final RedisUtil redisUtil;

    private String getPopularityFormRedis(String popularityDataType, String popularityDataId) {
        Object o = redisUtil.get(PopularityConstants.POPULARITY_REDIS_KEY_PREFIX + popularityDataType + "::" + popularityDataId);
        return o == null ? "0" : o.toString();
    }

    public void addPopularity(String popularityDataType, String popularityDataId) {
        redisUtil.incr(PopularityConstants.POPULARITY_REDIS_KEY_PREFIX + popularityDataType + "::" + popularityDataId, 1);
    }

    public String getPopularity(String popularityDataType, String popularityDataId) {
        PopularityConfig one = popularityConfigService.lambdaQuery().eq(PopularityConfig::getDataType, popularityDataType).one();

        String popularity = getPopularityFormRedis(popularityDataType, popularityDataId);
        if (one != null) {
            double c1 = Arithmetic.add(Double.parseDouble(popularity), one.getExtraValue().doubleValue());
            double c2 = Arithmetic.mul(c1, one.getMagValue());
            String str = String.valueOf(Math.floor(c2));
            return str.substring(0, str.indexOf("."));
        } else {
            return popularity;
        }
    }

}
