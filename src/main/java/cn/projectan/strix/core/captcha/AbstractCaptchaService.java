package cn.projectan.strix.core.captcha;


import cn.projectan.strix.core.captcha.impl.CaptchaServiceFactory;
import cn.projectan.strix.core.captcha.util.StrixCaptchaAESUtil;
import cn.projectan.strix.core.captcha.util.StrixCaptchaCacheUtil;
import cn.projectan.strix.core.captcha.util.StrixCaptchaImageUtils;
import cn.projectan.strix.core.captcha.util.StrixCaptchaMD5Util;
import cn.projectan.strix.model.constant.system.StrixCaptchaConst;
import cn.projectan.strix.model.enums.system.StrixCaptchaRepCodeEnum;
import cn.projectan.strix.model.enums.system.StrixCaptchaTypeEnum;
import cn.projectan.strix.model.other.system.captcha.StrixCaptchaInfoVO;
import cn.projectan.strix.model.response.system.module.captcha.StrixCaptchaResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    protected static final int HAN_ZI_SIZE = 25;

    // check校验坐标
    protected static final String REDIS_CAPTCHA_KEY = "strix:captcha:running:%s";

    // 后台二次校验坐标
    protected static final String REDIS_SECOND_CAPTCHA_KEY = "strix:captcha:running:second-%s";

    protected static final Long EXPIRES_SECONDS = 2 * 60L;

    protected static final Long EXPIRES_THREE = 3 * 60L;

    protected static String slipOffset = "5";

    protected static Boolean captchaAesStatus = true;

    protected static String cacheType = "local";

    protected static int captchaInterferenceOptions = 0;

    @Override
    public void init(final Properties config) {
        // 初始化底图
        boolean captchaInitOriginal = Boolean.parseBoolean(config.getProperty(StrixCaptchaConst.CAPTCHA_INIT_ORIGINAL));
        if (!captchaInitOriginal) {
            StrixCaptchaImageUtils.cacheImage(config.getProperty(StrixCaptchaConst.ORIGINAL_PATH_JIGSAW),
                    config.getProperty(StrixCaptchaConst.ORIGINAL_PATH_PIC_CLICK));
        }
        slipOffset = config.getProperty(StrixCaptchaConst.CAPTCHA_SLIP_OFFSET, "5");
        captchaAesStatus = Boolean.parseBoolean(config.getProperty(StrixCaptchaConst.CAPTCHA_AES_STATUS, "true"));
        cacheType = config.getProperty(StrixCaptchaConst.CAPTCHA_CACHE_TYPE, "local");
        captchaInterferenceOptions = Integer.parseInt(
                config.getProperty(StrixCaptchaConst.CAPTCHA_INTERFERENCE_OPTIONS, "0"));

        // 部署在 Linux 中，如果没有安装中文字段，水印和点选文字，中文无法显示，
        // 通过加载resources下的font字体解决，无需在 Linux 中安装字体
        // loadWaterMarkFont();

        if (cacheType.equals("local")) {
            // 初始化local缓存
            StrixCaptchaCacheUtil.init(Integer.parseInt(config.getProperty(StrixCaptchaConst.CAPTCHA_CACHE_MAX_NUMBER, "1000")),
                    Long.parseLong(config.getProperty(StrixCaptchaConst.CAPTCHA_TIMING_CLEAR_SECOND, "180")));
        }
        if (config.getProperty(StrixCaptchaConst.HISTORY_DATA_CLEAR_ENABLE, "0").equals("1")) {
            // 历史资源清除开关
            Runtime.getRuntime().addShutdownHook(new Thread(() -> destroy(config)));
        }
        if (config.getProperty(StrixCaptchaConst.REQ_FREQUENCY_LIMIT_ENABLE, "0").equals("1")) {
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

    private static FrequencyLimitHandler limitHandler;

    @Override
    public StrixCaptchaResp get(StrixCaptchaInfoVO strixCaptchaInfoVO) {
        if (limitHandler != null) {
            strixCaptchaInfoVO.setClientUid(getValidateClientId(strixCaptchaInfoVO));
            return limitHandler.validateGet(strixCaptchaInfoVO);
        }
        return null;
    }

    @Override
    public StrixCaptchaResp check(StrixCaptchaInfoVO strixCaptchaInfoVO) {
        if (limitHandler != null) {
            // 服务端参数验证
            strixCaptchaInfoVO.setClientUid(getValidateClientId(strixCaptchaInfoVO));
            return limitHandler.validateCheck(strixCaptchaInfoVO);
        }
        return null;
    }

    @Override
    public StrixCaptchaResp verification(StrixCaptchaInfoVO strixCaptchaInfoVO) {
        if (strixCaptchaInfoVO == null) {
            return StrixCaptchaRepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (!StringUtils.hasText(strixCaptchaInfoVO.getCaptchaVerification())) {
            return StrixCaptchaRepCodeEnum.NULL_ERROR.parseError("captchaVerification");
        }
        if (limitHandler != null) {
            return limitHandler.validateVerify(strixCaptchaInfoVO);
        }
        return null;
    }

    protected boolean validatedReq(StrixCaptchaResp resp) {
        return resp == null || resp.isSuccess();
    }

    protected String getValidateClientId(StrixCaptchaInfoVO req) {
        // 以服务端获取的客户端标识 做识别标志
        if (StringUtils.hasText(req.getBrowserInfo())) {
            return StrixCaptchaMD5Util.md5(req.getBrowserInfo());
        }
        // 以客户端Ui组件id做识别标志
        if (StringUtils.hasText(req.getClientUid())) {
            return req.getClientUid();
        }
        return null;
    }

    protected void afterValidateFail(StrixCaptchaInfoVO data) {
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

    public static boolean base64StrToImage(String imgStr, String path) {
        if (imgStr == null) {
            return false;
        }

        Base64.Decoder decoder = Base64.getDecoder();
        try {
            // 解密
            byte[] b = decoder.decode(imgStr);
            // 处理数据
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += (byte) 256;
                }
            }
            // 文件夹不存在则自动创建
            File tempFile = new File(path);
            if (!tempFile.getParentFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.getParentFile().mkdirs();
            }
            OutputStream out = new FileOutputStream(tempFile);
            out.write(b);
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            return false;
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

    protected static int getEnOrChLength(String s) {
        int enCount = 0;
        int chCount = 0;
        for (int i = 0; i < s.length(); i++) {
            int length = String.valueOf(s.charAt(i)).getBytes(StandardCharsets.UTF_8).length;
            if (length > 1) {
                chCount++;
            } else {
                enCount++;
            }
        }
        int chOffset = (HAN_ZI_SIZE / 2) * chCount + 5;
        int enOffset = enCount * 8;
        return chOffset + enOffset;
    }

}
