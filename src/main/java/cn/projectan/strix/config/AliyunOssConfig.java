package cn.projectan.strix.config;

import cn.projectan.strix.model.system.AliyunOssInstance;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云OSS
 *
 * @author 安炯奕
 * @date 2021/05/02 17:23
 */
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "oss", havingValue = "true")
public class AliyunOssConfig {

    private final Map<String, AliyunOssInstance> aliyunOssInstanceMap = new HashMap<>();

    public void addInstance(String id, AliyunOssInstance instance) {
        aliyunOssInstanceMap.put(id, instance);
    }

    public AliyunOssInstance getInstance(String id) {
        return aliyunOssInstanceMap.get(id);
    }

}
