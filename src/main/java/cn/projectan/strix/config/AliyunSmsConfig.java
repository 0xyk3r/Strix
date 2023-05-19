package cn.projectan.strix.config;

import com.aliyuncs.IAcsClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云短信服务
 *
 * @author 安炯奕
 * @date 2021/05/02 17:41
 */
@Component
@ConditionalOnProperty(prefix = "strix", name = "use-sms-aliyun", havingValue = "true")
public class AliyunSmsConfig {

    private final Map<String, IAcsClient> aliyunSmsInstanceMap = new HashMap<>();

    public void addInstance(String id, IAcsClient instance) {
        aliyunSmsInstanceMap.put(id, instance);
    }

    public IAcsClient getInstance(String id) {
        return aliyunSmsInstanceMap.get(id);
    }

}
