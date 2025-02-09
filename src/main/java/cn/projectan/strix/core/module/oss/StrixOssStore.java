package cn.projectan.strix.core.module.oss;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Strix OSS 客户端容器
 *
 * @author ProjectAn
 * @since 2021/05/02 17:23
 */
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "oss", havingValue = "true")
public class StrixOssStore {

    private final Map<String, StrixOssClient> instanceMap = new HashMap<>();

    public void addInstance(String key, StrixOssClient instance) {
        instanceMap.put(key, instance);
    }

    public StrixOssClient getInstance(String key) {
        return instanceMap.get(key);
    }

    public void removeInstance(String key) {
        Optional.ofNullable(instanceMap.get(key)).ifPresent(StrixOssClient::close);
        instanceMap.remove(key);
    }

    public Set<String> getInstanceKeySet() {
        return instanceMap.keySet();
    }

    @PreDestroy
    public void destroy() {
        instanceMap.values().forEach(StrixOssClient::close);
    }

}
