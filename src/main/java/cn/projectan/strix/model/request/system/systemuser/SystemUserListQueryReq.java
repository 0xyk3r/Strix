package cn.projectan.strix.model.request.system.systemuser;

import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.request.base.BasePageQueryReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/8/27 15:44
 */
@Data
public class SystemUserListQueryReq extends BasePageQueryReq<SystemUser> {

    private String keyword;

    private Integer status;

}
