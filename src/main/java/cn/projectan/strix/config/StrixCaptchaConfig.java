package cn.projectan.strix.config;

import cn.projectan.strix.core.captcha.CaptchaCacheService;
import cn.projectan.strix.core.captcha.CaptchaService;
import cn.projectan.strix.core.captcha.impl.CaptchaServiceFactory;
import cn.projectan.strix.core.captcha.util.StrixCaptchaImageUtils;
import cn.projectan.strix.model.constant.StrixCaptchaConst;
import cn.projectan.strix.model.properties.StrixCaptchaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Strix 验证码配置
 *
 * @author ProjectAn
 * @since 2024/3/26 16:44
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(StrixCaptchaProperties.class)
public class StrixCaptchaConfig {

    @Bean(name = "StrixCaptchaCacheService")
    public CaptchaCacheService captchaCacheService(StrixCaptchaProperties strixCaptchaProperties) {
        return CaptchaServiceFactory.getCache(strixCaptchaProperties.getCacheType().name());
    }

    @Bean
    public CaptchaService captchaService(StrixCaptchaProperties prop) {
        Properties config = new Properties();
        config.put(StrixCaptchaConst.CAPTCHA_CACHE_TYPE, prop.getCacheType().name());
        config.put(StrixCaptchaConst.CAPTCHA_WATER_MARK, prop.getWaterMark());
        config.put(StrixCaptchaConst.CAPTCHA_FONT_TYPE, prop.getFontType());
        config.put(StrixCaptchaConst.CAPTCHA_TYPE, prop.getType().getCodeValue());
        config.put(StrixCaptchaConst.CAPTCHA_INTERFERENCE_OPTIONS, prop.getInterferenceOptions());
        config.put(StrixCaptchaConst.ORIGINAL_PATH_JIGSAW, prop.getJigsaw());
        config.put(StrixCaptchaConst.ORIGINAL_PATH_PIC_CLICK, prop.getPicClick());
        config.put(StrixCaptchaConst.CAPTCHA_SLIP_OFFSET, prop.getSlipOffset());
        config.put(StrixCaptchaConst.CAPTCHA_AES_STATUS, String.valueOf(prop.getAesStatus()));
        config.put(StrixCaptchaConst.CAPTCHA_WATER_FONT, prop.getWaterFont());
        config.put(StrixCaptchaConst.CAPTCHA_CACHE_MAX_NUMBER, prop.getCacheNumber());
        config.put(StrixCaptchaConst.CAPTCHA_TIMING_CLEAR_SECOND, prop.getTimingClear());

        config.put(StrixCaptchaConst.HISTORY_DATA_CLEAR_ENABLE, prop.getHistoryDataClearEnable() ? "1" : "0");

        config.put(StrixCaptchaConst.REQ_FREQUENCY_LIMIT_ENABLE, prop.getReqFrequencyLimitEnable() ? "1" : "0");
        config.put(StrixCaptchaConst.REQ_GET_LOCK_LIMIT, prop.getReqGetLockLimit() + "");
        config.put(StrixCaptchaConst.REQ_GET_LOCK_SECONDS, prop.getReqGetLockSeconds() + "");
        config.put(StrixCaptchaConst.REQ_GET_MINUTE_LIMIT, prop.getReqGetMinuteLimit() + "");
        config.put(StrixCaptchaConst.REQ_CHECK_MINUTE_LIMIT, prop.getReqCheckMinuteLimit() + "");
        config.put(StrixCaptchaConst.REQ_VALIDATE_MINUTE_LIMIT, prop.getReqVerifyMinuteLimit() + "");

        config.put(StrixCaptchaConst.CAPTCHA_FONT_SIZE, prop.getFontSize() + "");
        config.put(StrixCaptchaConst.CAPTCHA_FONT_STYLE, prop.getFontStyle() + "");
        config.put(StrixCaptchaConst.CAPTCHA_WORD_COUNT, prop.getClickWordCount() + "");

        if ((StringUtils.hasText(prop.getJigsaw()) && prop.getJigsaw().startsWith("classpath:"))
                || (StringUtils.hasText(prop.getPicClick()) && prop.getPicClick().startsWith("classpath:"))) {
            // 自定义resources目录下初始化底图
            config.put(StrixCaptchaConst.CAPTCHA_INIT_ORIGINAL, "true");
            initializeBaseMap(prop.getJigsaw(), prop.getPicClick());
        }
        return CaptchaServiceFactory.getInstance(config);
    }

    private static void initializeBaseMap(String jigsaw, String picClick) {
        StrixCaptchaImageUtils.cacheBootImage(getResourcesImagesFile(jigsaw + "/original/*.png"),
                getResourcesImagesFile(jigsaw + "/slidingBlock/*.png"),
                getResourcesImagesFile(picClick + "/*.png"));
    }

    public static Map<String, String> getResourcesImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(path);
            for (Resource resource : resources) {
                byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String string = Base64.getEncoder().encodeToString(bytes);
                String filename = resource.getFilename();
                imgMap.put(filename, string);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return imgMap;
    }

}
