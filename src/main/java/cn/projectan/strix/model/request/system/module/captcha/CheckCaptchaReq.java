package cn.projectan.strix.model.request.system.module.captcha;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024/3/27 1:17
 */
@Data
@Schema(description = "通用 - 验证码 - 校验验证码请求")
public class CheckCaptchaReq {

    @Schema(description = "验证码类型", example = "blockPuzzle")
    private String captchaType;

    @Schema(description = "点位信息 JSON 字符串")
    private String pointJson;

    @Schema(description = "验证码标识")
    private String uuid;

}
