package cn.projectan.strix.model.response.system.user;

import cn.projectan.strix.model.db.SystemUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2021/8/27 14:29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserResp {

    private String id;

    private String nickname;

    private Integer status;

    private String phoneNumber;

    public SystemUserResp(SystemUser systemUser) {
        this.id = systemUser.getId();
        this.nickname = systemUser.getNickname();
        this.status = systemUser.getStatus();
        this.phoneNumber = systemUser.getPhoneNumber();
    }

}
