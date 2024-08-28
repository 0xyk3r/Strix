package cn.projectan.strix.utils.tempurl;

import cn.hutool.core.lang.UUID;
import cn.projectan.strix.model.constant.RedisKeyConstants;
import cn.projectan.strix.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 临时 URL 工具类
 *
 * @author ProjectAn
 * @date 2024/8/15 18:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempUrlUtil {

    private final RedisUtil redisUtil;

    /**
     * 创建临时 URL
     *
     * @param url URL
     * @param ttl 过期时间
     * @return 临时 URL 的 key
     */
    public String create(String url, long ttl) {
        String key = UUID.fastUUID().toString();
        redisUtil.set(RedisKeyConstants.STR_TEMP_URL_PUBLIC_PREFIX + key, url, ttl);
        return key;
    }

    /**
     * 创建私有临时 URL
     *
     * @param url URL
     * @param uid 用户 ID
     * @param ttl 过期时间
     * @return 临时 URL 的 key
     */
    public String createSecret(String url, String uid, long ttl) {
        String key = UUID.fastUUID().toString();
        redisUtil.set(RedisKeyConstants.STR_TEMP_URL_PRIVATE_PREFIX + key + ":" + uid, url, ttl);
        return key;
    }

    /**
     * 获取临时 URL
     *
     * @param key key
     * @return URL
     */
    public String get(String key) {
        Object o = redisUtil.get(RedisKeyConstants.STR_TEMP_URL_PUBLIC_PREFIX + key);
        return Optional.ofNullable(o).map(Object::toString).orElse(null);
    }

    /**
     * 获取私有临时 URL
     *
     * @param key key
     * @param uid 用户 ID
     * @return URL
     */
    public String getSecret(String key, String uid) {
        Object o = redisUtil.get(RedisKeyConstants.STR_TEMP_URL_PRIVATE_PREFIX + key + ":" + uid);
        return Optional.ofNullable(o).map(Object::toString).orElse(null);
    }

}
