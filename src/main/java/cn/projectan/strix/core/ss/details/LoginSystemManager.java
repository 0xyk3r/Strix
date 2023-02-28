package cn.projectan.strix.core.ss.details;

import cn.projectan.strix.model.constant.SystemManagerStatus;
import cn.projectan.strix.model.constant.SystemManagerType;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemPermission;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2023/2/25 0:05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSystemManager implements UserDetails {

    private SystemManager systemManager;

    private List<SystemPermission> permissions;

    private List<String> regionIds;

    public LoginSystemManager(SystemManager systemManager, List<SystemPermission> permissions, List<String> regionIds) {
        this.systemManager = systemManager;
        this.permissions = permissions;
        this.regionIds = regionIds;
    }

    @JsonIgnore
    private List<GrantedAuthority> authorities;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.authorities == null) {
            this.authorities = this.permissions.stream().map(p -> new SimpleGrantedAuthority(p.getPermissionKey())).collect(Collectors.toList());
            this.authorities.add(new SimpleGrantedAuthority("ROLE_SYSTEM_MANAGER"));
            if (systemManager.getManagerType() == SystemManagerType.SUPER_ACCOUNT) {
                this.authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_SYSTEM_MANAGER"));
            }
        }
        return this.authorities;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return systemManager.getLoginPassword();
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return systemManager.getNickname();
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return Objects.equals(systemManager.getManagerStatus(), SystemManagerStatus.NORMAL);
    }

}
