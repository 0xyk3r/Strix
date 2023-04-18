package cn.projectan.strix.config;

import cn.projectan.strix.utils.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * @author 安炯奕
 * @date 2023/4/17 12:22
 */
@Configuration
public class StrixLocaleResolver implements LocaleResolver {

    @Value("${strix.default-locale:zh_CN}")
    private String defaultLocale;

    @Autowired
    private HttpServletRequest request;

    @Bean
    public LocaleResolver localeResolver() {
        return new StrixLocaleResolver();
    }

    public Locale getLocal() {
        return resolveLocale(request);
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // 获取请求中的语言参数
        String language = request.getHeader("lang");
        Locale locale = I18nUtil.convertLocale(defaultLocale);
        if (StringUtils.hasText(language)) {
            locale = I18nUtil.convertLocale(language);
        }
        return locale;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {

    }

}
