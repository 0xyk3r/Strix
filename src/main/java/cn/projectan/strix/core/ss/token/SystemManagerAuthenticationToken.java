package cn.projectan.strix.core.ss.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * @author ProjectAn
 * @date 2023/2/25 14:26
 */
public class SystemManagerAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public SystemManagerAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public SystemManagerAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }

}
