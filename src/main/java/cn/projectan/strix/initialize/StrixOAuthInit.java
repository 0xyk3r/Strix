package cn.projectan.strix.initialize;

import cn.projectan.strix.core.module.oauth.StrixOAuthStore;
import cn.projectan.strix.model.db.OauthConfig;
import cn.projectan.strix.service.OauthConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ProjectAn
 * @date 2024/4/3 17:01
 */
@Slf4j
@Order(12)
@Component
@ConditionalOnBean(StrixOAuthStore.class)
@RequiredArgsConstructor
public class StrixOAuthInit implements ApplicationRunner {

    private final OauthConfigService oauthConfigService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<OauthConfig> oauthConfigList = oauthConfigService.list();
        oauthConfigService.createInstance(oauthConfigList);
    }

}
