package cn.projectan.strix.model.request.module.captcha;

import lombok.Data;

/**
 * @author ProjectAn
 * @date 2024/3/27 1:17
 */
@Data
public class CheckCaptchaReq {

    private String captchaType;

    private String pointJson;

    private String token;

}
