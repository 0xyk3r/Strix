package cn.projectan.strix.model.request.system.user;

import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/8/27 15:44
 */
@Data
public class SystemUserListReq extends BasePageReq<SystemUser> {

    private String keyword;

    private Integer status;

}
