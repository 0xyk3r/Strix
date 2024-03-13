package cn.projectan.strix.core.ss.provider;

import cn.projectan.strix.core.ss.token.SystemManagerAuthenticationToken;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/2/25 14:21
 */
@Component
public class SystemManagerAuthenticationProvider implements AuthenticationProvider {

    @Resource(name = "systemManagerUserDetailsServiceImpl")
    private UserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UserDetails userDetails = userDetailsService.loadUserByUsername(authentication.getName());
        return new UsernamePasswordAuthenticationToken(userDetails, authentication.getCredentials().toString(), userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(SystemManagerAuthenticationToken.class);
    }

}
