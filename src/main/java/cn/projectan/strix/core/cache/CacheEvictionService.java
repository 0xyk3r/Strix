package cn.projectan.strix.core.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

/**
 * 集中式缓存清除服务
 * <p>
 * 所有缓存失效操作统一收口到此 Service, 由 CacheEvictionListener 调用.
 * 使用 Spring AOP @CacheEvict, 因此必须通过代理调用 (不可 this 调用).
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@Service
public class CacheEvictionService {

    // ======================== Role Caches ========================

    @CacheEvict(value = "select_data", allEntries = true)
    public void evictRoleSelectCache() {
        log.debug("缓存清除: select_data (all)");
    }

    @CacheEvict(value = "menu_by_rid", allEntries = true)
    public void evictAllRoleMenuCache() {
        log.debug("缓存清除: menu_by_rid (all)");
    }

    @CacheEvict(value = "menu_by_rid", key = "#roleId")
    public void evictRoleMenuCache(String roleId) {
        log.debug("缓存清除: menu_by_rid, roleId={}", roleId);
    }

    @CacheEvict(value = "permission_by_rid", allEntries = true)
    public void evictAllRolePermissionCache() {
        log.debug("缓存清除: permission_by_rid (all)");
    }

    @CacheEvict(value = "permission_by_rid", key = "#roleId")
    public void evictRolePermissionCache(String roleId) {
        log.debug("缓存清除: permission_by_rid, roleId={}", roleId);
    }

    // ======================== Manager Caches ========================

    @CacheEvict(value = "menu_by_mid", allEntries = true)
    public void evictAllManagerMenuCache() {
        log.debug("缓存清除: menu_by_mid (all)");
    }

    @CacheEvict(value = "permission_by_mid", allEntries = true)
    public void evictAllManagerPermissionCache() {
        log.debug("缓存清除: permission_by_mid (all)");
    }

    // ======================== Config Cache ========================

    @CacheEvict(value = "strix:config", key = "#configKey")
    public void evictConfigCache(String configKey) {
        log.debug("缓存清除: strix:config, key={}", configKey);
    }

    @CacheEvict(value = "strix:config", allEntries = true)
    public void evictAllConfigCache() {
        log.debug("缓存清除: strix:config (all)");
    }

    // ======================== Region Cache ========================

    @Caching(evict = {
            @CacheEvict(value = "strix:region_by_id", key = "#regionId"),
            @CacheEvict(value = "strix:region_children", key = "#regionId")
    })
    public void evictRegionCache(String regionId) {
        log.debug("缓存清除: region, regionId={}", regionId);
    }

    @Caching(evict = {
            @CacheEvict(value = "strix:region_by_id", allEntries = true),
            @CacheEvict(value = "strix:region_children", allEntries = true)
    })
    public void evictAllRegionCache() {
        log.debug("缓存清除: region (all)");
    }
}
