package cn.projectan.strix.core.captcha.impl;

import cn.projectan.strix.core.captcha.CaptchaCacheService;
import cn.projectan.strix.core.captcha.CaptchaService;
import cn.projectan.strix.model.properties.system.StrixCaptchaProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 验证码服务工厂
 * <p>
 * 由 {@code StrixCaptchaConfig} 在 Spring 上下文初始化时调用 {@link #init} 注册实例。
 *
 * @author ProjectAn
 * @since 2024-03-26
 */
public class CaptchaServiceFactory {

    private static final Map<String, CaptchaService> instances = new HashMap<>();
    private static final Map<String, CaptchaCacheService> cacheService = new HashMap<>();
    private static volatile boolean initialized = false;

    private CaptchaServiceFactory() {
    }

    /**
     * 初始化工厂（由 Spring 配置类调用）
     *
     * @param redisCache Redis 缓存实现
     */
    public static synchronized void init(CaptchaCacheServiceImpl redisCache) {
        if (initialized) {
            return;
        }
        cacheService.put("redis", redisCache);
        cacheService.put("local", new CaptchaLocalCacheServiceImpl());
        instances.put("blockPuzzle", new BlockPuzzleCaptchaServiceImpl());
        initialized = true;
    }

    public static CaptchaService getInstance(Properties config) {
        if (!initialized) {
            throw new IllegalStateException("CaptchaServiceFactory has not been initialized. Ensure StrixCaptchaConfig is loaded.");
        }
        String captchaType = config.getProperty(StrixCaptchaProperties.Key.CAPTCHA_TYPE, "blockPuzzle");
        CaptchaService ret = instances.get(captchaType);
        if (ret == null) {
            throw new IllegalArgumentException("Unsupported captcha type: " + captchaType);
        }
        ret.init(config);
        return ret;
    }

    public static CaptchaCacheService getCache(String cacheType) {
        if (!initialized) {
            throw new IllegalStateException("CaptchaServiceFactory has not been initialized. Ensure StrixCaptchaConfig is loaded.");
        }
        return cacheService.get(cacheType);
    }
}
