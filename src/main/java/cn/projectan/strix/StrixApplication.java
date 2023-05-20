package cn.projectan.strix;

import cn.projectan.strix.model.properties.StrixModuleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application 启动类
 *
 * @author 安炯奕
 * @date 2021-05-02
 */
@EnableAsync
@EnableCaching
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication(scanBasePackages = "cn.projectan")
@EnableConfigurationProperties(StrixModuleProperties.class)
public class StrixApplication {

    public static void main(String[] args) {
        SpringApplication.run(StrixApplication.class, args);
    }

}
