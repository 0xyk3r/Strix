package cn.projectan.strix.core.datamask;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Strix 数据脱敏工具配置
 *
 * @author ProjectAn
 * @since 2023/2/22 14:52
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataMaskConfiguration {

    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        AnnotationIntrospector ai = objectMapper.getSerializationConfig().getAnnotationIntrospector();
        AnnotationIntrospector newAi = AnnotationIntrospectorPair.pair(ai, new DataMaskAnnotationIntrospector());
        objectMapper.setAnnotationIntrospector(newAi);
        log.info("Strix DataMask: 数据脱敏工具初始化完成.");
    }

}
