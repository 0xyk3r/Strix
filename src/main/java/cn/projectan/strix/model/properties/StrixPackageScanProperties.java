package cn.projectan.strix.model.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 安炯奕
 * @date 2023/6/18 15:27
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "strix.package-scan")
public class StrixPackageScanProperties {

    private String[] constant;

}
