package cn.projectan.strix.model.response.system.systemuser;

import cn.projectan.strix.model.db.SystemUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 安炯奕
 * @date 2021/8/27 14:29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserQueryByIdResp {

    private String id;

    private String nickname;

    private Integer status;

    private String phoneNumber;

    public SystemUserQueryByIdResp(SystemUser systemUser) {
        this.id = systemUser.getId();
        this.nickname = systemUser.getNickname();
        this.status = systemUser.getStatus();
        this.phoneNumber = systemUser.getPhoneNumber();
    }

}
