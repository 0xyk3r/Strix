package cn.projectan.strix.model.response.system.module.captcha;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 验证码校验响应
 *
 * @author ProjectAn
 */
@Data
@Schema(description = "通用 - 验证码 - 校验响应")
public class CheckCaptchaResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "校验结果")
    private Boolean result;

    @Schema(description = "后台二次校验参数")
    private String captchaVerification;

}
