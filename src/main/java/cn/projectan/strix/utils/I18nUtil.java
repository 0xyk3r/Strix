package cn.projectan.strix.utils;

import cn.projectan.strix.config.StrixLocaleResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * @author 安炯奕
 * @date 2023/4/17 12:25
 */
@Slf4j
@Component
public class I18nUtil {

    @Value("${spring.messages.basename}")
    private String basename;

    private static String defaultLocale;

    private final StrixLocaleResolver resolver;

    private static StrixLocaleResolver customLocaleResolver;

    private static String[] paths;


    public I18nUtil(StrixLocaleResolver resolver) {
        this.resolver = resolver;
    }


    @PostConstruct
    public void init() {
        setBasename(basename);
        setCustomLocaleResolver(resolver);
    }

    private static Locale convertLocale(String locale) {
        String[] split = locale.split("_");
        return new Locale(split[0], split[1]);
    }

    /**
     * 获取 国际化后内容信息
     *
     * @param code 国际化key
     * @return 国际化后内容信息
     */
    public static String getMessage(String code) {
        Locale locale = customLocaleResolver.getLocal();
        return getMessage(code, null, code, locale);
    }

    /**
     * 获取指定语言中的国际化信息，如果没有则走英文
     *
     * @param code 国际化 key
     * @param lang 语言参数
     * @return 国际化后内容信息
     */
    public static String getMessage(String code, String lang) {
        Locale locale;
        if (!StringUtils.hasText(lang)) {
            locale = convertLocale(defaultLocale);
        } else {
            try {
                String[] split = lang.split("_");
                locale = new Locale(split[0], split[1]);
            } catch (Exception e) {
                locale = convertLocale(defaultLocale);
            }
        }
        return getMessage(code, null, code, locale);
    }


    public static String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.toString());
        messageSource.setBasenames(paths);
        String content;
        try {
            content = messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.error("国际化参数获取失败===>{},{}", e.getMessage(), e);
            content = defaultMessage;
        }
        return content;

    }

    public static void setBasename(String basename) {
        I18nUtil.paths = StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(basename));
    }

    public static void setCustomLocaleResolver(StrixLocaleResolver resolver) {
        I18nUtil.customLocaleResolver = resolver;
    }

    @Value("${strix.default-locale:zh_CN}")
    public void setDefaultLocale(String defaultLocale) {
        I18nUtil.defaultLocale = defaultLocale;
    }

}
