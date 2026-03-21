package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemRolePermissionMapper;
import cn.projectan.strix.model.db.system.SystemRolePermission;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRolePermissionService extends ServiceImpl<SystemRolePermissionMapper, SystemRolePermission> {

    /**
     * 根据角色 ID 获取关联的权限 ID 列表
     */
    public List<String> listPermissionIdsByRoleId(String roleId) {
        return lambdaQuery()
                .select(SystemRolePermission::getSystemPermissionId)
                .eq(SystemRolePermission::getSystemRoleId, roleId)
                .list()
                .stream()
                .map(SystemRolePermission::getSystemPermissionId)
                .collect(Collectors.toList());
    }

    /**
     * 删除角色与多个权限的关联关系
     */
    public boolean deleteByRoleIdAndPermissionIds(String roleId, List<String> permissionIds) {
        return lambdaUpdate()
                .eq(SystemRolePermission::getSystemRoleId, roleId)
                .in(SystemRolePermission::getSystemPermissionId, permissionIds)
                .remove();
    }

    /**
     * 删除角色与单个权限的关联关系
     */
    public boolean deleteByRoleIdAndPermissionId(String roleId, String permissionId) {
        return lambdaUpdate()
                .eq(SystemRolePermission::getSystemRoleId, roleId)
                .eq(SystemRolePermission::getSystemPermissionId, permissionId)
                .remove();
    }
}
