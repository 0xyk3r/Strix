package cn.projectan.strix.model.properties.system;


import cn.projectan.strix.model.enums.system.StrixCaptchaTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strix 验证码配置
 */
@Data
@ConfigurationProperties(StrixCaptchaProperties.PREFIX)
public class StrixCaptchaProperties {
    public static final String PREFIX = "strix.captcha";

    /**
     * 验证码类型
     */
    private StrixCaptchaTypeEnum type = StrixCaptchaTypeEnum.BLOCK_PUZZLE;

    /**
     * 滑动拼图底图路径
     */
    private String jigsaw = "";

    /**
     * 右下角水印文字 (我的水印)
     */
    private String waterMark = "Strix";

    /**
     * 右下角水印字体 (文泉驿正黑)
     */
    private String waterFont = "WenQuanZhengHei.ttf";

    /**
     * 校验滑动拼图允许误差偏移量 (默认5像素)
     */
    private String slipOffset = "5";

    /**
     * aes加密坐标开启或者禁用 (true|false)
     */
    private Boolean aesStatus = true;

    /**
     * 滑块干扰项 (0/1/2)
     */
    private String interferenceOptions = "0";

    /**
     * local 缓存的阈值
     */
    private String cacheNumber = "1000";

    /**
     * 定时清理过期local缓存 (秒)
     */
    private String timingClear = "180";

    /**
     * 缓存类型
     */
    private StorageType cacheType = StorageType.local;

    /**
     * 历史数据清除开关
     */
    private Boolean historyDataClearEnable = false;

    /**
     * 一分钟内接口请求次数限制开关
     */
    private Boolean reqFrequencyLimitEnable = false;

    /**
     * 一分钟内 check 接口失败次数
     */
    private int reqGetLockLimit = 5;

    /**
     * 失败后锁定时间(秒)
     */
    private int reqGetLockSeconds = 300;

    /**
     * get 接口一分钟内限制访问数
     */
    private int reqGetMinuteLimit = 100;

    /**
     * check 接口一分钟内限制访问数
     */
    private int reqCheckMinuteLimit = 100;

    /**
     * verify 接口一分钟内限制访问数
     */
    private int reqVerifyMinuteLimit = 100;

    public enum StorageType {
        /**
         * 内存.
         */
        local,
        /**
         * redis.
         */
        redis,
        /**
         * 其他.
         */
        other,
    }

    /**
     * 配置键常量
     */
    public interface Key {
        String ORIGINAL_PATH_JIGSAW = "captcha.captchaOriginalPath.jigsaw";
        String CAPTCHA_CACHE_TYPE = "captcha.cacheType";
        String CAPTCHA_WATER_MARK = "captcha.water.mark";
        String CAPTCHA_TYPE = "captcha.type";
        String CAPTCHA_INTERFERENCE_OPTIONS = "captcha.interference.options";
        String CAPTCHA_SLIP_OFFSET = "captcha.slip.offset";
        String CAPTCHA_AES_STATUS = "captcha.aes.status";
        String CAPTCHA_WATER_FONT = "captcha.water.font";
        String CAPTCHA_CACHE_MAX_NUMBER = "captcha.cache.number";
        String CAPTCHA_TIMING_CLEAR_SECOND = "captcha.timing.clear";
        String HISTORY_DATA_CLEAR_ENABLE = "captcha.history.data.clear.enable";
        String REQ_FREQUENCY_LIMIT_ENABLE = "captcha.req.frequency.limit.enable";
        String REQ_GET_MINUTE_LIMIT = "captcha.req.get.minute.limit";
        String REQ_GET_LOCK_LIMIT = "captcha.req.get.lock.limit";
        String REQ_GET_LOCK_SECONDS = "captcha.req.get.lock.seconds";
        String REQ_VALIDATE_MINUTE_LIMIT = "captcha.req.verify.minute.limit";
        String REQ_CHECK_MINUTE_LIMIT = "captcha.req.check.minute.limit";
    }
}
