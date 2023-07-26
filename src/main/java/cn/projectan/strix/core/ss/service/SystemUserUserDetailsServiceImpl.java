package cn.projectan.strix.core.ss.service;

import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.utils.I18nUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author 安炯奕
 * @date 2023/2/25 14:30
 */
@Service
@RequiredArgsConstructor
public class SystemUserUserDetailsServiceImpl implements UserDetailsService {

    private final SystemUserService systemUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SystemUser systemUser = systemUserService.getById(username);
        if (systemUser == null) {
            throw new UsernameNotFoundException(I18nUtil.getMessage("warn.login"));
        }
        return new LoginSystemUser(systemUser);
    }

}
