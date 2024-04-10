package cn.projectan.strix.core.captcha.impl;


import cn.projectan.strix.core.captcha.AbstractCaptchaService;
import cn.projectan.strix.core.captcha.CaptchaService;
import cn.projectan.strix.model.enums.CaptchaRepCodeEnum;
import cn.projectan.strix.model.other.captcha.CaptchaInfoVO;
import cn.projectan.strix.model.response.module.captcha.StrixCaptchaResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * 默认验证码服务
 *
 * @author ProjectAn
 * @date 2024/3/30 13:00
 */
@Slf4j
public class DefaultCaptchaServiceImpl extends AbstractCaptchaService {

    @Override
    public String captchaType() {
        return "default";
    }

    @Override
    public void init(Properties config) {
        for (String s : CaptchaServiceFactory.instances.keySet()) {
            if (captchaType().equals(s)) {
                continue;
            }
            getService(s).init(config);
        }
    }

    @Override
    public void destroy(Properties config) {
        for (String s : CaptchaServiceFactory.instances.keySet()) {
            if (captchaType().equals(s)) {
                continue;
            }
            getService(s).destroy(config);
        }
    }

    private CaptchaService getService(String captchaType) {
        return CaptchaServiceFactory.instances.get(captchaType);
    }

    @Override
    public StrixCaptchaResp get(CaptchaInfoVO captchaInfoVO) {
        if (captchaInfoVO == null) {
            return CaptchaRepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (!StringUtils.hasText(captchaInfoVO.getCaptchaType())) {
            return CaptchaRepCodeEnum.NULL_ERROR.parseError("类型");
        }
        return getService(captchaInfoVO.getCaptchaType()).get(captchaInfoVO);
    }

    @Override
    public StrixCaptchaResp check(CaptchaInfoVO captchaInfoVO) {
        if (captchaInfoVO == null) {
            return CaptchaRepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (!StringUtils.hasText(captchaInfoVO.getCaptchaType())) {
            return CaptchaRepCodeEnum.NULL_ERROR.parseError("类型");
        }
        if (!StringUtils.hasText(captchaInfoVO.getToken())) {
            return CaptchaRepCodeEnum.NULL_ERROR.parseError("token");
        }
        return getService(captchaInfoVO.getCaptchaType()).check(captchaInfoVO);
    }

    @Override
    public StrixCaptchaResp verification(CaptchaInfoVO captchaInfoVO) {
        if (captchaInfoVO == null) {
            return CaptchaRepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (!StringUtils.hasText(captchaInfoVO.getCaptchaVerification())) {
            return CaptchaRepCodeEnum.NULL_ERROR.parseError("二次校验参数");
        }
        try {
            String codeKey = String.format(REDIS_SECOND_CAPTCHA_KEY, captchaInfoVO.getCaptchaVerification());
            if (!CaptchaServiceFactory.getCache(cacheType).exists(codeKey)) {
                return StrixCaptchaResp.errorMsg(CaptchaRepCodeEnum.API_CAPTCHA_INVALID);
            }
            // 二次校验取值后，即刻失效
            CaptchaServiceFactory.getCache(cacheType).delete(codeKey);
        } catch (Exception e) {
            log.error("Strix Captcha: 验证码坐标解析失败", e);
            return StrixCaptchaResp.errorMsg(e.getMessage());
        }
        return StrixCaptchaResp.success();
    }

}
