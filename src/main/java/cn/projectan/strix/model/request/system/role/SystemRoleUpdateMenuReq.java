package cn.projectan.strix.model.request.system.role;

import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/7/23 17:17
 */
@Data
public class SystemRoleUpdateMenuReq {

    private String menuIds;

    private String permissionIds;

}
