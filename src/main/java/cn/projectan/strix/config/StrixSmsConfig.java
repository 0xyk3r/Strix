package cn.projectan.strix.config;

import cn.projectan.strix.core.sms.StrixSmsClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 阿里云短信服务
 *
 * @author 安炯奕
 * @date 2021/05/02 17:41
 */
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "sms", havingValue = "true")
public class StrixSmsConfig {

    private final Map<String, StrixSmsClient> instanceMap = new HashMap<>();

    public <T> void addInstance(String id, StrixSmsClient client) {
        instanceMap.put(id, client);
    }

    public StrixSmsClient getInstance(String id) {
        return instanceMap.get(id);
    }

    public Set<String> getInstanceKeySet() {
        return instanceMap.keySet();
    }

}
