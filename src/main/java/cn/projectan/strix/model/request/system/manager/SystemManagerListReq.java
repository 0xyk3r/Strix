package cn.projectan.strix.model.request.system.manager;

import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/6/11 18:02
 */
@Data
public class SystemManagerListReq extends BasePageReq<SystemManager> {

    private String keyword;

    private Integer managerStatus;

    private Integer managerType;

    private String regionId;

}
