package cn.projectan.strix.core.captcha.impl;

import cn.projectan.strix.core.captcha.CaptchaCacheService;
import cn.projectan.strix.core.captcha.CaptchaService;
import cn.projectan.strix.model.properties.system.StrixCaptchaProperties;
import cn.projectan.strix.util.common.SpringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 验证码服务工厂
 *
 * @author ProjectAn
 * @since 2024-03-26
 */
public class CaptchaServiceFactory {

    public static CaptchaService getInstance(Properties config) {
        String captchaType = config.getProperty(StrixCaptchaProperties.Key.CAPTCHA_TYPE, "blockPuzzle");
        CaptchaService ret = instances.get(captchaType);
        if (ret == null) {
            throw new RuntimeException("unsupported-[captcha.type]=" + captchaType);
        }
        ret.init(config);
        return ret;
    }

    public static CaptchaCacheService getCache(String cacheType) {
        return cacheService.get(cacheType);
    }

    public static final Map<String, CaptchaService> instances = new HashMap<>();
    public static final Map<String, CaptchaCacheService> cacheService = new HashMap<>();

    static {
        cacheService.put("redis", SpringUtil.getBean(CaptchaCacheServiceImpl.class));
        cacheService.put("local", new CaptchaLocalCacheServiceImpl());
        instances.put("blockPuzzle", new BlockPuzzleCaptchaServiceImpl());
    }

}
