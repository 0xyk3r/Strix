package cn.projectan.strix.model.response.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author 安炯奕
 * @date 2021/5/12 19:17
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

        private Integer managerType;

    }

}
