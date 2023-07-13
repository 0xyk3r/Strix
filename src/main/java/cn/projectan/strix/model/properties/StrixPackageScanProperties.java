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

    /**
     * 数据库实体类的包名, 多个包名用逗号分隔
     */
    private String[] entity;

    /**
     * 服务接口类的包名, 多个包名用逗号分隔
     */
    private String[] service;

    /**
     * 常量类的包名, 多个包名用逗号分隔
     */
    private String[] constant;

}
