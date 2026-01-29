package cn.projectan.strix.core.module.oauth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Strix OAuth 客户端容器
 *
 * @author ProjectAn
 * @since 2024/4/3 16:38
 */
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "oauth", havingValue = "true")
public class StrixOAuthStore {

    private final Map<String, StrixOAuthClient> instanceMap = new HashMap<>();

    /**
     * 添加实例
     *
     * @param key      OAuth 配置 Key
     * @param instance OAuth 客户端实例
     */
    public void addInstance(String key, StrixOAuthClient instance) {
        instanceMap.put(key, instance);
    }

    /**
     * 获取指定 Key 的实例
     *
     * @param key OAuth 配置 Key
     * @return OAuth 客户端实例
     */
    public StrixOAuthClient getInstance(String key) {
        return instanceMap.get(key);
    }

    /**
     * 获取指定平台的实例
     *
     * @param key      OAuth 配置 Key
     * @param platform 平台类型
     * @return OAuth 客户端实例
     * @see cn.projectan.strix.model.dict.system.OAuthPlatform
     */
    public StrixOAuthClient getInstance(String key, Short platform) {
        StrixOAuthClient instance = instanceMap.get(key);
        if (instance != null && instance.getPlatform() == platform) {
            return instance;
        }
        return null;
    }

    /**
     * 移除指定 Key 的实例
     *
     * @param key OAuth 配置 Key
     */
    public void removeInstance(String key) {
        instanceMap.remove(key);
    }

    /**
     * 获取所有实例的 Key 集合
     *
     * @return 实例 Key 集合
     */
    public Set<String> getInstanceKeySet() {
        return instanceMap.keySet();
    }

}
