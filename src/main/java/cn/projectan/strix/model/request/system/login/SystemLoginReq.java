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
    @NotBlank(message = "登录名不能为空")
    @Size(max = 32, message = "登录名长度不能超过32个字符")
    private String loginName;

    @Schema(description = "登录密码", example = "password123")
    @NotBlank(message = "密码不能为空")
    @DataMask
    private String loginPassword;

    @Schema(description = "验证码校验参数")
    @NotBlank(message = "验证码不能为空")
    private String captchaVerification;

}
