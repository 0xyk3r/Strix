package cn.projectan.strix.controller.wechat.base;

import cn.projectan.strix.model.db.WechatUser;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 安炯奕
 * @date 2021/8/31 13:58
 */
public class BaseWechatController {

    protected WechatUser getLoginWechatUser() {
        WechatUser wechatUser = (WechatUser) getRequest().getAttribute("_LoginWechatUser");
        Assert.notNull(wechatUser, "获取登录信息失败");
        return wechatUser;
    }

    protected HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes != null) {
            return servletRequestAttributes.getRequest();
        } else {
            return null;
        }
    }

    protected String getLoginWechatUserId() {
        return getLoginWechatUser().getId();
    }

}
