package cn.projectan.strix.core.captcha;


import cn.hutool.crypto.digest.DigestUtil;
import cn.projectan.strix.core.captcha.impl.CaptchaServiceFactory;
import cn.projectan.strix.core.captcha.util.StrixCaptchaAESUtil;
import cn.projectan.strix.core.captcha.util.StrixCaptchaCacheUtil;
import cn.projectan.strix.core.captcha.util.StrixCaptchaImageUtils;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.enums.system.StrixCaptchaTypeEnum;
import cn.projectan.strix.model.other.system.captcha.CaptchaDataVO;
import cn.projectan.strix.model.properties.system.StrixCaptchaProperties;
import cn.projectan.strix.model.response.system.module.captcha.CheckCaptchaResp;
import cn.projectan.strix.model.response.system.module.captcha.GetCaptchaResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * AbstractCaptchaService
 *
 * @author ProjectAn
 * @since 2024-03-26
 */
@Slf4j
public abstract class AbstractCaptchaService implements CaptchaService {

    protected static final String IMAGE_TYPE_PNG = "png";

    // check校验坐标
    protected static final String REDIS_CAPTCHA_KEY = "strix:captcha:running:%s";

    // 后台二次校验坐标
    protected static final String REDIS_SECOND_CAPTCHA_KEY = "strix:captcha:running:second-%s";

    protected static final Long EXPIRES_SECONDS = 2 * 60L;

    protected static final Long EXPIRES_THREE = 3 * 60L;

    // 配置属性 - 使用实例字段避免线程安全问题
    protected String slipOffset = "5";
    protected boolean captchaAesStatus = true;
    protected String cacheType = "local";
    protected int captchaInterferenceOptions = 0;

    private FrequencyLimitHandler limitHandler;

    @Override
    public void init(final Properties config) {
        // 初始化底图 - 统一入口
        StrixCaptchaImageUtils.cacheImage(config.getProperty(StrixCaptchaProperties.Key.ORIGINAL_PATH_JIGSAW));

        slipOffset = config.getProperty(StrixCaptchaProperties.Key.CAPTCHA_SLIP_OFFSET, "5");
        captchaAesStatus = Boolean.parseBoolean(config.getProperty(StrixCaptchaProperties.Key.CAPTCHA_AES_STATUS, "true"));
        cacheType = config.getProperty(StrixCaptchaProperties.Key.CAPTCHA_CACHE_TYPE, "local");
        captchaInterferenceOptions = Integer.parseInt(
                config.getProperty(StrixCaptchaProperties.Key.CAPTCHA_INTERFERENCE_OPTIONS, "0"));

        if (cacheType.equals("local")) {
            // 初始化local缓存
            StrixCaptchaCacheUtil.init(Integer.parseInt(config.getProperty(StrixCaptchaProperties.Key.CAPTCHA_CACHE_MAX_NUMBER, "1000")),
                    Long.parseLong(config.getProperty(StrixCaptchaProperties.Key.CAPTCHA_TIMING_CLEAR_SECOND, "180")));
        }
        if (config.getProperty(StrixCaptchaProperties.Key.HISTORY_DATA_CLEAR_ENABLE, "0").equals("1")) {
            // 历史资源清除开关
            Runtime.getRuntime().addShutdownHook(new Thread(() -> destroy(config)));
        }
        if (config.getProperty(StrixCaptchaProperties.Key.REQ_FREQUENCY_LIMIT_ENABLE, "0").equals("1")) {
            if (limitHandler == null) {
                // 接口分钟内限流开关
                limitHandler = new FrequencyLimitHandler.DefaultLimitHandler(config, getCacheService(cacheType));
            }
        }
        log.info("Strix Captcha: 初始化 <{}> 验证码底图完成.", StrixCaptchaTypeEnum.getCodeDescByCodeValue(captchaType()));
    }

    protected CaptchaCacheService getCacheService(String cacheType) {
        return CaptchaServiceFactory.getCache(cacheType);
    }

    @Override
    public void destroy(Properties config) {
    }

    @Override
    public RetResult<GetCaptchaResp> get(CaptchaDataVO captchaDataVO) {
        if (limitHandler != null) {
            captchaDataVO.setClientUid(getValidateClientId(captchaDataVO));
            RetResult<?> r = limitHandler.validateGet(captchaDataVO);
            if (r != null) {
                return RetBuilder.error(r.getCode(), r.getMsg());
            }
        }
        return null;
    }

    @Override
    public RetResult<CheckCaptchaResp> check(CaptchaDataVO captchaDataVO) {
        if (limitHandler != null) {
            // 服务端参数验证
            captchaDataVO.setClientUid(getValidateClientId(captchaDataVO));
            RetResult<?> r = limitHandler.validateCheck(captchaDataVO);
            if (r != null) {
                return RetBuilder.error(r.getCode(), r.getMsg());
            }
        }
        return null;
    }

    @Override
    public RetResult<Void> verification(CaptchaDataVO captchaDataVO) {
        if (captchaDataVO == null) {
            return RetBuilder.error(RetCode.BAT_REQUEST, "captchaVO不能为空");
        }
        if (!StringUtils.hasText(captchaDataVO.getCaptchaVerification())) {
            return RetBuilder.error(RetCode.BAT_REQUEST, "captchaVerification不能为空");
        }
        if (limitHandler != null) {
            RetResult<?> r = limitHandler.validateVerify(captchaDataVO);
            if (r != null) {
                return RetBuilder.error(r.getCode(), r.getMsg());
            }
        }
        return null;
    }

    protected boolean validatedReq(RetResult<?> resp) {
        return resp == null || resp.getCode() == RetCode.SUCCESS;
    }

    protected String getValidateClientId(CaptchaDataVO req) {
        // 以服务端获取的客户端标识 做识别标志
        if (StringUtils.hasText(req.getBrowserInfo())) {
            return DigestUtil.md5Hex(req.getBrowserInfo());
        }
        // 以客户端Ui组件id做识别标志
        if (StringUtils.hasText(req.getClientUid())) {
            return req.getClientUid();
        }
        return null;
    }

    protected void afterValidateFail(CaptchaDataVO data) {
        if (limitHandler != null) {
            // 验证失败 分钟内计数
            String fails = String.format(FrequencyLimitHandler.LIMIT_KEY, "FAIL", data.getClientUid());
            CaptchaCacheService cs = getCacheService(cacheType);
            if (!cs.exists(fails)) {
                cs.set(fails, "1", 60);
            }
            cs.increment(fails, 1);
        }
    }

    /**
     * 解密前端坐标aes加密
     *
     * @param point 前端坐标
     * @return 解密后的坐标
     * @throws Exception 异常
     */
    public static String decrypt(String point, String key) throws Exception {
        return StrixCaptchaAESUtil.aesDecrypt(point, key);
    }

}
