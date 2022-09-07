package cn.projectan.strix.controller.system.base;

import cn.projectan.strix.model.constant.SystemManagerType;
import cn.projectan.strix.model.db.SystemManager;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/5/13 18:30
 */
public class BaseSystemController {

    protected SystemManager getLoginManager() {
        SystemManager systemManager = (SystemManager) getRequest().getAttribute("_LoginSystemManager");
        Assert.notNull(systemManager, "获取登录信息失败");
        return systemManager;
    }

    protected HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes != null) {
            return servletRequestAttributes.getRequest();
        } else {
            return null;
        }
    }

    protected String getLoginManagerId() {
        return getLoginManager().getId();
    }

    protected Integer getLoginManagerStatus() {
        return getLoginManager().getManagerStatus();
    }

    protected Integer getLoginManagerType() {
        return getLoginManager().getManagerType();
    }

    protected String getLoginManagerRegionId() {
        return getLoginManager().getRegionId();
    }

    protected boolean isLoggedInSuperManager() {
        return getLoginManager().getManagerType() == SystemManagerType.SUPER_ACCOUNT;
    }

    protected List<String> getLoginManagerRegionIdList() {
        List<String> loginSystemManagerRegionIdList = (List<String>) getRequest().getAttribute("_LoginSystemManagerRegionIdList");
        if (loginSystemManagerRegionIdList.size() == 0) {
            loginSystemManagerRegionIdList.add("NullData");
        }
        return loginSystemManagerRegionIdList;
    }

    protected List<String> getLoginManagerRegionIdListExcludeCurrent() {
        List<String> loginSystemManagerRegionIdList = (List<String>) getRequest().getAttribute("_LoginSystemManagerRegionIdList");
        if (loginSystemManagerRegionIdList.size() == 0) {
            loginSystemManagerRegionIdList.add("NullData");
        }
        loginSystemManagerRegionIdList.remove(getLoginManagerRegionId());
        return loginSystemManagerRegionIdList;
    }

}
