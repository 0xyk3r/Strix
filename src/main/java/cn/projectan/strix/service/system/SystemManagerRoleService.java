package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemManagerRoleMapper;
import cn.projectan.strix.model.db.system.SystemManagerRole;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Strix 系统管理人员 角色关系 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemManagerRoleService extends ServiceImpl<SystemManagerRoleMapper, SystemManagerRole> {

    /**
     * 根据角色 ID 获取管理人员 ID 列表
     */
    public List<String> listManagerIdsByRoleId(String roleId) {
        return lambdaQuery()
                .eq(SystemManagerRole::getSystemRoleId, roleId)
                .list()
                .stream()
                .map(SystemManagerRole::getSystemManagerId)
                .collect(Collectors.toList());
    }

    /**
     * 删除管理人员与多个角色的关联关系
     */
    public boolean deleteByManagerIdAndRoleIds(String managerId, List<String> roleIds) {
        return lambdaUpdate()
                .eq(SystemManagerRole::getSystemManagerId, managerId)
                .in(SystemManagerRole::getSystemRoleId, roleIds)
                .remove();
    }

    /**
     * 删除管理人员的所有角色关联
     */
    public boolean deleteByManagerId(String managerId) {
        return lambdaUpdate()
                .eq(SystemManagerRole::getSystemManagerId, managerId)
                .remove();
    }
}
