package cn.projectan.strix.model.response.system.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2021/5/12 19:17
 */
@Schema(description = "管理员登录响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemManagerLoginResp {

    @Schema(description = "管理员信息")
    private LoginManagerInfo info;

    @Schema(description = "登录令牌")
    private String token;

    @Schema(description = "令牌过期时间")
    private LocalDateTime tokenExpire;

    @Schema(description = "登录管理员信息")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginManagerInfo {

        @Schema(description = "管理员ID")
        private String id;

        @Schema(description = "管理员昵称")
        private String nickname;

        @Schema(description = "管理员类型")
        private Short type;

        @Schema(description = "所属区域ID")
        private String regionId;

        // 存储的是菜单权限Key和按钮权限Key的集合
        @Schema(description = "权限标识列表")
        private List<String> permissionKeys;

        @Schema(description = "DiceBear 头像配置 JSON，为 null 时前端以管理员 ID 为 seed 自动生成")
        private String avatarConfig;

        public LoginManagerInfo(String id, String nickname, Short type, String regionId) {
            this.id = id;
            this.nickname = nickname;
            this.type = type;
            this.regionId = regionId;
        }
    }

}
