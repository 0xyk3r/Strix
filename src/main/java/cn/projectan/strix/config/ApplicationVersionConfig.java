package cn.projectan.strix.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 应用版本配置
 *
 * @author ProjectAn
 * @since 2025/12/17
 */
@Slf4j
@Getter
@Configuration
public class ApplicationVersionConfig {

    /**
     * 应用名称
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 应用版本（从 MANIFEST.MF 中读取，开发环境默认为 DEV）
     */
    @Value("${application.version:DEV}")
    private String applicationVersion;

    /**
     * 获取应用版本
     * 优先从 package 中读取，如果读取不到则使用配置的版本
     */
    public String getVersion() {
        // 尝试从 MANIFEST.MF 读取版本号
        String manifestVersion = getClass().getPackage().getImplementationVersion();
        if (manifestVersion != null && !manifestVersion.isBlank()) {
            return manifestVersion;
        }

        // 如果读取不到，使用配置的版本（通常是开发环境）
        return applicationVersion != null ? applicationVersion : "UNKNOWN";
    }
}
