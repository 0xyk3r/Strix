package cn.projectan.strix.controller.api.base;

import cn.projectan.strix.controller.BaseController;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.utils.SecurityUtils;
import org.springframework.util.Assert;

/**
 * @author ProjectAn
 * @date 2023/4/6 13:43
 */
public class BaseApiController extends BaseController {

    protected SystemUser getLoginUser() {
        SystemUser systemUser = (SystemUser) SecurityUtils.getAuthentication().getPrincipal();
        Assert.notNull(systemUser, "获取登录信息失败");
        return systemUser;
    }

    protected String getLoginUserId() {
        return getLoginUser().getId();
    }

}
