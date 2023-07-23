package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.mapper.SystemManagerMapper;
import cn.projectan.strix.model.constant.SystemManagerType;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemManagerRole;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.service.*;
import cn.projectan.strix.utils.RedisUtil;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.TreeSet;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-12
 */
@Service
public class SystemManagerServiceImpl extends ServiceImpl<SystemManagerMapper, SystemManager> implements SystemManagerService {

    @Autowired
    private SystemRoleService systemRoleService;
    @Autowired
    private SystemManagerRoleService systemManagerRoleService;
    @Autowired
    private SystemRegionService systemRegionService;
    @Autowired
    private SystemMenuService systemMenuService;
    @Autowired
    private SystemPermissionService systemPermissionService;
    @Autowired
    private RedisUtil redisUtil;

    @Cacheable(value = "strix:system:manager:menu_by_smid", key = "#systemManagerId")
    @Override
    public List<SystemMenu> getAllSystemMenuByManager(String systemManagerId) {
        QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
        systemManagerRoleQueryWrapper.select("system_role_id");
        systemManagerRoleQueryWrapper.eq("system_manager_id", systemManagerId);
        List<String> systemManagerRoleIdList = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);
        return systemRoleService.getMenusByRoleId(new TreeSet<>(systemManagerRoleIdList));
    }

    @Cacheable(value = "strix:system:manager:permission_by_smid", key = "#systemManagerId")
    @Override
    public List<SystemPermission> getAllSystemPermissionByManager(String systemManagerId) {
        QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
        systemManagerRoleQueryWrapper.select("system_role_id");
        systemManagerRoleQueryWrapper.eq("system_manager_id", systemManagerId);
        List<String> systemManagerRoleIdList = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);
        return systemRoleService.getSystemPermissionByRoleId(new TreeSet<>(systemManagerRoleIdList));
    }

    @Override
    public LoginSystemManager getLoginInfo(String systemManagerId) {
        SystemManagerServiceImpl proxy = SpringUtil.getAopProxy(this);

        SystemManager systemManager = proxy.getById(systemManagerId);

        List<SystemMenu> menus;
        List<SystemPermission> permissions;
        List<String> regionIds = null;
        if (systemManager.getType() == SystemManagerType.SUPER_ACCOUNT) {
            // 超级账号默认拥有所有权限
            menus = systemMenuService.list(new LambdaQueryWrapper<SystemMenu>().orderByAsc(SystemMenu::getSortValue));
            permissions = systemPermissionService.list();
        } else {
            // 普通账号
            menus = proxy.getAllSystemMenuByManager(systemManager.getId());
            permissions = proxy.getAllSystemPermissionByManager(systemManager.getId());
            if (StringUtils.hasText(systemManager.getRegionId())) {
                regionIds = systemRegionService.getChildrenIdList(systemManager.getRegionId());
            }
        }
        return new LoginSystemManager(systemManager, menus, permissions, regionIds);
    }

    @Override
    public void refreshLoginInfo(List<String> systemManagerIdList) {
        systemManagerIdList.forEach(managerId -> {
            Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + managerId);
            if (existToken != null) {
                LoginSystemManager loginSystemManager = this.getLoginInfo(managerId);
                redisUtil.set("strix:system:manager:login_token:token:" + existToken, loginSystemManager);
            }
        });
    }

}
