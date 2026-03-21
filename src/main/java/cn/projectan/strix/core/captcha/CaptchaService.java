package cn.projectan.strix.core.captcha;


import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.other.system.captcha.CaptchaData;
import cn.projectan.strix.model.response.system.module.captcha.CheckCaptchaResp;
import cn.projectan.strix.model.response.system.module.captcha.GetCaptchaResp;

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
     * @param captchaDataVO 验证码数据
     * @return RetResult<CaptchaGetResp>
     */
    RetResult<GetCaptchaResp> get(CaptchaData captchaDataVO);

    /**
     * 核对验证码 (前端)
     *
     * @param captchaDataVO 验证码数据
     * @return RetResult<CaptchaCheckResp>
     */
    RetResult<CheckCaptchaResp> check(CaptchaData captchaDataVO);

    /**
     * 二次校验验证码 (后端)
     *
     * @param captchaDataVO 验证码数据
     * @return RetResult<Void>
     */
    RetResult<Void> verification(CaptchaData captchaDataVO);

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
