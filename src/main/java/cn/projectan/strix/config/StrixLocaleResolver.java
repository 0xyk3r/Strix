package cn.projectan.strix.config;

import cn.projectan.strix.utils.I18nUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;
import java.util.Optional;

/**
 * @author ProjectAn
 * @date 2023/4/17 12:22
 */
@Configuration
@RequiredArgsConstructor
public class StrixLocaleResolver implements LocaleResolver {

    @Value("${strix.default-locale:zh_CN}")
    private String defaultLocale;

    private final HttpServletRequest request;

    @Bean
    public LocaleResolver localeResolver() {
        return new StrixLocaleResolver(request);
    }

    public Locale getLocal() {
        return resolveLocale(request);
    }

    @NotNull
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // 获取请求中的语言参数
        String language = request.getHeader("lang");
        return Optional.ofNullable(language)
                .map(I18nUtil::convertLocale)
                .orElse(I18nUtil.convertLocale(defaultLocale));
    }

    @Override
    public void setLocale(@NotNull HttpServletRequest request, HttpServletResponse response, Locale locale) {

    }

}
