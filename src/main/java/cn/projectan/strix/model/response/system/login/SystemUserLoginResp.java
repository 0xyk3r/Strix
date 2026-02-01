package cn.projectan.strix.model.response.system.login;

import cn.projectan.strix.core.ss.details.LoginSystemUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2026/1/29 12:36
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserLoginResp {

    private LoginUserInfo info;

    private String token;

    private LocalDateTime tokenExpire;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUserInfo {

        private String id;

        private String nickname;

        private String role;

        private Boolean completion;

        public LoginUserInfo(LoginSystemUser loginSystemUser) {
            this.id = loginSystemUser.getSystemUser().getId();
            this.nickname = loginSystemUser.getSystemUser().getNickname();
            this.role = "user";
            this.completion = true;
        }
    }

}
