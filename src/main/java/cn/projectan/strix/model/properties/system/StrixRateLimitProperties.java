package cn.projectan.strix.model.properties.system;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API 速率限制配置属性
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "strix.rate-limit")
public class StrixRateLimitProperties {

    /**
     * 是否启用速率限制
     */
    private boolean enabled = true;

    /**
     * 默认时间窗口内最大请求数
     */
    private int defaultLimit = 600;

    /**
     * 默认时间窗口（秒）
     */
    private int defaultWindow = 60;

}
