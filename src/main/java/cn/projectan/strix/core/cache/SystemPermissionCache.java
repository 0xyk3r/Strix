package cn.projectan.strix.core.cache;

import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.SystemPermission;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemPermissionService;
import cn.projectan.strix.utils.RedisUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
@RequiredArgsConstructor
public class SystemPermissionCache {

    private final SystemManagerService systemManagerService;
    private final SystemPermissionService systemPermissionService;
    private final RedisUtil redisUtil;

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
        redisUtil.delKeys("strix:system:role:permission_by_rid:*");
        redisUtil.delKeys("strix:system:manager:permission_by_smid:*");
    }

    public void updateRedisBySystemRoleId(String roleId) {
        redisUtil.delKeys("strix:system:role:permission_by_rid::" + roleId);
        redisUtil.delKeys("strix:system:role:select_data:*");
        // TODO 可优化为仅清除拥有该角色的管理用户缓存
        redisUtil.delKeys("strix:system:manager:permission_by_smid:*");
    }

    public void updateRedisBySystemManageId(String managerId) {
        redisUtil.delKeys("strix:system:manager:permission_by_smid::" + managerId);

        // TODO 暂不确定写在这里是否合适
        Object existToken = redisUtil.get("strix:system:manager:login_token:login:id_" + managerId);
        if (existToken != null) {
            // 刷新登录token信息
            LoginSystemManager loginSystemManager = systemManagerService.getLoginInfo(managerId);
            redisUtil.set("strix:system:manager:login_token:token:" + existToken, loginSystemManager);
        }
    }

    public void updateRamAndRedis() {
        updateRam();
        updateRedis();
    }

}
