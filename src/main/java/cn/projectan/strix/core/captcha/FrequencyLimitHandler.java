package cn.projectan.strix.core.captcha;


import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.other.system.captcha.CaptchaDataVO;
import cn.projectan.strix.model.properties.system.StrixCaptchaProperties;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Properties;

public interface FrequencyLimitHandler {

    String LIMIT_KEY = "strix:captcha:limit:%s:%s";

    /**
     * get 接口限流
     *
     * @param captchaDataVO captchaVO
     * @return RetResult<?> - null表示通过, 非null表示被限流
     */
    RetResult<?> validateGet(CaptchaDataVO captchaDataVO);

    /**
     * check 接口限流
     *
     * @param captchaDataVO captchaVO
     * @return RetResult<?> - null表示通过, 非null表示被限流
     */
    RetResult<?> validateCheck(CaptchaDataVO captchaDataVO);

    /**
     * verify 接口限流
     *
     * @param captchaDataVO captchaVO
     * @return RetResult<?> - null表示通过, 非null表示被限流
     */
    RetResult<?> validateVerify(CaptchaDataVO captchaDataVO);

    /***
     * 验证码接口限流:
     *      客户端ClientUid 组件实例化时设置一次，如：场景码+UUID，客户端可以本地缓存,保证一个组件只有一个值
     * 针对同一个客户端的请求，做如下限制:
     * get
     * 	 1分钟内check失败5次，锁定5分钟
     * 	 1分钟内不能超过120次。
     * check:
     *   1分钟内不超过600次
     * verify:
     *   1分钟内不超过600次
     */
    class DefaultLimitHandler implements FrequencyLimitHandler {
        private final Properties config;
        private final CaptchaCacheService cacheService;

        public DefaultLimitHandler(Properties config, CaptchaCacheService cacheService) {
            this.config = config;
            this.cacheService = cacheService;
        }

        private String getClientCId(CaptchaDataVO input, String type) {
            return String.format(LIMIT_KEY, type, input.getClientUid());
        }

        @Override
        public RetResult<?> validateGet(CaptchaDataVO d) {
            // 无客户端身份标识，不限制
            if (!StringUtils.hasText(d.getClientUid())) {
                return null;
            }
            String getKey = getClientCId(d, "GET");
            String lockKey = getClientCId(d, "LOCK");
            // 失败次数过多，锁定
            if (Objects.nonNull(cacheService.get(lockKey))) {
                return RetBuilder.error(RetCode.BAT_REQUEST, "验证码获取请求过于频繁，请稍后再试");
            }
            String getCount = cacheService.get(getKey);
            if (Objects.isNull(getCount)) {
                cacheService.set(getKey, "1", 60);
                getCount = "1";
            }
            cacheService.increment(getKey, 1);
            // 1分钟内请求次数过多
            if (Long.parseLong(getCount) > Long.parseLong(config.getProperty(StrixCaptchaProperties.Key.REQ_GET_MINUTE_LIMIT, "120"))) {
                return RetBuilder.error(RetCode.BAT_REQUEST, "验证码获取请求过于频繁，请稍后再试");
            }

            // 失败次数验证
            String failKey = getClientCId(d, "FAIL");
            String failCount = cacheService.get(failKey);
            // 没有验证失败，通过校验
            if (Objects.isNull(failCount)) {
                return null;
            }
            // 1分钟内失败5次
            if (Long.parseLong(failCount) > Long.parseLong(config.getProperty(StrixCaptchaProperties.Key.REQ_GET_LOCK_LIMIT, "5"))) {
                // get接口锁定5分钟
                cacheService.set(lockKey, "1", Long.parseLong(config.getProperty(StrixCaptchaProperties.Key.REQ_GET_LOCK_SECONDS, "300")));
                return RetBuilder.error(RetCode.BAT_REQUEST, "验证码获取请求过于频繁，请稍后再试");
            }
            return null;
        }

        @Override
        public RetResult<?> validateCheck(CaptchaDataVO d) {
            // 无客户端身份标识，不限制
            if (!StringUtils.hasText(d.getClientUid())) {
                return null;
            }
            String key = getClientCId(d, "CHECK");
            String v = cacheService.get(key);
            if (Objects.isNull(v)) {
                cacheService.set(key, "1", 60);
                v = "1";
            }
            cacheService.increment(key, 1);
            if (Long.parseLong(v) > Long.parseLong(config.getProperty(StrixCaptchaProperties.Key.REQ_CHECK_MINUTE_LIMIT, "600"))) {
                return RetBuilder.error(RetCode.BAT_REQUEST, "验证码校验请求过于频繁，请稍后再试");
            }
            return null;
        }

        @Override
        public RetResult<?> validateVerify(CaptchaDataVO d) {
            String key = getClientCId(d, "VERIFY");
            String v = cacheService.get(key);
            if (Objects.isNull(v)) {
                cacheService.set(key, "1", 60);
                v = "1";
            }
            cacheService.increment(key, 1);
            if (Long.parseLong(v) > Long.parseLong(config.getProperty(StrixCaptchaProperties.Key.REQ_VALIDATE_MINUTE_LIMIT, "600"))) {
                return RetBuilder.error(RetCode.BAT_REQUEST, "验证码验证请求过于频繁，请稍后再试");
            }
            return null;
        }
    }

}
