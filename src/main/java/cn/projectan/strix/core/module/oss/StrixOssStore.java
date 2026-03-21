package cn.projectan.strix.core.module.oss;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strix OSS 客户端容器
 *
 * @author ProjectAn
 * @since 2021/05/02 17:23
 */
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "oss", havingValue = "true")
public class StrixOssStore {

    private final Map<String, StrixOssClient> instanceMap = new ConcurrentHashMap<>();

    public void addInstance(String key, StrixOssClient instance) {
        instanceMap.put(key, instance);
    }

    public StrixOssClient getInstance(String key) {
        return instanceMap.get(key);
    }

    public void removeInstance(String key) {
        // 原子地移除并关闭，避免 check-then-act 竞态
        StrixOssClient removed = instanceMap.remove(key);
        if (removed != null) {
            removed.close();
        }
    }

    public Set<String> getInstanceKeySet() {
        return instanceMap.keySet();
    }

    @PreDestroy
    public void destroy() {
        instanceMap.values().forEach(StrixOssClient::close);
    }

}
