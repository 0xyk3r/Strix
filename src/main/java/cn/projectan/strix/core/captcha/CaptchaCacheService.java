package cn.projectan.strix.core.captcha;

/**
 * 验证码缓存接口
 *
 * @author ProjectAn
 * @since 2024-03-26
 */
public interface CaptchaCacheService {

    void set(String key, String value, long expiresInSeconds);

    boolean exists(String key);

    void delete(String key);

    String get(String key);

    /**
     * 缓存类型 local/redis/memcache/..
     *
     * @return 缓存类型
     */
    String type();

    /**
     * 自增
     *
     * @param key key
     * @param val 自增值
     * @return 自增后的值
     */
    default Long increment(String key, long val) {
        return 0L;
    }

}
