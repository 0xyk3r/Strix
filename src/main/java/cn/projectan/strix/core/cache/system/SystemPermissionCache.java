package cn.projectan.strix.core.cache.system;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.system.SystemPermission;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.service.system.SystemPermissionService;
import cn.projectan.strix.service.system.TokenSessionService;
import cn.projectan.strix.util.common.RedisUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统权限缓存
 *
 * @author ProjectAn
 * @since 2021/5/13 18:36
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemPermissionCache {

    private final SystemPermissionService systemPermissionService;
    private final SystemManagerService systemManagerService;
    private final TokenSessionService tokenSessionService;
    private final RedisUtil redisUtil;

    private volatile List<SystemPermission> instance = new ArrayList<>();

    @PostConstruct
    private void init() {
        instance = systemPermissionService.list();
        log.info("Strix Cache: 管理系统权限缓存加载完成, 缓存了 {} 个权限.", instance.size());
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
        redisUtil.delLike("strix:system:manager:permission_by_mid:*");
    }

    public void updateRedisBySystemRoleId(String roleId) {
        redisUtil.delLike("strix:system:role:permission_by_rid::" + roleId);
        redisUtil.delLike("strix:system:role:select_data:*");
        redisUtil.delLike("strix:system:manager:permission_by_mid:*");
    }

    public void updateRedisBySystemManageId(String managerId) {
        redisUtil.delLike("strix:system:manager:permission_by_mid::" + managerId);

        LoginSystemManager loginSystemManager = systemManagerService.getLoginInfo(managerId);
        tokenSessionService.refreshManagerLoginInfo(managerId, loginSystemManager);
    }

    public void updateRamAndRedis() {
        updateRam();
        updateRedis();
    }

}
