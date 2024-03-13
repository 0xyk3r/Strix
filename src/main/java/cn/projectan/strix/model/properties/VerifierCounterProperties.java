package cn.projectan.strix.model.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author ProjectAn
 * @date 2023/5/18 15:21
 */
@Getter
@ConfigurationProperties(prefix = "strix.verifier-counter")
public class VerifierCounterProperties {

    /**
     * 短信验证码配置
     */
    private final Sms sms = new Sms();
    /**
     * 邮箱验证码配置
     */
    private final Email email = new Email();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sms {
        /**
         * 限制次数
         */
        private Long limit;
        /**
         * 次数记录时长 单位秒
         */
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
        private Long limit;
        /**
         * 次数记录时长 单位秒
         */
        private Long seconds;
    }

}
