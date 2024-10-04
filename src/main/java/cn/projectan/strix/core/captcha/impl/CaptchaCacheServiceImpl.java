package cn.projectan.strix.core.captcha.impl;

import cn.projectan.strix.core.captcha.CaptchaCacheService;
import cn.projectan.strix.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 验证码缓存方案 (redis)
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
@Service
@RequiredArgsConstructor
public class CaptchaCacheServiceImpl implements CaptchaCacheService {

    private final RedisUtil redisUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        if (key.startsWith("strix:captcha:limit:")) {
            stringRedisTemplate.opsForValue().set(key, value, expiresInSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } else {
            redisUtil.set(key, value, expiresInSeconds);
        }
    }

    @Override
    public boolean exists(String key) {
        return redisUtil.hasKey(key);
    }

    @Override
    public void delete(String key) {
        redisUtil.del(key);
    }

    @Override
    public String get(String key) {
        Object o = redisUtil.get(key);
        return o == null ? null : o.toString();
    }

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public Long increment(String key, long val) {
        return redisUtil.incr(key, val);
    }

}
