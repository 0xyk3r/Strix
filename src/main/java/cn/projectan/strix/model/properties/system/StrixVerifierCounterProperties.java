package cn.projectan.strix.model.properties.system;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author ProjectAn
 * @since 2023/5/18 15:21
 */
@Getter
@Validated
@ConfigurationProperties(prefix = "strix.verifier-counter")
public class StrixVerifierCounterProperties {

    /**
     * 短信验证码配置
     */
    @Valid
    private final Sms sms = new Sms();
    /**
     * 邮箱验证码配置
     */
    @Valid
    private final Email email = new Email();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sms {
        /**
         * 限制次数
         */
        @Positive
        private Long limit;
        /**
         * 次数记录时长 单位秒
         */
        @Positive
        private Long seconds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Email {
        /**
         * 限制次数
         */
        @Positive
        private Long limit;
        /**
         * 次数记录时长 单位秒
         */
        @Positive
        private Long seconds;
    }

}
