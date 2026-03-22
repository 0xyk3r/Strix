package cn.projectan.strix.model.response.system.login;

import cn.projectan.strix.core.ss.details.LoginSystemUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2026/1/29 12:36
 */
@Schema(description = "用户登录响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserLoginResp {

    @Schema(description = "用户信息")
    private LoginUserInfo info;

    @Schema(description = "登录令牌")
    private String token;

    @Schema(description = "令牌过期时间")
    private LocalDateTime tokenExpire;

    @Schema(description = "登录用户信息")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUserInfo {

        @Schema(description = "用户ID")
        private String id;

        @Schema(description = "用户昵称")
        private String nickname;

        @Schema(description = "用户角色")
        private String role;

        @Schema(description = "是否完善信息")
        private Boolean completion;

        public LoginUserInfo(LoginSystemUser loginSystemUser) {
            this.id = loginSystemUser.getSystemUser().getId();
            this.nickname = loginSystemUser.getSystemUser().getNickname();
            this.role = "user";
            this.completion = true;
        }
    }

}
