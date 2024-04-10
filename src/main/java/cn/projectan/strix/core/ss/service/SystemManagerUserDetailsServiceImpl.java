package cn.projectan.strix.core.ss.service;

import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemRegionService;
import cn.projectan.strix.utils.I18nUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 系统管理员用户详情服务
 * <p>由于实现了自定义登录api，该类在项目中无实际作用</p>
 *
 * @author ProjectAn
 * @date 2023/2/25 14:23
 */
@Service
@RequiredArgsConstructor
public class SystemManagerUserDetailsServiceImpl implements UserDetailsService {

    private final SystemManagerService systemManagerService;
    private final SystemRegionService systemRegionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryChainWrapper<SystemManager> query = systemManagerService.lambdaQuery().eq(SystemManager::getLoginName, username);
        SystemManager systemManager = query.one();

        if (systemManager == null) {
            throw new UsernameNotFoundException(I18nUtil.getMessage("warn.login"));
        }

        return systemManagerService.getLoginInfo(systemManager.getId());
    }

}
