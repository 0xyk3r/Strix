package cn.projectan.strix.controller.system.base;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.constant.SystemManagerType;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/5/13 18:30
 */
public class BaseSystemController {

    private static final List<String> NO_REGION_PERMISSION = List.of("NullData");

    protected LoginSystemManager getLoginSystemManager() {
        return SecurityUtils.getLoginSystemManager();
    }

    protected SystemManager getSystemManager() {
        return SecurityUtils.getSystemManager();
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
        return getSystemManager().getId();
    }

    protected Integer getLoginManagerStatus() {
        return getSystemManager().getManagerStatus();
    }

    protected Integer getLoginManagerType() {
        return getSystemManager().getManagerType();
    }

    protected String getLoginManagerRegionId() {
        return getSystemManager().getRegionId();
    }

    protected boolean isSuperManager() {
        return getSystemManager().getManagerType() == SystemManagerType.SUPER_ACCOUNT;
    }

    protected List<String> getLoginManagerRegionIdList() {
        List<String> loginSystemManagerRegionIdList = getLoginSystemManager().getRegionIds();
        if (CollectionUtils.isEmpty(loginSystemManagerRegionIdList)) {
            return NO_REGION_PERMISSION;
        }
        return loginSystemManagerRegionIdList;
    }

    protected List<String> getLoginManagerRegionIdListExcludeCurrent() {
        List<String> loginSystemManagerRegionIdList = getLoginSystemManager().getRegionIds();
        if (CollectionUtils.isEmpty(loginSystemManagerRegionIdList)) {
            return NO_REGION_PERMISSION;
        }
        loginSystemManagerRegionIdList.remove(getLoginManagerRegionId());
        return loginSystemManagerRegionIdList;
    }

}
