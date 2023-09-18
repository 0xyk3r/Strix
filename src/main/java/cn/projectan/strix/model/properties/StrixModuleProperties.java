package cn.projectan.strix.model.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 安炯奕
 * @date 2023/5/20 14:42
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "strix.module")
public class StrixModuleProperties {

    /**
     * 是否启用 SMS 服务
     */
    private Boolean sms = false;

    /**
     * 是否启用 OSS 服务
     */
    private Boolean oss = false;

    /**
     * 是否启用 Job 定时任务服务
     */
    private Boolean job = false;

    /**
     * 是否启用 Auth 服务
     */
    private Boolean auth = false;

    /**
     * 是否启用 Push 服务
     */
    private Boolean push = false;

    /**
     * 是否启用 Payment 服务
     */
    private Boolean payment = false;

}
