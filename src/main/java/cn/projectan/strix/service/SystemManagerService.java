package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.db.SystemRegion;
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
     * 获取管理用户的所有系统权限
     */
    List<SystemPermission> getAllSystemPermissionByManager(String systemManagerId);

    /**
     * 获取管理用户的所有菜单权限
     */
    List<SystemMenu> getAllSystemMenuByManager(String systemManagerId);

}
