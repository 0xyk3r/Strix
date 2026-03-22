package cn.projectan.strix.util.common;

import cn.projectan.strix.config.StrixLocaleResolver;
import cn.projectan.strix.model.properties.system.StrixProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 国际化工具类
 * <p>
 * 通过构造器注入 Spring 依赖，同时提供静态方法供非 Spring 管理的类使用。
 *
 * @author ProjectAn
 * @since 2023/4/17 12:25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class I18nUtil {

    private static final Locale FALLBACK_LOCALE = Locale.CHINA;

    private final StrixProperties strixProperties;
    private final StrixLocaleResolver resolver;

    @org.springframework.beans.factory.annotation.Value("${spring.messages.basename}")
    private String basename;

    private static volatile I18nUtil instance;
    private ReloadableResourceBundleMessageSource messageSource;

    @PostConstruct
    private void init() {
        messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.toString());
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setBasenames(StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(basename)));
        instance = this;
        log.info("Strix I18n: 初始化完成, 当前默认语言为: {}.", strixProperties.getDefaultLocale());
    }

    /**
     * 获取国际化消息
     */
    public String getMessage(String code) {
        return getMessage(code, null, code, resolver.getLocal());
    }

    /**
     * 获取指定语言的国际化消息
     */
    public String getMessage(String code, String lang) {
        Locale locale = convertLocale(lang);
        return getMessage(code, null, code, locale);
    }

    /**
     * 获取国际化消息（完整参数）
     */
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.error("国际化参数获取失败: {}", e.getMessage(), e);
            return defaultMessage;
        }
    }

    // ======================== 静态代理方法 (供非注入场景使用) ========================

    public static String get(String code) {
        return instance.getMessage(code);
    }

    public static String get(String code, String lang) {
        return instance.getMessage(code, lang);
    }

    public static String get(String code, Object[] args, String defaultMessage, Locale locale) {
        return instance.getMessage(code, args, defaultMessage, locale);
    }

    /**
     * 将语言字符串转换为 Locale 对象（无状态，保持静态）
     */
    public static Locale convertLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            if (instance == null || !StringUtils.hasText(instance.strixProperties.getDefaultLocale())) {
                return FALLBACK_LOCALE;
            }
            return convertLocale(instance.strixProperties.getDefaultLocale());
        }
        try {
            String[] split = locale.split("_");
            Locale.Builder builder = new Locale.Builder().setLanguage(split[0]);
            if (split.length >= 2) {
                builder.setRegion(split[1]);
            }
            return builder.build();
        } catch (Exception ignore) {
            log.warn("无法解析语言参数：{}，将使用默认语言.", locale);
            return FALLBACK_LOCALE;
        }
    }

}
