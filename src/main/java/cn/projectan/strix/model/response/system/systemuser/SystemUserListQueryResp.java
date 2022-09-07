package cn.projectan.strix.model.response.system.systemuser;

import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.response.base.BasePageResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2021/8/27 14:23
 */
@Getter
@NoArgsConstructor
public class SystemUserListQueryResp extends BasePageResp {

    private List<SystemUserItem> systemUserList = new ArrayList<>();

    public SystemUserListQueryResp(List<SystemUser> users, Long total) {
        systemUserList = users.stream().map(SystemUserItem::new).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemUserItem {

        private String id;

        private String nickname;

        private Integer status;

        private String phoneNumber;

        public SystemUserItem(SystemUser systemUser) {
            this.id = systemUser.getId();
            this.nickname = systemUser.getNickname();
            this.status = systemUser.getStatus();
            this.phoneNumber = systemUser.getPhoneNumber();
        }
    }

}
