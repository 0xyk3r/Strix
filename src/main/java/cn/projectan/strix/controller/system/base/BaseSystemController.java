package cn.projectan.strix.controller.system.base;

import cn.projectan.strix.controller.BaseController;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.utils.SecurityUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 系统管理端基础控制器
 *
 * @author ProjectAn
 * @date 2021/5/13 18:30
 */
public class BaseSystemController extends BaseController {

    private static final List<String> EMPTY_FILL_LIST = List.of("NullData");

    protected SystemManager loginManager() {
        return SecurityUtils.getSystemManager();
    }

    protected String loginManagerId() {
        return SecurityUtils.getManagerId();
    }

    protected String loginManagerRegionId() {
        return SecurityUtils.getManagerRegionId();
    }

    protected boolean isSuperManager() {
        return SecurityUtils.isSuperAdmin();
    }

    protected boolean notSuperManager() {
        return !SecurityUtils.isSuperAdmin();
    }

    protected List<String> loginManagerRegionIdList() {
        List<String> loginSystemManagerRegionIdList = SecurityUtils.getManagerRegionIdList();
        if (CollectionUtils.isEmpty(loginSystemManagerRegionIdList)) {
            return EMPTY_FILL_LIST;
        }
        return loginSystemManagerRegionIdList;
    }

    protected List<String> loginManagerRegionIdListExcludeCurrent() {
        List<String> loginSystemManagerRegionIdList = SecurityUtils.getManagerRegionIdList();
        if (CollectionUtils.isEmpty(loginSystemManagerRegionIdList)) {
            return EMPTY_FILL_LIST;
        }
        loginSystemManagerRegionIdList.remove(loginManagerRegionId());
        return loginSystemManagerRegionIdList;
    }

}
