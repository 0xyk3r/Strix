package cn.projectan.strix.initialize;

import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.db.SecurityUrl;
import cn.projectan.strix.service.SecurityUrlService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RegExUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 允许匿名访问的URL列表初始化类
 *
 * @author ProjectAn
 * @date 2023/4/6 16:24
 */
@Configuration
public class AnonymousUrlInit implements InitializingBean, ApplicationContextAware {

    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    private ApplicationContext applicationContext;

    @Setter
    @Getter
    private List<String> urls = new ArrayList<>();

    public final String ASTERISK = "*";

    public AnonymousUrlInit(SecurityUrlService securityUrlService) {
        // 从数据库中获取允许匿名访问的URL列表
        List<String> urlList = securityUrlService.list(
                        new QueryWrapper<SecurityUrl>()
                                .select("url")
                                .eq("rule_type", "permitAll")
                )
                .stream().map(SecurityUrl::getUrl).toList();
        urls.addAll(urlList);
    }

    @Override
    public void afterPropertiesSet() {
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);
            // 获取方法上的 @Anonymous 注解，并替换 PathVariable 为 *
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            if (method != null && info.getPathPatternsCondition() != null) {
                Objects.requireNonNull(info.getPathPatternsCondition().getPatterns())
                        .forEach(url -> urls.add(RegExUtils.replaceAll(url.getPatternString(), PATTERN, ASTERISK)));
            }
            // 获取类上的 @Anonymous 注解，并替换 PathVariable 为 *
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            if (method != null && info.getPathPatternsCondition() != null) {
                Objects.requireNonNull(info.getPathPatternsCondition().getPatterns())
                        .forEach(url -> urls.add(RegExUtils.replaceAll(url.getPatternString(), PATTERN, ASTERISK)));
            }
        });
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext context) throws BeansException {
        this.applicationContext = context;
    }

}
