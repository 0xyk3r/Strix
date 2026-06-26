package cn.projectan.strix.model.request.system.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改昵称请求
 *
 * @author ProjectAn
 */
@Schema(description = "修改昵称请求")
@Data
public class ProfileNicknameUpdateReq {

    @Schema(description = "新昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "{validation.required:field.user.nickname}")
    @Size(min = 2, max = 20, message = "{validation.size:field.user.nickname}")
    private String nickname;

}
