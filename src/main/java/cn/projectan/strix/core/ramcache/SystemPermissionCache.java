package cn.projectan.strix.core.ramcache;

import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.service.SystemPermissionService;
import cn.projectan.strix.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2021/5/13 18:36
 */
@Slf4j
@Component
public class SystemPermissionCache {

    @Autowired
    private SystemPermissionService systemPermissionService;
    @Autowired
    private RedisUtil redisUtil;

    private List<SystemPermission> instance = new ArrayList<>();

    @PostConstruct
    private void init() {
        instance = systemPermissionService.list();
        log.info(String.format("Strix Cache: 管理系统权限缓存加载成功, 缓存了 %s 个权限.", instance.size()));
    }

    public List<SystemPermission> getByIds(String... systemPermissionIds) {
        List<String> systemPermissionIdList = Arrays.asList(systemPermissionIds);
        return instance.stream().filter(p -> systemPermissionIdList.contains(p.getId())).collect(Collectors.toList());
    }

    public void updateRam() {
        init();
    }

    public void updateRedis() {
        redisUtil.delLike("strix:system:role:permission_by_rid:*");
        redisUtil.delLike("strix:system:manager:permission_by_smid:*");
    }

    public void updateRedisBySystemRoleId(String roleId) {
        redisUtil.delLike("strix:system:role:permission_by_rid::" + roleId);
        redisUtil.delLike("strix:system:role:select_data:*");
        // TODO 可优化为仅清除拥有该角色的管理用户缓存
        redisUtil.delLike("strix:system:manager:permission_by_smid:*");
    }

    public void updateRedisBySystemManageId(String managerId) {
        redisUtil.delLike("strix:system:manager:permission_by_smid::" + managerId);
        redisUtil.delLike("strix:system:manager:is_super_manager_by_smid::" + managerId);
    }

    public void updateRamAndRedis() {
        updateRam();
        updateRedis();
    }

}
