package cn.projectan.strix.model.properties.system;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * CORS 跨域配置属性
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "strix.cors")
public class StrixCorsProperties {

    /**
     * 允许的域名列表，默认允许所有
     */
    private List<String> allowedOrigins = List.of("*");

}
