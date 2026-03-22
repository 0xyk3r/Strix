package cn.projectan.strix.model.request.system.module.captcha;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2024/3/27 1:17
 */
@Data
@Schema(description = "通用 - 验证码 - 校验验证码请求")
public class CheckCaptchaReq {

    @NotBlank(message = "验证码类型不能为空")
    @Schema(description = "验证码类型", example = "blockPuzzle")
    private String captchaType;

    @NotBlank(message = "点位信息不能为空")
    @Schema(description = "点位信息 JSON 字符串")
    private String pointJson;

    @NotBlank(message = "验证码标识不能为空")
    @Schema(description = "验证码标识")
    private String uuid;

}
