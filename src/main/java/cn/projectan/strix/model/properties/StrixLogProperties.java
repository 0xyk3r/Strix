package cn.projectan.strix.model.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author ProjectAn
 * @since 2023/7/14 19:30
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "strix.log")
public class StrixLogProperties {

    /**
     * 是否启用日志
     */
    private Boolean enable = false;

}
