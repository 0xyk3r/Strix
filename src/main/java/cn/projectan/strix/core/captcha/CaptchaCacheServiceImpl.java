package cn.projectan.strix.core.captcha;

import cn.projectan.strix.utils.RedisUtil;
import com.anji.captcha.service.CaptchaCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author 安炯奕
 * @date 2022/9/30 15:13
 */
public class CaptchaCacheServiceImpl implements CaptchaCacheService {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String redisKeyHandler(String key) {
        key = key.toLowerCase();
        key = key.replace("aj.captcha.req.limit-get-", "strix:captcha:limit:get:");
        key = key.replace("aj.captcha.req.limit-check-", "strix:captcha:limit:check:");
        key = key.replace("aj.captcha.req.limit-fail-", "strix:captcha:limit:fail:");
        key = key.replace("running:captcha", "strix:captcha:running");
        return key;
    }

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        key = redisKeyHandler(key);
        if (key.startsWith("strix:captcha:limit:")) {
            stringRedisTemplate.opsForValue().set(key, value, expiresInSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } else {
            redisUtil.set(key, value, expiresInSeconds);
        }
    }

    @Override
    public boolean exists(String key) {
        key = redisKeyHandler(key);
        return redisUtil.hasKey(key);
    }

    @Override
    public void delete(String key) {
        key = redisKeyHandler(key);
        redisUtil.del(key);
    }

    @Override
    public String get(String key) {
        key = redisKeyHandler(key);
        Object o = redisUtil.get(key);
        return o == null ? null : o.toString();
    }

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public Long increment(String key, long val) {
        key = redisKeyHandler(key);
        return redisUtil.incr(key, val);
    }

}
