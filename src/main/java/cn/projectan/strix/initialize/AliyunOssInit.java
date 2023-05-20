package cn.projectan.strix.initialize;

import cn.projectan.strix.config.AliyunOssConfig;
import cn.projectan.strix.model.db.AliyunOss;
import cn.projectan.strix.model.system.AliyunOssInstance;
import cn.projectan.strix.service.AliyunOssService;
import cn.projectan.strix.utils.AliyunOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/5/2 17:20
 */
@Slf4j
@Order(value = 1)
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "oss", havingValue = "true")
public class AliyunOssInit implements ApplicationRunner {

    @Autowired
    private AliyunOssService aliyunOssService;

    @Autowired
    private AliyunOssConfig aliyunOssConfig;
    @Autowired
    private AliyunOssUtil aliyunOssUtil;

    @Override
    public void run(ApplicationArguments args) {
        List<AliyunOss> aliyunOssList = aliyunOssService.list();

        for (AliyunOss aliyunOss : aliyunOssList) {
            AliyunOssInstance aliyunOssInstance = aliyunOssUtil.createInstance(aliyunOss);
            if (aliyunOssInstance != null) {
                aliyunOssConfig.addInstance(aliyunOss.getId(), aliyunOssInstance);
            }
        }
    }

}
