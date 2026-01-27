package cn.projectan.strix.util.tool;

import cn.projectan.strix.model.properties.system.StrixVerifierCounterProperties;
import cn.projectan.strix.util.common.RedisUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码计数器
 *
 * @author ProjectAn
 * @since 2023/5/15 22:29
 */
@Component
@EnableConfigurationProperties(StrixVerifierCounterProperties.class)
public class VerifierCounter {

    private final RedisUtil redisUtil;
    private final StrixVerifierCounterProperties prop;

    private static final String SMS_PREFIX = "strix:util:verifier_counter:sms::";
    private static final String EMAIL_PREFIX = "strix:util:verifier_counter:email::";

    public VerifierCounter(RedisUtil redisUtil,
                           StrixVerifierCounterProperties prop) {
        this.redisUtil = redisUtil;
        this.prop = prop;
    }

    public boolean isSmsOverLimit(String data) {
        return isOverLimit(SMS_PREFIX + data, prop.getSms().getLimit(), prop.getSms().getSeconds());
    }

    public boolean isEmailOverLimit(String data) {
        return isOverLimit(EMAIL_PREFIX + data, prop.getEmail().getLimit(), prop.getEmail().getSeconds());
    }

    private boolean isOverLimit(String key, Long limit, Long seconds) {
        if (redisUtil.hasKey(key)) {
            long count = redisUtil.incr(key);
            return count > limit;
        } else {
            redisUtil.set(key, 1, seconds);
        }
        return false;
    }

}
