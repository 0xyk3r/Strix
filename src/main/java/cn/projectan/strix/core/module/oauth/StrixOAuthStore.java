package cn.projectan.strix.core.module.oauth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author ProjectAn
 * @date 2024/4/3 16:38
 */
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "oauth", havingValue = "true")
public class StrixOAuthStore {

    private final Map<String, StrixOAuthClient> instanceMap = new HashMap<>();

    public void addInstance(String key, StrixOAuthClient instance) {
        instanceMap.put(key, instance);
    }

    public StrixOAuthClient getInstance(String key) {
        return instanceMap.get(key);
    }

    public void removeInstance(String key) {
        instanceMap.remove(key);
    }

    public Set<String> getInstanceKeySet() {
        return instanceMap.keySet();
    }

}
