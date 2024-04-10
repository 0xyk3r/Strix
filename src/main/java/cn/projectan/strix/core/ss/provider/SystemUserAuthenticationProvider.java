package cn.projectan.strix.core.ss.provider;

import cn.projectan.strix.core.ss.token.SystemUserAuthenticationToken;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * 系统用户认证提供者
 * <p>由于实现了自定义登录api，该类在项目中无实际作用</p>
 *
 * @author ProjectAn
 * @date 2023/2/25 14:28
 */
@Service
public class SystemUserAuthenticationProvider implements AuthenticationProvider {

    @Resource(name = "systemUserUserDetailsServiceImpl")
    private UserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UserDetails userDetails = userDetailsService.loadUserByUsername(authentication.getName());
        return new UsernamePasswordAuthenticationToken(userDetails, authentication.getCredentials().toString(), userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(SystemUserAuthenticationToken.class);
    }

}
