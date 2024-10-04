package cn.projectan.strix.model.response.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2021/5/12 19:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLoginResp {

    private LoginManagerInfo info;

    private String token;

    private LocalDateTime tokenExpire;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginManagerInfo {

        private String id;

        private String nickname;

        private Integer type;

        // 存储的是菜单权限Key和按钮权限Key的集合
        private List<String> permissionKeys;

        public LoginManagerInfo(String id, String nickname, Integer type) {
            this.id = id;
            this.nickname = nickname;
            this.type = type;
        }
    }

}
