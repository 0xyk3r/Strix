package cn.projectan.strix.util.common;

import cn.projectan.strix.config.StrixLocaleResolver;
import cn.projectan.strix.model.properties.system.StrixProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    private static Locale fallbackLocale = Locale.CHINA;

    private final StrixProperties strixProperties;
    private final StrixLocaleResolver resolver;
    private final MessageSource messageSource;

    private static volatile I18nUtil instance;

    @PostConstruct
    private void init() {
        fallbackLocale = convertLocale(strixProperties.getDefaultLocale());
        instance = this;
        log.info("Strix I18n: 初始化完成, 当前默认语言为: {}.", strixProperties.getDefaultLocale());
    }

    /**
     * 获取国际化消息
     */
    public String getMessage(String code) {
        return getMessage(code, null, code, resolver.getLocale());
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
        } catch (NoSuchMessageException e) {
            log.warn("I18n key not found: {}", code);
            return defaultMessage;
        }
    }

    // ======================== 静态代理方法 (供非注入场景使用) ========================

    public static String get(String code) {
        if (instance == null) {
            return code;
        }
        return instance.getMessage(code);
    }

    public static String get(String code, Object... args) {
        if (instance == null) {
            return code;
        }
        return instance.getMessage(code, args, code, instance.resolver.getLocale());
    }

    /**
     * 便捷方法："{0}不存在" 模板
     */
    public static String notFound(String fieldKey) {
        return get("assert.notFound", (Object) get(fieldKey));
    }

    /**
     * 便捷方法："{0}不能为空" 模板
     */
    public static String notEmpty(String fieldKey) {
        return get("assert.notEmpty", (Object) get(fieldKey));
    }

    /**
     * 便捷方法："{0}无效" 模板
     */
    public static String invalid(String fieldKey) {
        return get("assert.invalid", (Object) get(fieldKey));
    }

    /**
     * 便捷方法："{0}失败" 模板
     */
    public static String failed(String fieldKey) {
        return get("assert.failed", (Object) get(fieldKey));
    }

    /**
     * 便捷方法："{0}初始化失败" 模板
     */
    public static String initFailed(String fieldKey) {
        return get("assert.initFailed", (Object) get(fieldKey));
    }

    public static String get(String code, String lang) {
        if (instance == null) {
            return code;
        }
        return instance.getMessage(code, lang);
    }

    public static String get(String code, Object[] args, String defaultMessage, Locale locale) {
        if (instance == null) {
            return code;
        }
        return instance.getMessage(code, args, defaultMessage, locale);
    }

    /**
     * 将语言字符串转换为 Locale 对象（无状态，保持静态）
     * 支持下划线格式 (zh_CN) 和连字符格式 (zh-CN)
     */
    public static Locale convertLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            if (instance == null || !StringUtils.hasText(instance.strixProperties.getDefaultLocale())) {
                return fallbackLocale;
            }
            return convertLocale(instance.strixProperties.getDefaultLocale());
        }
        try {
            return Locale.forLanguageTag(locale.replace("_", "-"));
        } catch (Exception ignore) {
            log.warn("无法解析语言参数：{}，将使用默认语言.", locale);
            return fallbackLocale;
        }
    }

}
