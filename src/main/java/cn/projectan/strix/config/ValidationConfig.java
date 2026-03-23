package cn.projectan.strix.config;

import cn.projectan.strix.core.validation.StrixMessageInterpolator;
import jakarta.validation.MessageInterpolator;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * 参数校验配置
 * <p>
 * 注册自定义 {@link StrixMessageInterpolator}，支持 "{模板key:字段标签key}" 格式的参数化校验消息。
 *
 * @author ProjectAn
 */
@Configuration
public class ValidationConfig {

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        MessageInterpolator defaultInterpolator = new ResourceBundleMessageInterpolator();
        bean.setMessageInterpolator(new StrixMessageInterpolator(defaultInterpolator, messageSource));
        return bean;
    }
}
