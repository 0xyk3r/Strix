package cn.projectan.strix.core.captcha;

import cn.projectan.captcha.service.CaptchaCacheService;
import cn.projectan.strix.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author ProjectAn
 * @date 2022/9/30 15:13
 */
public class CaptchaCacheServiceImpl implements CaptchaCacheService {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
