package cn.projectan.strix.model.response.system.module.captcha;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 验证码获取响应
 *
 * @author ProjectAn
 */
@Data
@Schema(description = "通用 - 验证码 - 获取响应")
public class GetCaptchaResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码唯一标识")
    private String uuid;

    @Schema(description = "原始图片Base64")
    private String originalImageBase64;

    @Schema(description = "滑块图片Base64")
    private String jigsawImageBase64;

    @Schema(description = "加密密钥")
    private String secretKey;

}
