package cn.projectan.strix.model.response.system.user;

import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2021/8/27 14:23
 */
@Schema(description = "用户列表响应")
@Getter
@NoArgsConstructor
public class SystemUserListResp extends BasePageResp {

    @Schema(description = "用户列表")
    private List<SystemUserItem> systemUserList = new ArrayList<>();

    public SystemUserListResp(List<SystemUser> users, Long total) {
        systemUserList = users.stream().map(SystemUserItem::new).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "用户列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemUserItem {

        @Schema(description = "用户ID")
        private String id;

        @Schema(description = "用户昵称")
        private String nickname;

        @Schema(description = "用户状态")
        private Short status;

        @Schema(description = "手机号码")
        private String phoneNumber;

        public SystemUserItem(SystemUser systemUser) {
            this.id = systemUser.getId();
            this.nickname = systemUser.getNickname();
            this.status = systemUser.getStatus();
            this.phoneNumber = systemUser.getPhoneNumber();
        }
    }

}
