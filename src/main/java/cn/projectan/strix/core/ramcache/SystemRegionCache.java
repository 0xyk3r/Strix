package cn.projectan.strix.core.ramcache;

import cn.projectan.strix.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 安炯奕
 * @date 2021/9/29 14:03
 */
@Slf4j
@Component
public class SystemRegionCache {

    @Autowired
    private RedisUtil redisUtil;

    public void refreshRedisCache() {
        redisUtil.delLike("strix:system:region:queryRegionById:*");
        redisUtil.delLike("strix:system:region:getChildrenIdList:*");
    }

    public void refreshRedisCacheById(String id) {
        redisUtil.del("strix:system:region:queryRegionById::" + id);
        redisUtil.del("strix:system:region:getChildrenIdList::" + id);
    }

}
