package cn.projectan.strix.service;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-12
 */
public interface SystemManagerService extends IService<SystemManager> {

    /**
     * 获取管理用户的所有菜单权限
     */
    List<SystemMenu> getAllSystemMenuByManager(String systemManagerId);

    /**
     * 获取管理用户的所有系统权限
     */
    List<SystemPermission> getAllSystemPermissionByManager(String systemManagerId);

    /**
     * 登陆时获取用户完整权限信息
     *
     * @param systemManagerId 管理用户ID
     * @return LoginSystemManager
     */
    LoginSystemManager getLoginInfo(String systemManagerId);

    /**
     * 刷新 redis 中的用户权限信息
     */
    void refreshLoginInfo(List<String> systemManagerIdList);

}
