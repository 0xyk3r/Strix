package cn.projectan.strix.model.properties.system;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 延迟任务配置属性
 *
 * @author ProjectAn
 * @since 2024-12-18
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "strix.delayed-task")
public class StrixDelayedTaskProperties {

    /**
     * 是否启用延迟任务功能
     */
    private Boolean enabled = false;

    /**
     * 每次扫描获取的最大任务数
     */
    private Integer batchSize = 100;

}
