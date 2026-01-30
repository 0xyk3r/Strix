package cn.projectan.strix.model.request.system.module.captcha;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024/3/27 1:17
 */
@Data
@Schema(description = "通用 - 验证码 - 获取验证码请求")
public class GetCaptchaReq {

    @Schema(description = "验证码类型", example = "blockPuzzle")
    private String captchaType;

}
