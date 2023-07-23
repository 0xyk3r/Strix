package cn.projectan.strix.controller.wechat.base;

import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.utils.SecurityUtils;
import org.springframework.util.Assert;

/**
 * @author 安炯奕
 * @date 2021/8/31 13:58
 */
public class BaseWechatController {

    protected SystemUser getLoginSystemUser() {
        SystemUser systemUser = (SystemUser) SecurityUtils.getAuthentication().getPrincipal();
        Assert.notNull(systemUser, "获取登录信息失败");
        return systemUser;
    }

    protected String getLoginWechatUserId() {
        return getLoginSystemUser().getId();
    }

}
