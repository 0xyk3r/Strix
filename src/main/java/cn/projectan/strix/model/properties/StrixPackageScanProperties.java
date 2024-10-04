package cn.projectan.strix.model.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author ProjectAn
 * @since 2023/6/18 15:27
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "strix.package-scan")
public class StrixPackageScanProperties {

    /**
     * 服务包的包名, 多个包名用逗号分隔
     * <br>所有可能被 {@link cn.projectan.strix.utils.ReflectUtil ReflectUtil} 反射调用的类都需要包含在该包内
     */
    private String[] job;

    /**
     * 模型包的包名, 多个包名用逗号分隔
     * <br>所有可能被序列化/反序列化的类都需要包含在该包内
     */
    private String[] model;

}
