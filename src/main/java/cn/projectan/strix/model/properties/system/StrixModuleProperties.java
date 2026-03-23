package cn.projectan.strix.model.properties.system;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author ProjectAn
 * @since 2023/5/20 14:42
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "strix.module")
public class StrixModuleProperties {

    /**
     * 是否启用 SMS 服务
     */
    private boolean sms = false;

    /**
     * 是否启用 OSS 服务
     */
    private boolean oss = false;

    /**
     * 是否启用 Job 定时任务服务
     */
    private boolean job = false;

    /**
     * 是否启用 OAuth 服务
     */
    private boolean oauth = false;

    /**
     * 是否启用 Push 服务
     */
    private boolean push = false;

    /**
     * 是否启用 Pay 服务
     */
    private boolean pay = false;

}
