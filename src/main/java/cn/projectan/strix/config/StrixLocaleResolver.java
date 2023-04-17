package cn.projectan.strix.config;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private HttpServletRequest request;

    public Locale getLocal() {
        return resolveLocale(request);
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // 获取请求中的语言参数
        String language = request.getParameter("lang");
        // 如果没有就使用默认的 根据主机的语言环境生成一个 Locale
        Locale locale = Locale.getDefault();
        // 如果请求的链接中携带了 国际化的参数
        if (StringUtils.hasText(language)) {
            String[] s = language.split("_");
            locale = new Locale(s[0], s[1]);
        }
        return locale;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {

    }

}
