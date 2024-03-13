package cn.projectan.strix.core.module.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Strix SMS 短信服务
 *
 * @author ProjectAn
 * @date 2021/05/02 17:41
 */
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "sms", havingValue = "true")
public class StrixSmsConfig {

    private final Map<String, StrixSmsClient> instanceMap = new HashMap<>();

    public void addInstance(String key, StrixSmsClient client) {
        instanceMap.put(key, client);
    }

    public StrixSmsClient getInstance(String key) {
        return instanceMap.get(key);
    }

    public void removeInstance(String key) {
        instanceMap.remove(key);
    }

    public Set<String> getInstanceKeySet() {
        return instanceMap.keySet();
    }

}
