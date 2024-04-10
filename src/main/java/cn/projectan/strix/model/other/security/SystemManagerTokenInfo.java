package cn.projectan.strix.model.other.security;

import lombok.Data;

import java.util.List;

/**
 * @author ProjectAn
 * @date 2024/4/5 下午10:51
 */
@Data
public class SystemManagerTokenInfo extends BaseTokenInfo {

    private String nickname;

    private Integer status;

    private Integer type;

    private String regionId;

    private List<String> menuKeys;

    private List<String> permissionKeys;

    public SystemManagerTokenInfo() {
    }

    public SystemManagerTokenInfo(Integer uType, String uid, String nickname, Integer status, Integer type, String regionId, List<String> menuKeys, List<String> permissionKeys) {
        super(uType, uid);
        this.nickname = nickname;
        this.status = status;
        this.type = type;
        this.regionId = regionId;
        this.menuKeys = menuKeys;
        this.permissionKeys = permissionKeys;
    }
}
