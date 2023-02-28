package cn.projectan.strix.core.ss.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * @author 安炯奕
 * @date 2023/2/25 14:27
 */
public class SystemUserAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public SystemUserAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public SystemUserAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }

}
