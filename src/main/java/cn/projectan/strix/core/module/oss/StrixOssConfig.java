package cn.projectan.strix.core.module.oss;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Strix OSS 对象存储服务
 *
 * @author 安炯奕
 * @date 2021/05/02 17:23
 */
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "oss", havingValue = "true")
public class StrixOssConfig {

    private final Map<String, StrixOssClient> instanceMap = new HashMap<>();

    public void addInstance(String key, StrixOssClient instance) {
        instanceMap.put(key, instance);
    }

    public StrixOssClient getInstance(String key) {
        return instanceMap.get(key);
    }

    public void removeInstance(String key) {
        instanceMap.remove(key);
    }

    public Set<String> getInstanceKeySet() {
        return instanceMap.keySet();
    }

}
