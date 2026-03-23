package cn.projectan.strix.config;

import cn.projectan.strix.core.captcha.CaptchaCacheService;
import cn.projectan.strix.core.captcha.CaptchaService;
import cn.projectan.strix.core.captcha.impl.CaptchaCacheServiceImpl;
import cn.projectan.strix.core.captcha.impl.CaptchaServiceFactory;
import cn.projectan.strix.model.properties.system.StrixCaptchaProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Strix 验证码配置
 *
 * @author ProjectAn
 * @since 2024/3/26 16:44
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StrixCaptchaProperties.class)
public class StrixCaptchaConfig {

    private final CaptchaCacheServiceImpl captchaCacheServiceImpl;

    @PostConstruct
    public void initFactory() {
        CaptchaServiceFactory.init(captchaCacheServiceImpl);
    }

    @Bean(name = "StrixCaptchaCacheService")
    public CaptchaCacheService captchaCacheService(StrixCaptchaProperties strixCaptchaProperties) {
        return CaptchaServiceFactory.getCache(strixCaptchaProperties.getCacheType().name());
    }

    @Bean
    public CaptchaService captchaService(StrixCaptchaProperties prop) {
        Properties config = new Properties();
        config.put(StrixCaptchaProperties.Key.CAPTCHA_CACHE_TYPE, prop.getCacheType().name());
        config.put(StrixCaptchaProperties.Key.CAPTCHA_WATER_MARK, prop.getWaterMark());
        config.put(StrixCaptchaProperties.Key.CAPTCHA_TYPE, prop.getType().getCodeValue());
        config.put(StrixCaptchaProperties.Key.CAPTCHA_INTERFERENCE_OPTIONS, prop.getInterferenceOptions());
        config.put(StrixCaptchaProperties.Key.ORIGINAL_PATH_JIGSAW, prop.getJigsaw());
        config.put(StrixCaptchaProperties.Key.CAPTCHA_SLIP_OFFSET, prop.getSlipOffset());
        config.put(StrixCaptchaProperties.Key.CAPTCHA_AES_STATUS, String.valueOf(prop.getAesStatus()));
        config.put(StrixCaptchaProperties.Key.CAPTCHA_WATER_FONT, prop.getWaterFont());
        config.put(StrixCaptchaProperties.Key.CAPTCHA_CACHE_MAX_NUMBER, prop.getCacheNumber());
        config.put(StrixCaptchaProperties.Key.CAPTCHA_TIMING_CLEAR_SECOND, prop.getTimingClear());

        config.put(StrixCaptchaProperties.Key.HISTORY_DATA_CLEAR_ENABLE, prop.getHistoryDataClearEnable() ? "1" : "0");

        config.put(StrixCaptchaProperties.Key.REQ_FREQUENCY_LIMIT_ENABLE, prop.getReqFrequencyLimitEnable() ? "1" : "0");
        config.put(StrixCaptchaProperties.Key.REQ_GET_LOCK_LIMIT, prop.getReqGetLockLimit() + "");
        config.put(StrixCaptchaProperties.Key.REQ_GET_LOCK_SECONDS, prop.getReqGetLockSeconds() + "");
        config.put(StrixCaptchaProperties.Key.REQ_GET_MINUTE_LIMIT, prop.getReqGetMinuteLimit() + "");
        config.put(StrixCaptchaProperties.Key.REQ_CHECK_MINUTE_LIMIT, prop.getReqCheckMinuteLimit() + "");
        config.put(StrixCaptchaProperties.Key.REQ_VALIDATE_MINUTE_LIMIT, prop.getReqVerifyMinuteLimit() + "");

        return CaptchaServiceFactory.getInstance(config);
    }
}
