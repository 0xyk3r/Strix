package cn.projectan.strix.initialize;

import cn.projectan.strix.model.db.SecurityUrl;
import cn.projectan.strix.service.SecurityUrlService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 加载需要指定权限/角色的 URL
 *
 * @author ProjectAn
 * @date 2023/5/26 17:38
 */
@Slf4j
@Order(1)
@Component
public class RoleUrlInit {

    private final Map<String, String> urlRoleMap;

    private final Map<String, String> urlAnyRoleMap;

    public RoleUrlInit(SecurityUrlService securityUrlService) {
        urlRoleMap = securityUrlService.list(
                        new QueryWrapper<SecurityUrl>()
                                .select("url", "rule_value")
                                .eq("rule_type", "hasRole")
                )
                .stream().collect(Collectors.toMap(SecurityUrl::getUrl, SecurityUrl::getRuleValue, (k1, k2) -> k1));
        urlAnyRoleMap = securityUrlService.list(
                        new QueryWrapper<SecurityUrl>()
                                .select("url", "rule_value")
                                .eq("rule_type", "hasAnyRole")
                )
                .stream().collect(Collectors.toMap(SecurityUrl::getUrl, SecurityUrl::getRuleValue, (k1, k2) -> k1));
    }

    public Map<String, String> getUrlRoleMap() {
        return urlRoleMap;
    }

    public Map<String, String> getUrlAnyRoleMap() {
        return urlAnyRoleMap;
    }

}
