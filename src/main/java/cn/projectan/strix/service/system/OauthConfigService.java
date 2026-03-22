package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.oauth.OAuthClientFactory;
import cn.projectan.strix.core.module.oauth.StrixOAuthClient;
import cn.projectan.strix.core.module.oauth.StrixOAuthStore;
import cn.projectan.strix.mapper.system.OauthConfigMapper;
import cn.projectan.strix.model.db.system.OauthConfig;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Strix OAuth 配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OauthConfigService extends ServiceImpl<OauthConfigMapper, OauthConfig> {

    private final StrixOAuthStore strixOAuthStore;
    private final List<OAuthClientFactory> oAuthClientFactories;

    private Map<Short, OAuthClientFactory> factoryMap;

    @PostConstruct
    private void initFactoryMap() {
        factoryMap = oAuthClientFactories.stream()
                .collect(Collectors.toMap(OAuthClientFactory::supportedPlatform, Function.identity()));
    }

    /**
     * 获取指定 configKey 的 OAuth 客户端实例
     */
    public StrixOAuthClient<?> getInstance(String configKey) {
        return strixOAuthStore.getInstance(configKey);
    }

    /**
     * 获取指定 configKey 和平台的 OAuth 客户端实例
     */
    public StrixOAuthClient<?> getInstance(String configKey, short platform) {
        return strixOAuthStore.getInstance(configKey, platform);
    }

    /**
     * 创建 OAuth 配置
     *
     * @param oauthConfigList OAuth 配置列表
     */
    public void createInstance(List<OauthConfig> oauthConfigList) {
        for (OauthConfig oauthConfig : oauthConfigList) {
            try {
                OAuthClientFactory factory = factoryMap.get(oauthConfig.getPlatform());
                if (factory == null) {
                    log.error("Strix OAuth: 初始化 OAuth 服务实例 <{}> 失败. (不支持的平台: {})", oauthConfig.getName(), oauthConfig.getPlatform());
                    continue;
                }
                StrixOAuthClient<?> client = factory.createClient(oauthConfig);
                strixOAuthStore.addInstance(oauthConfig.getKey(), client);
                log.info("Strix OAuth: 初始化 OAuth 服务实例 <{}> 成功.", oauthConfig.getName());
            } catch (Exception e) {
                log.error("Strix OAuth: 初始化 OAuth 服务实例 <{}> 失败. (配置信息错误)", oauthConfig.getName(), e);
            }
        }
    }

}
