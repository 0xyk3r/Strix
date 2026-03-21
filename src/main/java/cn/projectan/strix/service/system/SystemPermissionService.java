package cn.projectan.strix.service.system;

import cn.projectan.strix.core.cache.system.SystemMenuCache;
import cn.projectan.strix.core.cache.system.SystemPermissionCache;
import cn.projectan.strix.mapper.system.SystemPermissionMapper;
import cn.projectan.strix.model.db.system.SystemPermission;
import cn.projectan.strix.model.db.system.SystemRolePermission;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Strix 系统权限 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemPermissionService extends ServiceImpl<SystemPermissionMapper, SystemPermission> {

    private final SystemRolePermissionService systemRolePermissionService;
    @Lazy
    private final SystemMenuCache systemMenuCache;
    @Lazy
    private final SystemPermissionCache systemPermissionCache;

    /**
     * 查询全部权限（按创建时间升序）
     */
    public List<SystemPermission> listAll() {
        return lambdaQuery()
                .orderByAsc(SystemPermission::getCreatedTime)
                .list();
    }

    /**
     * 查询全部权限（仅 id、name，用于穿梭框）
     */
    public List<SystemPermission> listForTransfer() {
        return lambdaQuery()
                .select(SystemPermission::getId, SystemPermission::getName)
                .list();
    }

    /**
     * 根据 ID 集合删除系统权限
     *
     * @param idList 系统权限 ID 集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(Collection<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
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
        systemMenuCache.updateRamAndRedis();
        systemPermissionCache.updateRamAndRedis();
    }

}
