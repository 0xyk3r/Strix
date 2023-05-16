package cn.projectan.strix.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 安炯奕
 * @date 2023/5/15 22:29
 */
@Getter
@Component
@ConfigurationProperties(prefix = "strix.verifier-counter")
public class VerifierCounter {

    private final RedisUtil redisUtil;

    /**
     * 短信验证码配置
     */
    private final Sms sms = new Sms();
    /**
     * 邮箱验证码配置
     */
    private final Email email = new Email();

    private static final String SMS_PREFIX = "strix:util:verifier_counter:sms::";
    private static final String EMAIL_PREFIX = "strix:util:verifier_counter:email::";

    public VerifierCounter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    public boolean isSmsOverLimit(String data) {
        return isOverLimit(SMS_PREFIX + data, sms.getLimit(), sms.getSeconds());
    }

    public boolean isEmailOverLimit(String data) {
        return isOverLimit(EMAIL_PREFIX + data, email.getLimit(), email.getSeconds());
    }

    private boolean isOverLimit(String key, Long limit, Long seconds) {
        if (redisUtil.hasKey(key)) {
            long count = redisUtil.incr(key, 1);
            return count > limit;
        } else {
            redisUtil.set(key, 1, seconds);
        }
        return false;
    }


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
