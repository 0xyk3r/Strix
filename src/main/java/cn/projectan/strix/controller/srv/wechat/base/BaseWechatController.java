package cn.projectan.strix.controller.srv.wechat.base;

import cn.projectan.strix.controller.BaseController;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.system.SecurityUtil;
import org.springframework.util.Assert;

/**
 * 微信端基础控制器
 *
 * @author ProjectAn
 * @since 2021/8/31 13:58
 */
public class BaseWechatController extends BaseController {

    /**
     * 获取登录用户信息
     */
    protected SystemUser getLoginSystemUser() {
        SystemUser systemUser = SecurityUtil.getSystemUser();
        Assert.notNull(systemUser, I18nUtil.failed("field.loginInfo"));
        return systemUser;
    }

    /**
     * 获取登录用户 ID
     */
    protected String getLoginSystemUserId() {
        return getLoginSystemUser().getId();
    }

}
