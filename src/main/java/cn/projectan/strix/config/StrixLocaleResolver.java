package cn.projectan.strix.config;

import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.http.ServletUtils;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;
import java.util.Optional;

/**
 * Strix I18n 语言解析器
 * <p>
 * 通过 {@code @Component("localeResolver")} 注册为 Spring MVC 的 LocaleResolver Bean,
 * DispatcherServlet 会自动按名称 "localeResolver" 查找并使用此 Bean.
 *
 * @author ProjectAn
 * @since 2023/4/17 12:22
 */
@Component("localeResolver")
public class StrixLocaleResolver implements LocaleResolver {

    @Value("${strix.default-locale:zh_CN}")
    private String defaultLocale;

    public Locale getLocal() {
        return resolveLocale(ServletUtils.getRequest());
    }

    @Nonnull
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // 获取请求中的语言参数
        String requestLang = request.getHeader("lang");
        return Optional.ofNullable(requestLang)
                .map(I18nUtil::convertLocale)
                .orElse(I18nUtil.convertLocale(defaultLocale));
    }

    @Override
    public void setLocale(@Nonnull HttpServletRequest request, HttpServletResponse response, Locale locale) {

    }

}
