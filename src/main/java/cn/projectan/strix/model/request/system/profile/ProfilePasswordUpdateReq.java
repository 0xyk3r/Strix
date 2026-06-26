package cn.projectan.strix.model.request.system.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 *
 * @author ProjectAn
 */
@Schema(description = "修改密码请求")
@Data
public class ProfilePasswordUpdateReq {

    @Schema(description = "当前密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "当前密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度应在 6~32 位之间")
    private String newPassword;

}
