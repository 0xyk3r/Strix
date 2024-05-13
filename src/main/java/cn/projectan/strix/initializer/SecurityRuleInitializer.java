package cn.projectan.strix.initializer;

import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.db.SecurityUrl;
import cn.projectan.strix.service.SecurityUrlService;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 安全规则初始化器
 *
 * @author ProjectAn
 * @date 2023/5/26 17:38
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
        // 从数据库中获取允许匿名访问的URL列表
        anonymousUrlList = securityUrlService.list(
                        new QueryWrapper<SecurityUrl>()
                                .select("url")
                                .eq("rule_type", "permitAll")
                )
                .stream().map(SecurityUrl::getUrl).collect(Collectors.toSet());
        // 从数据库中获取需要指定权限/角色的URL列表
        urlRoleMap = securityUrlService.list(
                        new QueryWrapper<SecurityUrl>()
                                .select("url", "rule_value")
                                .eq("rule_type", "hasRole")
                )
                .stream().collect(Collectors.toMap(SecurityUrl::getUrl, SecurityUrl::getRuleValue, (k1, k2) -> k1));
        // 从数据库中获取需要指定任意权限/角色的URL列表
        urlAnyRoleMap = securityUrlService.list(
                        new QueryWrapper<SecurityUrl>()
                                .select("url", "rule_value")
                                .eq("rule_type", "hasAnyRole")
                )
                .stream().collect(Collectors.toMap(SecurityUrl::getUrl, SecurityUrl::getRuleValue, (k1, k2) -> k1));

        // 从 URL 映射中获取允许匿名访问的 URL
        RequestMappingHandlerMapping mapping = SpringUtil.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);
            // 获取方法上的 @Anonymous 注解，并替换 PathVariable 为 *
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            if (method != null && info.getPathPatternsCondition() != null) {
                Objects.requireNonNull(info.getPathPatternsCondition().getPatterns())
                        .forEach(url -> anonymousUrlList.add(RegExUtils.replaceAll(url.getPatternString(), PATTERN, ASTERISK)));
            } else {
                // 获取类上的 @Anonymous 注解，并替换 PathVariable 为 *
                Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
                if (controller != null && info.getPathPatternsCondition() != null) {
                    Objects.requireNonNull(info.getPathPatternsCondition().getPatterns())
                            .forEach(url -> anonymousUrlList.add(RegExUtils.replaceAll(url.getPatternString(), PATTERN, ASTERISK)));
                }
            }
        });
        log.info("Strix Security: 初始化匿名访问API安全规则完成, 扫描到 {} 个 URL.", anonymousUrlList.size());
        log.info("Strix Security: 初始化角色访问API安全规则完成, 扫描到 {} 个 URL.", urlRoleMap.size());
        log.info("Strix Security: 初始化任意角色访问API安全规则完成, 扫描到 {} 个 URL.", urlAnyRoleMap.size());
    }

}
