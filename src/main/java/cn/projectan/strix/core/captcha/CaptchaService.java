package cn.projectan.strix.core.captcha;


import cn.projectan.strix.model.other.system.captcha.StrixCaptchaInfoVO;
import cn.projectan.strix.model.response.system.module.captcha.StrixCaptchaResp;

import java.util.Properties;

/**
 * 验证码服务接口
 *
 * @author ProjectAn
 * @since 2024-03-26
 */
public interface CaptchaService {

    /**
     * 配置初始化
     */
    void init(Properties config);

    /**
     * 获取验证码
     *
     * @param strixCaptchaInfoVO 验证码VO
     * @return ResponseResp
     */
    StrixCaptchaResp get(StrixCaptchaInfoVO strixCaptchaInfoVO);

    /**
     * 核对验证码 (前端)
     *
     * @param strixCaptchaInfoVO 验证码VO
     * @return ResponseResp
     */
    StrixCaptchaResp check(StrixCaptchaInfoVO strixCaptchaInfoVO);

    /**
     * 二次校验验证码 (后端)
     *
     * @param strixCaptchaInfoVO 验证码VO
     * @return ResponseResp
     */
    StrixCaptchaResp verification(StrixCaptchaInfoVO strixCaptchaInfoVO);

    /***
     * 验证码类型
     * @return String
     */
    String captchaType();

    /**
     * 历史资源清除 (过期的图片文件，生成的临时图片...)
     *
     * @param config 配置项 控制资源清理的粒度
     */
    void destroy(Properties config);

}
