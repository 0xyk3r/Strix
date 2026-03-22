package cn.projectan.strix.model.request.system.login;

import cn.projectan.strix.core.datamask.DataMask;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/5/12 18:42
 */
@Data
public class SystemLoginReq {

    @NotBlank(message = "登录名不能为空")
    @Size(max = 32, message = "登录名长度不能超过32个字符")
    private String loginName;

    @NotBlank(message = "密码不能为空")
    @DataMask
    private String loginPassword;

    @NotBlank(message = "验证码不能为空")
    private String captchaVerification;

}
