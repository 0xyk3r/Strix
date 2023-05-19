package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.mapper.SystemManagerMapper;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemManagerRole;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.service.SystemManagerRoleService;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemRegionService;
import cn.projectan.strix.service.SystemRoleService;
import cn.projectan.strix.utils.SpringUtil;
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

    @Cacheable(value = "strix:system:manager:permission_by_smid", key = "#systemManagerId")
    @Override
    public List<SystemPermission> getAllSystemPermissionByManager(String systemManagerId) {
        QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
        systemManagerRoleQueryWrapper.select("system_manager_role_id");
        systemManagerRoleQueryWrapper.eq("system_manager_id", systemManagerId);
        List<String> systemManagerRoleIdList = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);
        return systemRoleService.getSystemPermissionByRoleId(new TreeSet<>(systemManagerRoleIdList));
    }

    @Cacheable(value = "strix:system:manager:menu_by_smid", key = "#systemManagerId")
    @Override
    public List<SystemMenu> getAllSystemMenuByManager(String systemManagerId) {
        QueryWrapper<SystemManagerRole> systemManagerRoleQueryWrapper = new QueryWrapper<>();
        systemManagerRoleQueryWrapper.select("system_manager_role_id");
        systemManagerRoleQueryWrapper.eq("system_manager_id", systemManagerId);
        List<String> systemManagerRoleIdList = systemManagerRoleService.listObjs(systemManagerRoleQueryWrapper, Object::toString);
        return systemRoleService.getMenusByRoleId(new TreeSet<>(systemManagerRoleIdList));
    }

    @Override
    public LoginSystemManager getLoginInfo(String systemManagerId) {
        SystemManagerServiceImpl proxy = SpringUtil.getAopProxy(this);

        SystemManager systemManager = proxy.getById(systemManagerId);

        List<SystemPermission> permissions = proxy.getAllSystemPermissionByManager(systemManager.getId());
        List<String> regionIds = null;
        if (StringUtils.hasText(systemManager.getRegionId())) {
            regionIds = systemRegionService.getChildrenIdList(systemManager.getRegionId());
        }
        return new LoginSystemManager(systemManager, permissions, regionIds);
    }

}
