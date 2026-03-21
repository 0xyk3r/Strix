package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemRoleMenuMapper;
import cn.projectan.strix.model.db.system.SystemRoleMenu;
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
 * @since 2021-06-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRoleMenuService extends ServiceImpl<SystemRoleMenuMapper, SystemRoleMenu> {

    /**
     * 根据角色 ID 获取关联的菜单 ID 列表
     */
    public List<String> listMenuIdsByRoleId(String roleId) {
        return lambdaQuery()
                .select(SystemRoleMenu::getSystemMenuId)
                .eq(SystemRoleMenu::getSystemRoleId, roleId)
                .list()
                .stream()
                .map(SystemRoleMenu::getSystemMenuId)
                .collect(Collectors.toList());
    }

    /**
     * 删除角色与指定菜单的关联关系
     */
    public boolean deleteByRoleIdAndMenuIds(String roleId, List<String> menuIds) {
        return lambdaUpdate()
                .eq(SystemRoleMenu::getSystemRoleId, roleId)
                .in(SystemRoleMenu::getSystemMenuId, menuIds)
                .remove();
    }
}
