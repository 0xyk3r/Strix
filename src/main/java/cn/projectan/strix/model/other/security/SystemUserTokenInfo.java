package cn.projectan.strix.model.other.security;

import lombok.Data;

/**
 * @author ProjectAn
 * @date 2024/4/5 下午10:51
 */
@Data
public class SystemUserTokenInfo extends BaseTokenInfo {

    private String nickname;

    private String phoneNumber;

    private Integer status;

    public SystemUserTokenInfo() {
    }

    public SystemUserTokenInfo(Integer uType, String uid, String nickname, String phoneNumber, Integer status) {
        super(uType, uid);
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

}
