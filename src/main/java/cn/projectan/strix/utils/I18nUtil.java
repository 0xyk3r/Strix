package cn.projectan.strix.utils;

import cn.projectan.strix.config.StrixLocaleResolver;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 国际化工具类
 *
 * @author ProjectAn
 * @since 2023/4/17 12:25
 */
@Slf4j
@Component
public class I18nUtil {

    @Value("${spring.messages.basename}")
    private String basename;

    private static String[] paths;

    private static String defaultLocale;

    private static StrixLocaleResolver resolver;

    @PostConstruct
    public void init() {
        setBasename(basename);
        log.info("Strix I18n: 初始化完成, 当前语言为: {}.", defaultLocale);
    }

    /**
     * 获取 国际化后内容信息
     *
     * @param code 国际化key
     * @return 国际化后内容信息
     */
    public static String get(String code) {
        return get(code, null, code, resolver.getLocal());
    }

    /**
     * 获取指定语言中的国际化信息，如果没有则走英文
     *
     * @param code 国际化 key
     * @param lang 语言参数
     * @return 国际化后内容信息
     */
    public static String get(String code, String lang) {
        Locale locale = convertLocale(lang);
        return get(code, null, code, locale);
    }

    public static String get(String code, Object[] args, String defaultMessage, Locale locale) {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.toString());
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setBasenames(paths);
        String content;
        try {
            content = messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.error("国际化参数获取失败，{}", e.getMessage(), e);
            content = defaultMessage;
        }
        return content;
    }

    public static void setBasename(String basename) {
        I18nUtil.paths = StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(basename));
    }

    @Value("${strix.default-locale:zh_CN}")
    public void setDefaultLocale(String defaultLocale) {
        I18nUtil.defaultLocale = defaultLocale;
    }

    @Autowired
    public void setResolver(StrixLocaleResolver resolver) {
        I18nUtil.resolver = resolver;
    }

    public static Locale convertLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            return convertLocale(defaultLocale);
        }
        try {
            String[] split = locale.split("_");
            return new Locale.Builder()
                    .setLanguage(split[0])
                    .setRegion(split[1])
                    .build();
        } catch (Exception ignore) {
            log.warn("无法解析语言参数：{}，将使用默认语言：{}.", locale, defaultLocale);
            return convertLocale(defaultLocale);
        }
    }

}
