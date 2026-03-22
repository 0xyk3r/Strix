package cn.projectan.strix.model.request.system.manager;

import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.request.base.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/6/11 18:02
 */
@Data
public class SystemManagerListReq extends BasePageReq<SystemManager> {

    @Size(max = 64)
    private String keyword;

    private Short status;

    private Short type;

    private String regionId;

    private String roleId;

}
