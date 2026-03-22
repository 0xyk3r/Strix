package cn.projectan.strix.model.response.system.user;

import cn.projectan.strix.model.db.system.SystemUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2021/8/27 14:29
 */
@Schema(description = "用户详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserResp {

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户状态")
    private Short status;

    @Schema(description = "手机号码")
    private String phoneNumber;

    public SystemUserResp(SystemUser systemUser) {
        this.id = systemUser.getId();
        this.nickname = systemUser.getNickname();
        this.status = systemUser.getStatus();
        this.phoneNumber = systemUser.getPhoneNumber();
    }

}
