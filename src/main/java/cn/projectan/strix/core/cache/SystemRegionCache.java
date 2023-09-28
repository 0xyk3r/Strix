package cn.projectan.strix.core.cache;

import cn.projectan.strix.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author 安炯奕
 * @date 2021/9/29 14:03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemRegionCache {

    private final RedisUtil redisUtil;

    public void refreshRedisCache() {
        redisUtil.delKeys("strix:system:region:queryRegionById:*");
        redisUtil.delKeys("strix:system:region:getChildrenIdList:*");
    }

    public void refreshRedisCacheById(String id) {
        if (StringUtils.hasText(id)) {
            redisUtil.del("strix:system:region:queryRegionById::" + id);
            redisUtil.del("strix:system:region:getChildrenIdList::" + id);
        }
    }

}
