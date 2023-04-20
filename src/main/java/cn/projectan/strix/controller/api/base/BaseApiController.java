package cn.projectan.strix.controller.api.base;

import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.utils.SecurityUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 安炯奕
 * @date 2023/4/6 13:43
 */
public class BaseApiController {

    protected SystemUser getLoginUser() {
        SystemUser systemUser = (SystemUser) SecurityUtils.getAuthentication().getPrincipal();
        Assert.notNull(systemUser, "获取登录信息失败");
        return systemUser;
    }

    protected String getLoginUserId() {
        return getLoginUser().getId();
    }

    protected HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes != null) {
            return servletRequestAttributes.getRequest();
        } else {
            return null;
        }
    }

}
