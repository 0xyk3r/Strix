package cn.projectan.strix.core.ss.service;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemRegionService;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/2/25 14:23
 */
@Service
public class SystemManagerUserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SystemManagerService systemManagerService;

    @Autowired
    private SystemRegionService systemRegionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryChainWrapper<SystemManager> query = systemManagerService.lambdaQuery().eq(SystemManager::getLoginName, username);
        SystemManager systemManager = query.one();

        if (systemManager == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        List<SystemPermission> permissions = systemManagerService.getAllSystemPermissionByManager(systemManager.getId());
        List<String> regionIds = null;
        if (StringUtils.hasText(systemManager.getRegionId())) {
            regionIds = systemRegionService.getChildrenIdList(systemManager.getRegionId());
        }

        return new LoginSystemManager(systemManager, permissions, regionIds);
    }

}
