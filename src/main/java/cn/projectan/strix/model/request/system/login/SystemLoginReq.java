package cn.projectan.strix.model.request.system.login;

import cn.projectan.strix.core.datamask.DataMask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/5/12 18:42
 */
@Schema(description = "系统登录请求")
@Data
public class SystemLoginReq {

    @Schema(description = "登录账号", example = "admin")
    @NotBlank(message = "{validation.required:field.login.name}")
    @Size(max = 32, message = "{validation.length:field.login.name}")
    private String loginName;

    @Schema(description = "登录密码", example = "password123")
    @NotBlank(message = "{validation.required:field.login.password}")
    @DataMask
    private String loginPassword;

    @Schema(description = "验证码校验参数")
    @NotBlank(message = "{validation.required:field.login.captcha}")
    private String captchaVerification;

}
