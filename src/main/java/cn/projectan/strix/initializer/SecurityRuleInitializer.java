package cn.projectan.strix.initializer;

import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.db.SecurityUrl;
import cn.projectan.strix.service.SecurityUrlService;
import cn.projectan.strix.util.SpringUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 安全规则初始化器
 *
 * @author ProjectAn
 * @since 2023/5/26 17:38
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Getter
@Component
public class SecurityRuleInitializer {

    private static final String ASTERISK = "*";
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    private final Map<String, String> urlRoleMap;
    private final Map<String, String> urlAnyRoleMap;
    private final Set<String> anonymousUrlList;

    public SecurityRuleInitializer(SecurityUrlService securityUrlService) {
        List<SecurityUrl> securityUrls = securityUrlService.lambdaQuery()
                .select(SecurityUrl::getUrl, SecurityUrl::getRuleType, SecurityUrl::getRuleValue)
                .list();
        // 允许匿名访问的URL列表
        anonymousUrlList = securityUrls.stream()
                .filter(url -> "permitAll".equals(url.getRuleType()))
                .map(SecurityUrl::getUrl)
                .collect(Collectors.toSet());
        // 需要指定权限/角色的URL列表
        urlRoleMap = securityUrls.stream()
                .filter(url -> "hasRole".equals(url.getRuleType()))
                .collect(Collectors.toMap(SecurityUrl::getUrl, SecurityUrl::getRuleValue, (k1, k2) -> k1));
        // 需要指定任意权限/角色的URL列表
        urlAnyRoleMap = securityUrls.stream()
                .filter(url -> "hasAnyRole".equals(url.getRuleType()))
                .collect(Collectors.toMap(SecurityUrl::getUrl, SecurityUrl::getRuleValue, (k1, k2) -> k1));

        // 从 URL 映射中获取允许匿名访问的 URL
        RequestMappingHandlerMapping mapping = SpringUtil.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
        map.forEach((info, handlerMethod) -> {
            if (info.getPathPatternsCondition() != null) {
                Set<String> patterns = Objects.requireNonNull(info.getPathPatternsCondition().getPatterns())
                        .stream()
                        .map(url -> RegExUtils.replaceAll(url.getPatternString(), PATTERN, ASTERISK))
                        .collect(Collectors.toSet());

                if (AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class) != null ||
                        AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class) != null) {
                    anonymousUrlList.addAll(patterns);
                }
            }
        });
        log.info("Strix Security: 初始化匿名访问API安全规则完成, 扫描到 {} 个 URL.", anonymousUrlList.size());
        log.info("Strix Security: 初始化角色访问API安全规则完成, 扫描到 {} 个 URL.", urlRoleMap.size());
        log.info("Strix Security: 初始化任意角色访问API安全规则完成, 扫描到 {} 个 URL.", urlAnyRoleMap.size());
    }

}
