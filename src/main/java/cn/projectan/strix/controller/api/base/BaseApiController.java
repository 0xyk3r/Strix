package cn.projectan.strix.controller.api.base;

import cn.projectan.strix.controller.BaseController;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.util.I18nUtil;
import cn.projectan.strix.util.SecurityUtils;
import org.springframework.util.Assert;

/**
 * 用户端基础API
 *
 * @author ProjectAn
 * @since 2023/4/6 13:43
 */
public class BaseApiController extends BaseController {

    /**
     * 获取当前登录用户
     */
    protected SystemUser getLoginUser() {
        SystemUser systemUser = (SystemUser) SecurityUtils.getAuthentication().getPrincipal();
        Assert.notNull(systemUser, I18nUtil.get("error.noLoginInfo"));
        return systemUser;
    }

    /**
     * 获取当前登录用户ID
     */
    protected String getLoginUserId() {
        return getLoginUser().getId();
    }

}
