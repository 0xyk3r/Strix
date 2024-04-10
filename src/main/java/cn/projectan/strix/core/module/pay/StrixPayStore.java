package cn.projectan.strix.core.module.pay;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Strix Pay 支付服务
 *
 * @author ProjectAn
 * @date 2024/4/2 17:13
 */
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "pay", havingValue = "true")
public class StrixPayStore {

    private final Map<String, StrixPayClient> instanceMap = new HashMap<>();

    public void addInstance(String key, StrixPayClient instance) {
        instanceMap.put(key, instance);
    }

    public StrixPayClient getInstance(String key) {
        return instanceMap.get(key);
    }

    public void removeInstance(String key) {
        instanceMap.remove(key);
    }

    public Set<String> getInstanceKeySet() {
        return instanceMap.keySet();
    }

}
