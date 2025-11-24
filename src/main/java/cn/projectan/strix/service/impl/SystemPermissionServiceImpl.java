package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.cache.SystemMenuCache;
import cn.projectan.strix.core.cache.SystemPermissionCache;
import cn.projectan.strix.mapper.SystemPermissionMapper;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.model.db.SystemRolePermission;
import cn.projectan.strix.service.SystemPermissionService;
import cn.projectan.strix.service.SystemRolePermissionService;
import cn.projectan.strix.util.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.jsonwebtoken.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemPermissionServiceImpl extends ServiceImpl<SystemPermissionMapper, SystemPermission> implements SystemPermissionService {

    private final SystemRolePermissionService systemRolePermissionService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIds(Collection<String> idList) {
        if (idList.isEmpty()) {
            return;
        }

        List<SystemPermission> permissionList = baseMapper.selectByIds(idList);
        Assert.isTrue(permissionList.size() == idList.size(), "部分权限不存在，无法删除");

        List<String> permissionIdList = permissionList.stream()
                .map(SystemPermission::getId)
                .toList();

        int res = baseMapper.deleteByIds(idList);
        Assert.isTrue(res == idList.size(), "权限删除失败，请重试");

        // 删除角色和系统权限间关系
        systemRolePermissionService.lambdaUpdate()
                .in(SystemRolePermission::getSystemPermissionId, permissionIdList)
                .remove();

        // 更新缓存
        SpringUtil.getBean(SystemMenuCache.class).updateRamAndRedis();
        SpringUtil.getBean(SystemPermissionCache.class).updateRamAndRedis();
    }

}
