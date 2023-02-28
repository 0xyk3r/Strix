package cn.projectan.strix.core.ss.service;

import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author 安炯奕
 * @date 2023/2/25 14:30
 */
@Service
public class SystemUserUserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SystemUserService systemUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SystemUser systemUser = systemUserService.getById(username);
        if (systemUser == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return new LoginSystemUser(systemUser);
    }

}
