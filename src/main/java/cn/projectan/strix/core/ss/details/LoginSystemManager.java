package cn.projectan.strix.core.ss.details;

import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.dict.SystemManagerStatus;
import cn.projectan.strix.model.dict.SystemManagerType;
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
 * 系统管理员登录信息
 *
 * @author ProjectAn
 * @date 2023/2/25 0:05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSystemManager implements UserDetails {

    private SystemManager systemManager;
    private List<String> menusKeys;
    private List<String> permissionKeys;
    private List<String> regionIds;
    private byte regionPermissionType;

    @JsonIgnore
    private List<GrantedAuthority> authorities;

    public LoginSystemManager(SystemManager systemManager, byte regionPermissionType, List<String> menusKeys, List<String> permissionKeys, List<String> regionIds) {
        this.systemManager = systemManager;
        this.regionPermissionType = regionPermissionType;
        this.menusKeys = menusKeys;
        this.permissionKeys = permissionKeys;
        this.regionIds = regionIds;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.authorities == null) {
            // 添加权限
            this.authorities = this.permissionKeys.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
            // 添加管理员角色
            this.authorities.add(new SimpleGrantedAuthority("ROLE_SYSTEM_MANAGER"));
            // 添加超级管理员角色
            if (systemManager.getType() == SystemManagerType.SUPER_ACCOUNT) {
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
        return Objects.equals(systemManager.getStatus(), SystemManagerStatus.NORMAL);
    }

}
