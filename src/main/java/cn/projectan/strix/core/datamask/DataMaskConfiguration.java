package cn.projectan.strix.core.datamask;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * @author 安炯奕
 * @date 2023/2/22 14:52
 */
@Slf4j
@Configuration
public class DataMaskConfiguration {

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("Strix Data Mask: 数据脱敏工具已启用.");
        AnnotationIntrospector ai = objectMapper.getSerializationConfig().getAnnotationIntrospector();
        AnnotationIntrospector newAi = AnnotationIntrospectorPair.pair(ai, new DataMaskAnnotationIntrospector());
        objectMapper.setAnnotationIntrospector(newAi);
    }

}
