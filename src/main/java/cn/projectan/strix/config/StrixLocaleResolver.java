package cn.projectan.strix.config;

import cn.projectan.strix.model.properties.system.StrixProperties;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.http.ServletUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * Strix I18n 语言解析器
 * <p>
 * 通过 {@code @Component("localeResolver")} 注册为 Spring MVC 的 LocaleResolver Bean,
 * DispatcherServlet 会自动按名称 "localeResolver" 查找并使用此 Bean.
 * <p>
 * 优先级: 自定义 lang 头 > Accept-Language 头 > 默认配置
 *
 * @author ProjectAn
 * @since 2023/4/17 12:22
 */
@Component("localeResolver")
@RequiredArgsConstructor
public class StrixLocaleResolver implements LocaleResolver {

    private final StrixProperties strixProperties;

    /**
     * 获取当前 Locale（非 Web 上下文安全）
     */
    public Locale getLocale() {
        try {
            return resolveLocale(ServletUtil.getRequest());
        } catch (Exception e) {
            return I18nUtil.convertLocale(strixProperties.getDefaultLocale());
        }
    }

    @Nonnull
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // 优先使用自定义 lang 头
        String requestLang = request.getHeader("lang");
        if (StringUtils.hasText(requestLang)) {
            return I18nUtil.convertLocale(requestLang);
        }

        // 其次使用标准 Accept-Language 头
        String acceptLanguage = request.getHeader("Accept-Language");
        if (StringUtils.hasText(acceptLanguage)) {
            // 取 Accept-Language 第一个语言标签 (e.g. "zh-CN,zh;q=0.9,en;q=0.8" → "zh-CN")
            String primaryLang = acceptLanguage.split(",")[0].split(";")[0].trim();
            return I18nUtil.convertLocale(primaryLang);
        }

        return I18nUtil.convertLocale(strixProperties.getDefaultLocale());
    }

    @Override
    public void setLocale(@Nonnull HttpServletRequest request, HttpServletResponse response, Locale locale) {

    }

}
