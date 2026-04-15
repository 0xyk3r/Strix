package cn.projectan.strix.core.cache;

import cn.projectan.strix.core.sse.SseSessionManager;
import cn.projectan.strix.model.event.cache.*;
import cn.projectan.strix.service.system.SystemManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 缓存失效事件监听器
 * <p>
 * 监听所有 CacheInvalidationEvent 子类, 调用 CacheEvictionService 清除缓存,
 * 并级联刷新 LoginInfo. 同时通过 CacheInvalidationBroadcaster 广播到其他实例.
 * <p>
 * 权限相关变更完成后, 通过 SSE 广播 auth:refresh 事件通知前端刷新权限.
 * <p>
 * 使用 @TransactionalEventListener(AFTER_COMMIT) 确保数据已提交后再清除缓存.
 * fallbackExecution=true 确保非事务上下文 (如直接 save/update) 也能触发.
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionListener {

    private final CacheEvictionService cacheEvictionService;
    private final SystemManagerService systemManagerService;
    private final CacheInvalidationBroadcaster broadcaster;
    private final SseSessionManager sseSessionManager;

    /**
     * 菜单变更 → 清除所有角色菜单缓存 + 所有管理员菜单缓存 + 刷新所有在线管理员 LoginInfo
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMenuChanged(MenuChangedEvent event) {
        log.info("处理菜单变更事件, remote={}", event.isRemote());
        cacheEvictionService.evictAllRoleMenuCache();
        cacheEvictionService.evictAllManagerMenuCache();
        systemManagerService.refreshLoginInfoForAllOnlineManagers();
        broadcastAuthRefresh("menu_changed");
        broadcastIfLocal(event);
    }

    /**
     * 权限变更 → 清除所有角色权限缓存 + 所有管理员权限缓存 + 刷新所有在线管理员 LoginInfo
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPermissionChanged(PermissionChangedEvent event) {
        log.info("处理权限变更事件, remote={}", event.isRemote());
        cacheEvictionService.evictAllRolePermissionCache();
        cacheEvictionService.evictAllManagerPermissionCache();
        systemManagerService.refreshLoginInfoForAllOnlineManagers();
        broadcastAuthRefresh("permission_changed");
        broadcastIfLocal(event);
    }

    /**
     * 角色基本信息变更 → 清除角色选择列表缓存 + 刷新该角色下管理员的 LoginInfo
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRoleChanged(RoleChangedEvent event) {
        log.info("处理角色变更事件, roleId={}, remote={}", event.getRoleId(), event.isRemote());
        cacheEvictionService.evictRoleSelectCache();
        systemManagerService.refreshLoginInfoByRole(event.getRoleId());
        broadcastAuthRefresh("role_changed");
        broadcastIfLocal(event);
    }

    /**
     * 角色-菜单关联变更 → 清除该角色的菜单缓存 + 级联刷新 LoginInfo
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRoleMenuChanged(RoleMenuChangedEvent event) {
        log.info("处理角色菜单变更事件, roleId={}, remote={}", event.getRoleId(), event.isRemote());
        cacheEvictionService.evictRoleMenuCache(event.getRoleId());
        cacheEvictionService.evictAllManagerMenuCache();
        systemManagerService.refreshLoginInfoByRole(event.getRoleId());
        broadcastAuthRefresh("role_menu_changed");
        broadcastIfLocal(event);
    }

    /**
     * 角色-权限关联变更 → 清除该角色的权限缓存 + 级联刷新 LoginInfo
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRolePermissionChanged(RolePermissionChangedEvent event) {
        log.info("处理角色权限变更事件, roleId={}, remote={}", event.getRoleId(), event.isRemote());
        cacheEvictionService.evictRolePermissionCache(event.getRoleId());
        cacheEvictionService.evictAllManagerPermissionCache();
        systemManagerService.refreshLoginInfoByRole(event.getRoleId());
        broadcastAuthRefresh("role_permission_changed");
        broadcastIfLocal(event);
    }

    /**
     * 管理员权限变更 (角色分配变化) → 刷新该管理员的 LoginInfo
     * (SystemManagerService.refreshLoginInfoByManager 自带 @CacheEvict for menu_by_mid + permission_by_mid)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onManagerPermissionChanged(ManagerPermissionChangedEvent event) {
        log.info("处理管理员权限变更事件, managerId={}, remote={}", event.getManagerId(), event.isRemote());
        systemManagerService.refreshLoginInfoByManager(event.getManagerId());
        broadcastAuthRefresh("manager_permission_changed");
        broadcastIfLocal(event);
    }

    /**
     * 系统配置变更 → 清除特定 config key 的缓存
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onConfigChanged(ConfigChangedEvent event) {
        log.info("处理配置变更事件, configKey={}, remote={}", event.getConfigKey(), event.isRemote());
        cacheEvictionService.evictConfigCache(event.getConfigKey());
        broadcastIfLocal(event);
    }

    /**
     * 地区变更 → 清除指定地区的缓存
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRegionChanged(RegionChangedEvent event) {
        log.info("处理地区变更事件, regionIds={}, remote={}", event.getRegionIds(), event.isRemote());
        if (event.getRegionIds() == null || event.getRegionIds().isEmpty()) {
            cacheEvictionService.evictAllRegionCache();
        } else {
            for (String regionId : event.getRegionIds()) {
                cacheEvictionService.evictRegionCache(regionId);
            }
        }
        broadcastIfLocal(event);
    }

    /**
     * 通过 SSE 广播 auth:refresh 事件, 通知所有已连接的前端刷新权限
     */
    private void broadcastAuthRefresh(String reason) {
        sseSessionManager.broadcast("auth:refresh", Map.of("reason", reason));
        log.debug("SSE 广播 auth:refresh, reason={}", reason);
    }

    private void broadcastIfLocal(CacheInvalidationEvent event) {
        if (!event.isRemote()) {
            broadcaster.broadcast(event);
        }
    }
}
