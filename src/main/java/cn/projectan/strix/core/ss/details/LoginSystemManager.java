package cn.projectan.strix.core.ss.details;

import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.db.system.SystemRegion;
import cn.projectan.strix.model.dict.system.SystemManagerStatus;
import cn.projectan.strix.model.dict.system.SystemManagerType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
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
 * @since 2023/2/25 0:05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSystemManager implements UserDetails {

    private SystemManager systemManager;
    private SystemRegion systemRegion;
    private List<String> menusKeys;
    private List<String> permissionKeys;
    private List<String> regionIds;
    private short regionPermissionType;

    @JsonIgnore
    private volatile List<GrantedAuthority> authorities;

    public LoginSystemManager(SystemManager systemManager,
                              SystemRegion systemRegion,
                              short regionPermissionType,
                              List<String> menusKeys,
                              List<String> permissionKeys,
                              List<String> regionIds) {
        this.systemManager = systemManager;
        this.systemRegion = systemRegion;
        this.regionPermissionType = regionPermissionType;
        this.menusKeys = menusKeys;
        this.permissionKeys = permissionKeys;
        this.regionIds = regionIds;
    }

    @Override
    @JsonIgnore
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> result = this.authorities;
        if (result == null) {
            synchronized (this) {
                result = this.authorities;
                if (result == null) {
                    // 添加权限
                    result = this.permissionKeys.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                    // 添加管理员角色
                    result.add(new SimpleGrantedAuthority("ROLE_SYSTEM_MANAGER"));
                    if (systemManager.getType() == SystemManagerType.SUPER_ACCOUNT) {
                        // 添加超级管理员角色
                        result.add(new SimpleGrantedAuthority("ROLE_SUPER_SYSTEM_MANAGER"));
                    }
                    this.authorities = result;
                }
            }
        }
        return result;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return systemManager.getLoginPassword();
    }

    @Override
    @JsonIgnore
    public @NonNull String getUsername() {
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
