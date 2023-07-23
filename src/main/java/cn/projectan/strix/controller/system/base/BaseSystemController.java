package cn.projectan.strix.controller.system.base;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.utils.SecurityUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/5/13 18:30
 */
public class BaseSystemController {

    private static final List<String> EMPTY_FILL_LIST = List.of("NullData");

    protected LoginSystemManager getLoginSystemManager() {
        return SecurityUtils.getLoginSystemManager();
    }

    protected SystemManager getSystemManager() {
        return SecurityUtils.getSystemManager();
    }

    protected String getLoginManagerId() {
        return getSystemManager().getId();
    }

    protected String getLoginManagerRegionId() {
        return getSystemManager().getRegionId();
    }

    protected boolean isSuperManager() {
        return SecurityUtils.isSuperAdmin();
    }

    protected List<String> getLoginManagerRegionIdList() {
        List<String> loginSystemManagerRegionIdList = getLoginSystemManager().getRegionIds();
        if (CollectionUtils.isEmpty(loginSystemManagerRegionIdList)) {
            return EMPTY_FILL_LIST;
        }
        return loginSystemManagerRegionIdList;
    }

    protected List<String> getLoginManagerRegionIdListExcludeCurrent() {
        List<String> loginSystemManagerRegionIdList = getLoginSystemManager().getRegionIds();
        if (CollectionUtils.isEmpty(loginSystemManagerRegionIdList)) {
            return EMPTY_FILL_LIST;
        }
        loginSystemManagerRegionIdList.remove(getLoginManagerRegionId());
        return loginSystemManagerRegionIdList;
    }

}
