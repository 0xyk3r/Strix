# M5: Cache Strategy Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the inconsistent dual-layer caching (volatile RAM + Spring @Cacheable/Redis) with a unified event-driven cache invalidation architecture, add Redis Pub/Sub for multi-instance support, differentiated TTL, and a new SystemConfig CRUD management page.

**Architecture:** Services publish sealed `CacheInvalidationEvent` subclasses after mutations. A `CacheEvictionListener` handles all eviction via `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)`, calling centralized `CacheEvictionService` `@CacheEvict` methods. A `CacheInvalidationBroadcaster` publishes events to Redis Pub/Sub channel `strix:cache:invalidation`; a `CacheInvalidationSubscriber` on other instances re-publishes as local Spring events (with `remote=true` to prevent re-broadcast). Four legacy RAM cache classes (`SystemMenuCache`, `SystemPermissionCache`, `SystemConfigCache`, `SystemRegionCache`) are deleted or refactored. LoginInfo refresh cascades are preserved but triggered by events instead of manual controller calls.

**Tech Stack:** Java 21 / Spring Boot 4.0.2 / MyBatis Plus 3.5.16 / Redis Pub/Sub / Spring @Cacheable + @CacheEvict (backend), Vue 3.5 / TypeScript / Vite 8 / Naive UI / useCrud (frontend)

**Design Spec:** `docs/superpowers/specs/2026-04-16-m5-cache-strategy-redesign.md`

## File Structure

**Backend — New files (7):**

| File | Purpose |
|------|---------|
| `model/event/cache/CacheInvalidationEvent.java` | Abstract sealed base event with instanceId + remote flag |
| `model/event/cache/MenuChangedEvent.java` | Published when menus are added/updated/deleted |
| `model/event/cache/PermissionChangedEvent.java` | Published when permissions are added/updated/deleted |
| `model/event/cache/RoleChangedEvent.java` | Published when role basic info changes (name/status/sort) |
| `model/event/cache/RoleMenuChangedEvent.java` | Published when role-menu associations change |
| `model/event/cache/RolePermissionChangedEvent.java` | Published when role-permission associations change |
| `model/event/cache/ManagerPermissionChangedEvent.java` | Published when manager's role assignment changes |
| `model/event/cache/ConfigChangedEvent.java` | Published when a system config key is changed |
| `model/event/cache/RegionChangedEvent.java` | Published when regions are modified |
| `core/cache/CacheEvictionService.java` | Centralized @CacheEvict methods for all cache names |
| `core/cache/CacheEvictionListener.java` | @TransactionalEventListener handlers with cascade logic |
| `core/cache/CacheInvalidationBroadcaster.java` | Publishes events to Redis Pub/Sub |
| `core/cache/CacheInvalidationSubscriber.java` | Receives Redis Pub/Sub and re-publishes as Spring events |
| `core/cache/CacheInvalidationMessage.java` | Serializable DTO for Pub/Sub channel messages |
| `model/request/system/config/SystemConfigUpdateReq.java` | Config create/update request DTO |
| `model/response/system/config/SystemConfigListResp.java` | Config list response DTO |
| `controller/system/SystemConfigController.java` | Config CRUD controller |

**Backend — Modified files (10):**

| File | Change |
|------|--------|
| `config/RedisConfig.java` | Add differentiated TTL map + RedisMessageListenerContainer bean |
| `service/system/SystemMenuService.java` | Remove cache class deps, publish MenuChangedEvent, add `getMenuAndChildrenIds()` |
| `service/system/SystemPermissionService.java` | Remove cache class deps, publish PermissionChangedEvent |
| `service/system/SystemRoleService.java` | Publish RoleChangedEvent/RoleMenuChangedEvent/RolePermissionChangedEvent in mutations |
| `service/system/SystemManagerService.java` | Add `refreshLoginInfoForAllOnlineManagers()`, remove `SystemMenuCache`/`SystemPermissionCache` if present |
| `service/system/SystemConfigService.java` | Expand with full CRUD + @Cacheable per-key + event publishing |
| `service/system/SystemRegionService.java` | Remove SystemRegionCache dep, publish RegionChangedEvent |
| `controller/system/SystemMenuController.java` | Remove SystemMenuCache dep + manual cache/refresh calls |
| `controller/system/SystemPermissionController.java` | Remove SystemPermissionCache dep + manual cache/refresh calls |
| `controller/system/SystemRoleController.java` | Remove cache class deps, replace `systemMenuCache.getIdListByParentMenu()` with service call, remove manual refreshLoginInfo |
| `controller/system/SystemRegionController.java` | Remove SystemRegionCache dep + manual cache calls |
| `controller/system/SystemManagerController.java` | Replace manual refreshLoginInfo with ManagerPermissionChangedEvent |
| `core/cache/system/SystemConfigCache.java` | Replace ConcurrentHashMap with @Cacheable, keep get/getBoolean/getLong API |
| `aot/LambdaRegistrationFeature.java` | Remove SystemMenuCache import (class will be deleted) |

**Backend — Deleted files (3):**

| File | Reason |
|------|--------|
| `core/cache/system/SystemMenuCache.java` | Replaced by event-driven eviction + SystemMenuService.getMenuAndChildrenIds() |
| `core/cache/system/SystemPermissionCache.java` | Replaced by event-driven eviction (getByIds() was dead code) |
| `core/cache/system/SystemRegionCache.java` | Replaced by event-driven eviction |

**Frontend — New files (2):**

| File | Purpose |
|------|---------|
| `api/system-config.ts` | SystemConfig CRUD API module |
| `views/System/SystemConfig/SystemConfigIndex.vue` | Config management page with useCrud |

**Frontend — Modified files (1):**

| File | Change |
|------|--------|
| `router/index.ts` | Add SystemConfig route after dict |

**SQL:**
- `sys_system_menu` + `sys_system_role_menu` entries for 系统配置管理

## Key Conventions

- **ObjectMapper import:** `tools.jackson.databind.ObjectMapper` (Spring Boot 4 / Jackson 3.x)
- **All controllers** must extend `BaseController` or `BaseSystemController`
- **Cache name format:** `strix:cache_name` (colon-separated, matching existing names)
- **Event publishing:** Use `ApplicationEventPublisher.publishEvent()` — inject via `@RequiredArgsConstructor`
- **@TransactionalEventListener:** `phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true`
- **HTTP export:** `import { http } from '@/plugins/axios'`
- **Axios baseURL:** `/api/` — all API paths relative
- **i18n messages:** `{validation.required:field.xxx.yyy}`

---

### Task 1: Event Hierarchy — CacheInvalidationEvent + 8 Concrete Events + Message DTO

**Files:**
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/cache/CacheInvalidationEvent.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/cache/MenuChangedEvent.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/cache/PermissionChangedEvent.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/cache/RoleChangedEvent.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/cache/RoleMenuChangedEvent.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/cache/RolePermissionChangedEvent.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/cache/ManagerPermissionChangedEvent.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/cache/ConfigChangedEvent.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/cache/RegionChangedEvent.java`
- Create: `Strix/src/main/java/cn/projectan/strix/core/cache/CacheInvalidationMessage.java`

- [ ] **Step 1: Create `model/event/cache/` package and abstract sealed base event**

```java
package cn.projectan.strix.model.event.cache;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * 缓存失效事件基类
 * <p>
 * sealed class — 所有缓存失效事件必须在此列举
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public abstract sealed class CacheInvalidationEvent extends ApplicationEvent
        permits MenuChangedEvent, PermissionChangedEvent, RoleChangedEvent,
                RoleMenuChangedEvent, RolePermissionChangedEvent,
                ManagerPermissionChangedEvent, ConfigChangedEvent, RegionChangedEvent {

    /** 发布此事件的实例 ID, 用于 Pub/Sub 防回环 */
    private final String instanceId;

    /** 是否来自远程实例 (Redis Pub/Sub), 为 true 时不再广播 */
    private final boolean remote;

    protected CacheInvalidationEvent(Object source, String instanceId, boolean remote) {
        super(source);
        this.instanceId = instanceId;
        this.remote = remote;
    }

    protected CacheInvalidationEvent(Object source, String instanceId) {
        this(source, instanceId, false);
    }

    /** 返回事件类型名称, 用于 Pub/Sub 序列化 */
    public abstract String getEventType();
}
```

- [ ] **Step 2: Create MenuChangedEvent**

```java
package cn.projectan.strix.model.event.cache;

/**
 * 菜单变更事件 — 菜单增/改/删时发布
 * <p>
 * 影响范围: 所有角色的菜单缓存 + 所有管理员的菜单缓存 + 所有在线管理员的 LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
public final class MenuChangedEvent extends CacheInvalidationEvent {

    public MenuChangedEvent(Object source, String instanceId) {
        super(source, instanceId);
    }

    public MenuChangedEvent(Object source, String instanceId, boolean remote) {
        super(source, instanceId, remote);
    }

    @Override
    public String getEventType() {
        return "MENU_CHANGED";
    }
}
```

- [ ] **Step 3: Create PermissionChangedEvent**

```java
package cn.projectan.strix.model.event.cache;

/**
 * 权限变更事件 — 权限增/改/删时发布
 * <p>
 * 影响范围: 所有角色的权限缓存 + 所有管理员的权限缓存 + 所有在线管理员的 LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
public final class PermissionChangedEvent extends CacheInvalidationEvent {

    public PermissionChangedEvent(Object source, String instanceId) {
        super(source, instanceId);
    }

    public PermissionChangedEvent(Object source, String instanceId, boolean remote) {
        super(source, instanceId, remote);
    }

    @Override
    public String getEventType() {
        return "PERMISSION_CHANGED";
    }
}
```

- [ ] **Step 4: Create RoleChangedEvent**

```java
package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 角色基本信息变更事件 — 角色名称/状态/排序等修改或删除时发布
 * <p>
 * 影响范围: role select_data 缓存 + 该角色下管理员的 LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class RoleChangedEvent extends CacheInvalidationEvent {

    private final String roleId;

    public RoleChangedEvent(Object source, String instanceId, String roleId) {
        super(source, instanceId);
        this.roleId = roleId;
    }

    public RoleChangedEvent(Object source, String instanceId, boolean remote, String roleId) {
        super(source, instanceId, remote);
        this.roleId = roleId;
    }

    @Override
    public String getEventType() {
        return "ROLE_CHANGED";
    }
}
```

- [ ] **Step 5: Create RoleMenuChangedEvent**

```java
package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 角色-菜单关联变更事件 — 修改角色的菜单分配时发布
 * <p>
 * 影响范围: 该角色的菜单缓存 + 该角色下管理员的菜单缓存 + LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class RoleMenuChangedEvent extends CacheInvalidationEvent {

    private final String roleId;

    public RoleMenuChangedEvent(Object source, String instanceId, String roleId) {
        super(source, instanceId);
        this.roleId = roleId;
    }

    public RoleMenuChangedEvent(Object source, String instanceId, boolean remote, String roleId) {
        super(source, instanceId, remote);
        this.roleId = roleId;
    }

    @Override
    public String getEventType() {
        return "ROLE_MENU_CHANGED";
    }
}
```

- [ ] **Step 6: Create RolePermissionChangedEvent**

```java
package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 角色-权限关联变更事件 — 修改角色的权限分配时发布
 * <p>
 * 影响范围: 该角色的权限缓存 + 该角色下管理员的权限缓存 + LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class RolePermissionChangedEvent extends CacheInvalidationEvent {

    private final String roleId;

    public RolePermissionChangedEvent(Object source, String instanceId, String roleId) {
        super(source, instanceId);
        this.roleId = roleId;
    }

    public RolePermissionChangedEvent(Object source, String instanceId, boolean remote, String roleId) {
        super(source, instanceId, remote);
        this.roleId = roleId;
    }

    @Override
    public String getEventType() {
        return "ROLE_PERMISSION_CHANGED";
    }
}
```

- [ ] **Step 7: Create ManagerPermissionChangedEvent**

```java
package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 管理员权限变更事件 — 修改管理员的角色分配时发布
 * <p>
 * 影响范围: 该管理员的菜单/权限缓存 + LoginInfo
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class ManagerPermissionChangedEvent extends CacheInvalidationEvent {

    private final String managerId;

    public ManagerPermissionChangedEvent(Object source, String instanceId, String managerId) {
        super(source, instanceId);
        this.managerId = managerId;
    }

    public ManagerPermissionChangedEvent(Object source, String instanceId, boolean remote, String managerId) {
        super(source, instanceId, remote);
        this.managerId = managerId;
    }

    @Override
    public String getEventType() {
        return "MANAGER_PERMISSION_CHANGED";
    }
}
```

- [ ] **Step 8: Create ConfigChangedEvent**

```java
package cn.projectan.strix.model.event.cache;

import lombok.Getter;

/**
 * 系统配置变更事件 — 配置值修改/新增/删除时发布
 * <p>
 * 影响范围: 特定 config key 的缓存
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class ConfigChangedEvent extends CacheInvalidationEvent {

    private final String configKey;

    public ConfigChangedEvent(Object source, String instanceId, String configKey) {
        super(source, instanceId);
        this.configKey = configKey;
    }

    public ConfigChangedEvent(Object source, String instanceId, boolean remote, String configKey) {
        super(source, instanceId, remote);
        this.configKey = configKey;
    }

    @Override
    public String getEventType() {
        return "CONFIG_CHANGED";
    }
}
```

- [ ] **Step 9: Create RegionChangedEvent**

```java
package cn.projectan.strix.model.event.cache;

import lombok.Getter;

import java.util.List;

/**
 * 地区变更事件 — 地区增/改/删时发布
 * <p>
 * 影响范围: 指定地区 ID 的缓存
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Getter
public final class RegionChangedEvent extends CacheInvalidationEvent {

    private final List<String> regionIds;

    public RegionChangedEvent(Object source, String instanceId, List<String> regionIds) {
        super(source, instanceId);
        this.regionIds = regionIds;
    }

    public RegionChangedEvent(Object source, String instanceId, boolean remote, List<String> regionIds) {
        super(source, instanceId, remote);
        this.regionIds = regionIds;
    }

    @Override
    public String getEventType() {
        return "REGION_CHANGED";
    }
}
```

- [ ] **Step 10: Create CacheInvalidationMessage (Pub/Sub DTO)**

```java
package cn.projectan.strix.core.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Redis Pub/Sub 缓存失效消息
 * <p>
 * 序列化为 JSON 通过 Redis channel 传递, 接收端根据 eventType 重建对应的 Spring Event
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationMessage {

    /** 事件类型 (对应 CacheInvalidationEvent.getEventType()) */
    private String eventType;

    /** 发布实例 ID (防回环) */
    private String instanceId;

    /** 事件携带的业务数据 (roleId, managerId, configKey, regionIds 等) */
    private Map<String, Object> payload;
}
```

- [ ] **Step 11: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL — these are standalone POJOs/events with no dependencies on modified files.

- [ ] **Step 12: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(cache): add sealed CacheInvalidationEvent hierarchy + 8 event types

- Abstract sealed base: instanceId + remote flag for Pub/Sub
- MenuChangedEvent, PermissionChangedEvent: global scope
- RoleChangedEvent, RoleMenuChangedEvent, RolePermissionChangedEvent: per-role
- ManagerPermissionChangedEvent: per-manager
- ConfigChangedEvent: per-config-key
- RegionChangedEvent: per-region-ids
- CacheInvalidationMessage: Pub/Sub serialization DTO

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 2: CacheEvictionService — Centralized @CacheEvict Methods

**Files:**
- Create: `Strix/src/main/java/cn/projectan/strix/core/cache/CacheEvictionService.java`

- [ ] **Step 1: Create CacheEvictionService**

This service centralizes ALL `@CacheEvict` operations. The `CacheEvictionListener` calls these methods — no other code should call `@CacheEvict` for these cache names (except `SystemManagerService.refreshLoginInfoByManager()` which retains its own `@CacheEvict` for `menu_by_mid` and `permission_by_mid`).

Cache name reference (from existing `@Cacheable` annotations):
- `select_data` — SystemRoleService line 62
- `menu_by_rid` — SystemRoleService lines 74, 87
- `permission_by_rid` — SystemRoleService lines 114, 127
- `menu_by_mid` — SystemManagerService line 143
- `permission_by_mid` — SystemManagerService line 159
- `strix:system:config` — new, for SystemConfigCache refactoring
- `strix:region_by_id` — SystemRegionService line 50
- `strix:region_children` — SystemRegionService line 91

```java
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

    @CacheEvict(value = "strix:system:config", key = "#configKey")
    public void evictConfigCache(String configKey) {
        log.debug("缓存清除: strix:system:config, key={}", configKey);
    }

    @CacheEvict(value = "strix:system:config", allEntries = true)
    public void evictAllConfigCache() {
        log.debug("缓存清除: strix:system:config (all)");
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
```

- [ ] **Step 2: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(cache): add CacheEvictionService with centralized @CacheEvict methods

- Role: evictRoleSelectCache, evictAllRoleMenuCache, evictRoleMenuCache(id),
        evictAllRolePermissionCache, evictRolePermissionCache(id)
- Manager: evictAllManagerMenuCache, evictAllManagerPermissionCache
- Config: evictConfigCache(key), evictAllConfigCache
- Region: evictRegionCache(id), evictAllRegionCache

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 3: CacheEvictionListener — Event Handlers with Cascade Logic

**Files:**
- Create: `Strix/src/main/java/cn/projectan/strix/core/cache/CacheEvictionListener.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/service/system/SystemManagerService.java` — add `refreshLoginInfoForAllOnlineManagers()`

- [ ] **Step 1: Add `refreshLoginInfoForAllOnlineManagers()` to SystemManagerService**

In `Strix/src/main/java/cn/projectan/strix/service/system/SystemManagerService.java`, add this method after the existing `refreshLoginInfoForManagers(List<String> managerIdList)` method (after line ~331):

```java
    /**
     * 刷新所有在线管理员的 LoginInfo
     * (用于菜单/权限全局变更时)
     */
    public void refreshLoginInfoForAllOnlineManagers() {
        Set<String> onlineManagerIds = tokenSessionService.getOnlineManagerIds();
        if (!onlineManagerIds.isEmpty()) {
            log.info("刷新所有在线管理员 LoginInfo, 在线人数: {}", onlineManagerIds.size());
            refreshLoginInfoForManagers(new ArrayList<>(onlineManagerIds));
        }
    }
```

Also add this import at the top if not already present:

```java
import java.util.ArrayList;
import java.util.Set;
```

- [ ] **Step 2: Create CacheEvictionListener**

```java
package cn.projectan.strix.core.cache;

import cn.projectan.strix.model.event.cache.*;
import cn.projectan.strix.service.system.SystemManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 缓存失效事件监听器
 * <p>
 * 监听所有 CacheInvalidationEvent 子类, 调用 CacheEvictionService 清除缓存,
 * 并级联刷新 LoginInfo. 同时通过 CacheInvalidationBroadcaster 广播到其他实例.
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

    /**
     * 菜单变更 → 清除所有角色菜单缓存 + 所有管理员菜单缓存 + 刷新所有在线管理员 LoginInfo
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMenuChanged(MenuChangedEvent event) {
        log.info("处理菜单变更事件, remote={}", event.isRemote());
        cacheEvictionService.evictAllRoleMenuCache();
        cacheEvictionService.evictAllManagerMenuCache();
        systemManagerService.refreshLoginInfoForAllOnlineManagers();
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

    private void broadcastIfLocal(CacheInvalidationEvent event) {
        if (!event.isRemote()) {
            broadcaster.broadcast(event);
        }
    }
}
```

- [ ] **Step 3: Verify backend compiles**

Note: This will fail because `CacheInvalidationBroadcaster` doesn't exist yet. That's expected — we'll create it in the next task. For now, verify the event hierarchy and CacheEvictionService compile correctly by temporarily commenting out the `broadcaster` field and all `broadcastIfLocal()` calls, or proceed directly to Task 4 and build them together.

Alternative: Skip compilation check here and compile after Task 4.

- [ ] **Step 4: Commit (defer to after Task 4 if combining)**

```bash
cd Strix && git add -A && git commit -m "feat(cache): add CacheEvictionListener with event handlers + cascade logic

- @TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)
- MenuChanged/PermissionChanged: evict all + refresh all online managers
- RoleChanged: evict select_data + refresh role's managers
- RoleMenu/RolePermissionChanged: evict per-role + cascade LoginInfo
- ManagerPermissionChanged: refresh single manager LoginInfo
- ConfigChanged: evict per-key
- RegionChanged: evict per-region or all
- SystemManagerService.refreshLoginInfoForAllOnlineManagers() added

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 4: Redis Pub/Sub + TTL Configuration

**Files:**
- Create: `Strix/src/main/java/cn/projectan/strix/core/cache/CacheInvalidationBroadcaster.java`
- Create: `Strix/src/main/java/cn/projectan/strix/core/cache/CacheInvalidationSubscriber.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/config/RedisConfig.java`

- [ ] **Step 1: Create CacheInvalidationBroadcaster**

```java
package cn.projectan.strix.core.cache;

import cn.projectan.strix.model.event.cache.*;
import cn.projectan.strix.util.common.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存失效广播器
 * <p>
 * 将本地 CacheInvalidationEvent 序列化为 JSON 并发布到 Redis Pub/Sub channel,
 * 使其他实例也能收到缓存失效通知.
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationBroadcaster {

    public static final String CHANNEL = "strix:cache:invalidation";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 广播缓存失效事件到 Redis Pub/Sub
     *
     * @param event 本地事件 (remote=false 时才应调用)
     */
    public void broadcast(CacheInvalidationEvent event) {
        try {
            CacheInvalidationMessage message = new CacheInvalidationMessage();
            message.setEventType(event.getEventType());
            message.setInstanceId(event.getInstanceId());
            message.setPayload(buildPayload(event));

            String json = ObjectMapperUtil.toJson(message);
            stringRedisTemplate.convertAndSend(CHANNEL, json);
            log.debug("缓存失效广播已发送: type={}, instanceId={}", event.getEventType(), event.getInstanceId());
        } catch (Exception e) {
            log.error("缓存失效广播失败: type={}", event.getEventType(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildPayload(CacheInvalidationEvent event) {
        Map<String, Object> payload = new HashMap<>();
        switch (event) {
            case RoleChangedEvent e -> payload.put("roleId", e.getRoleId());
            case RoleMenuChangedEvent e -> payload.put("roleId", e.getRoleId());
            case RolePermissionChangedEvent e -> payload.put("roleId", e.getRoleId());
            case ManagerPermissionChangedEvent e -> payload.put("managerId", e.getManagerId());
            case ConfigChangedEvent e -> payload.put("configKey", e.getConfigKey());
            case RegionChangedEvent e -> payload.put("regionIds", e.getRegionIds());
            case MenuChangedEvent ignored -> {}
            case PermissionChangedEvent ignored -> {}
        }
        return payload;
    }
}
```

- [ ] **Step 2: Create CacheInvalidationSubscriber**

```java
package cn.projectan.strix.core.cache;

import cn.projectan.strix.model.event.cache.*;
import cn.projectan.strix.util.common.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 缓存失效订阅器
 * <p>
 * 监听 Redis Pub/Sub channel, 将远程消息重建为 Spring ApplicationEvent 并发布.
 * 使用 instanceId 防回环 — 忽略自身实例发布的消息.
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber implements MessageListener {

    @Value("${strix.instance-id:#{T(java.util.UUID).randomUUID().toString().substring(0, 8)}}")
    private String instanceId;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            CacheInvalidationMessage msg = ObjectMapperUtil.fromJson(json, CacheInvalidationMessage.class);

            if (msg == null || instanceId.equals(msg.getInstanceId())) {
                return; // 忽略自身发布的消息 (防回环)
            }

            log.info("收到远程缓存失效消息: type={}, from={}", msg.getEventType(), msg.getInstanceId());

            CacheInvalidationEvent event = rebuildEvent(msg);
            if (event != null) {
                eventPublisher.publishEvent(event);
            }
        } catch (Exception e) {
            log.error("处理远程缓存失效消息失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private CacheInvalidationEvent rebuildEvent(CacheInvalidationMessage msg) {
        Map<String, Object> payload = msg.getPayload();
        String srcInstanceId = msg.getInstanceId();

        return switch (msg.getEventType()) {
            case "MENU_CHANGED" ->
                    new MenuChangedEvent(this, srcInstanceId, true);
            case "PERMISSION_CHANGED" ->
                    new PermissionChangedEvent(this, srcInstanceId, true);
            case "ROLE_CHANGED" ->
                    new RoleChangedEvent(this, srcInstanceId, true, (String) payload.get("roleId"));
            case "ROLE_MENU_CHANGED" ->
                    new RoleMenuChangedEvent(this, srcInstanceId, true, (String) payload.get("roleId"));
            case "ROLE_PERMISSION_CHANGED" ->
                    new RolePermissionChangedEvent(this, srcInstanceId, true, (String) payload.get("roleId"));
            case "MANAGER_PERMISSION_CHANGED" ->
                    new ManagerPermissionChangedEvent(this, srcInstanceId, true, (String) payload.get("managerId"));
            case "CONFIG_CHANGED" ->
                    new ConfigChangedEvent(this, srcInstanceId, true, (String) payload.get("configKey"));
            case "REGION_CHANGED" ->
                    new RegionChangedEvent(this, srcInstanceId, true, (List<String>) payload.get("regionIds"));
            default -> {
                log.warn("未知的缓存失效事件类型: {}", msg.getEventType());
                yield null;
            }
        };
    }

    /**
     * 获取本实例的 instanceId (供 Broadcaster 使用)
     */
    public String getInstanceId() {
        return instanceId;
    }
}
```

- [ ] **Step 3: Modify RedisConfig — Add differentiated TTL + Pub/Sub listener container**

In `Strix/src/main/java/cn/projectan/strix/config/RedisConfig.java`:

**3a.** Change the default TTL from 30 days to 1 day (line 67 area):

Find:
```java
.entryTtl(Duration.ofDays(30))
```

Replace with:
```java
.entryTtl(Duration.ofDays(1))
```

**3b.** Uncomment and populate the per-prefix TTL map (lines 76-84 area). Find the commented-out `initialCacheConfigurations` section and replace it. Find the existing commented-out code like:

```java
//        Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();
```

Replace that block with:

```java
        Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();
        // 字典: 7 天 (低频变更)
        initialCacheConfigurations.put("dict_data", redisCacheConfiguration.entryTtl(Duration.ofDays(7)));
        initialCacheConfigurations.put("dict_version", redisCacheConfiguration.entryTtl(Duration.ofDays(7)));
        // 认证/菜单/权限/角色: 1 天 (默认)
        initialCacheConfigurations.put("select_data", redisCacheConfiguration.entryTtl(Duration.ofDays(1)));
        initialCacheConfigurations.put("menu_by_rid", redisCacheConfiguration.entryTtl(Duration.ofDays(1)));
        initialCacheConfigurations.put("permission_by_rid", redisCacheConfiguration.entryTtl(Duration.ofDays(1)));
        initialCacheConfigurations.put("menu_by_mid", redisCacheConfiguration.entryTtl(Duration.ofDays(1)));
        initialCacheConfigurations.put("permission_by_mid", redisCacheConfiguration.entryTtl(Duration.ofDays(1)));
        // 系统配置: 1 小时 (中频变更)
        initialCacheConfigurations.put("strix:system:config", redisCacheConfiguration.entryTtl(Duration.ofHours(1)));
        // 地区: 7 天 (低频变更)
        initialCacheConfigurations.put("strix:region_by_id", redisCacheConfiguration.entryTtl(Duration.ofDays(7)));
        initialCacheConfigurations.put("strix:region_children", redisCacheConfiguration.entryTtl(Duration.ofDays(7)));
```

**3c.** Ensure the `RedisCacheManager` builder uses `withInitialCacheConfigurations`. Find the builder and update it:

```java
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .withInitialCacheConfigurations(initialCacheConfigurations)
                .build();
```

**3d.** Add the Redis Pub/Sub listener container bean. Add this new method to the class:

```java
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            CacheInvalidationSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(subscriber,
                new org.springframework.data.redis.listener.ChannelTopic(
                        CacheInvalidationBroadcaster.CHANNEL));
        return container;
    }
```

Add necessary imports:

```java
import cn.projectan.strix.core.cache.CacheInvalidationBroadcaster;
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
```

- [ ] **Step 4: Add `strix.instance-id` to application config**

In `Strix/src/main/resources/application.yml` (or `application-dev.yml`), add:

```yaml
strix:
  instance-id: ${random.uuid}
```

This ensures each JVM instance gets a unique ID for Pub/Sub anti-loop.

- [ ] **Step 5: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL. Tasks 1-4 now form a complete, compilable unit.

- [ ] **Step 6: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(cache): add Redis Pub/Sub + differentiated TTL configuration

- CacheInvalidationBroadcaster: serialize events to JSON, publish to Redis channel
- CacheInvalidationSubscriber: receive messages, rebuild Spring events (remote=true)
- Anti-loop via strix.instance-id matching
- RedisConfig: default TTL 30d→1d, per-cache TTLs (dict 7d, config 1h, region 7d)
- RedisMessageListenerContainer bean for Pub/Sub subscription

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 5: Refactor Menu Domain — SystemMenuService + SystemMenuController

**Files:**
- Modify: `Strix/src/main/java/cn/projectan/strix/service/system/SystemMenuService.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/controller/system/SystemMenuController.java`

- [ ] **Step 1: Refactor SystemMenuService**

In `Strix/src/main/java/cn/projectan/strix/service/system/SystemMenuService.java`:

**1a.** Remove `SystemMenuCache` and `SystemPermissionCache` lazy field injections (lines 35-37 area):

Find:
```java
    @Lazy
    private final SystemMenuCache systemMenuCache;
    @Lazy
    private final SystemPermissionCache systemPermissionCache;
```

Remove these two fields entirely.

**1b.** Add `ApplicationEventPublisher` and `CacheInvalidationSubscriber` injections:

Add these fields (as final constructor-injected):

```java
    private final ApplicationEventPublisher eventPublisher;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;
```

Add imports:

```java
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import cn.projectan.strix.model.event.cache.MenuChangedEvent;
import cn.projectan.strix.model.event.cache.PermissionChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
```

Remove old imports:

```java
import cn.projectan.strix.core.cache.system.SystemMenuCache;
import cn.projectan.strix.core.cache.system.SystemPermissionCache;
import org.springframework.context.annotation.Lazy;
```

**1c.** In the `deleteByIds()` method, replace the manual cache calls with event publishing. Find (around lines 106-107):

```java
        systemMenuCache.updateRamAndRedis();
        systemPermissionCache.updateRamAndRedis();
```

Replace with:

```java
        String instanceId = cacheInvalidationSubscriber.getInstanceId();
        eventPublisher.publishEvent(new MenuChangedEvent(this, instanceId));
        eventPublisher.publishEvent(new PermissionChangedEvent(this, instanceId));
```

**1d.** Add public `getMenuAndChildrenIds()` method (to replace `SystemMenuCache.getIdListByParentMenu()`). Add after the existing `findMenuChildrenIdList()` private method:

```java
    /**
     * 获取指定菜单及其所有子菜单的 ID 集合
     * (替代原 SystemMenuCache.getIdListByParentMenu())
     *
     * @param menuId 菜单 ID
     * @return 包含自身和所有子菜单的 ID 集合
     */
    public Set<String> getMenuAndChildrenIds(String menuId) {
        List<SystemMenu> allMenus = list();
        return findMenuChildrenIdList(allMenus, List.of(menuId));
    }
```

- [ ] **Step 2: Refactor SystemMenuController**

In `Strix/src/main/java/cn/projectan/strix/controller/system/SystemMenuController.java`:

**2a.** Remove `SystemMenuCache` field injection (line 56 area):

Find and remove:

```java
    private final SystemMenuCache systemMenuCache;
```

Remove import:

```java
import cn.projectan.strix.core.cache.system.SystemMenuCache;
```

**2b.** Add `ApplicationEventPublisher` and `CacheInvalidationSubscriber` injections:

```java
    private final ApplicationEventPublisher eventPublisher;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;
```

Add imports:

```java
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import cn.projectan.strix.model.event.cache.MenuChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
```

**2c.** In the `add` method (around line 102), replace:

```java
        systemMenuCache.updateRamAndRedis();
```

With:

```java
        eventPublisher.publishEvent(new MenuChangedEvent(this, cacheInvalidationSubscriber.getInstanceId()));
```

**2d.** In the `update` method (around line 130), replace the same cache call:

```java
        systemMenuCache.updateRamAndRedis();
```

With:

```java
        eventPublisher.publishEvent(new MenuChangedEvent(this, cacheInvalidationSubscriber.getInstanceId()));
```

**2e.** In the `delete` method (around lines 151-153), remove both the cache call and the manual LoginInfo refresh:

Find:

```java
        systemMenuCache.updateRamAndRedis();

        systemManagerService.refreshLoginInfoByMenu(menuId);
```

Replace with:

```java
        eventPublisher.publishEvent(new MenuChangedEvent(this, cacheInvalidationSubscriber.getInstanceId()));
```

(The `CacheEvictionListener` handles `refreshLoginInfoForAllOnlineManagers()` as part of the `MenuChangedEvent` cascade.)

**2f.** Remove the unused `findSystemMenuChildrenIdList` private method (lines 179-196 area) if it exists and is not used by anything else in this controller.

**2g.** Remove `SystemManagerService` injection if it's only used for `refreshLoginInfoByMenu()`. Check first — if other methods still use it, keep it.

- [ ] **Step 3: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
cd Strix && git add -A && git commit -m "refactor(cache): menu domain — event-driven cache invalidation

- SystemMenuService: remove SystemMenuCache/SystemPermissionCache deps,
  publish MenuChangedEvent/PermissionChangedEvent, add getMenuAndChildrenIds()
- SystemMenuController: remove SystemMenuCache dep,
  replace manual cache + refreshLoginInfo with MenuChangedEvent

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 6: Refactor Permission Domain — SystemPermissionService + SystemPermissionController

**Files:**
- Modify: `Strix/src/main/java/cn/projectan/strix/service/system/SystemPermissionService.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/controller/system/SystemPermissionController.java`

- [ ] **Step 1: Refactor SystemPermissionService**

In `Strix/src/main/java/cn/projectan/strix/service/system/SystemPermissionService.java`:

**1a.** Remove `SystemMenuCache` and `SystemPermissionCache` lazy field injections (lines 35-37):

Find and remove:

```java
    @Lazy
    private final SystemMenuCache systemMenuCache;
    @Lazy
    private final SystemPermissionCache systemPermissionCache;
```

**1b.** Add event publishing fields:

```java
    private final ApplicationEventPublisher eventPublisher;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;
```

Add imports:

```java
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import cn.projectan.strix.model.event.cache.PermissionChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
```

Remove old imports:

```java
import cn.projectan.strix.core.cache.system.SystemMenuCache;
import cn.projectan.strix.core.cache.system.SystemPermissionCache;
import org.springframework.context.annotation.Lazy;
```

**1c.** In `deleteByIds()` (around lines 84-85), replace:

```java
        systemMenuCache.updateRamAndRedis();
        systemPermissionCache.updateRamAndRedis();
```

With (BUG FIX: `systemMenuCache.updateRamAndRedis()` was erroneous here — deleting permissions should NOT evict menu cache):

```java
        eventPublisher.publishEvent(new PermissionChangedEvent(this, cacheInvalidationSubscriber.getInstanceId()));
```

- [ ] **Step 2: Refactor SystemPermissionController**

In `Strix/src/main/java/cn/projectan/strix/controller/system/SystemPermissionController.java`:

**2a.** Remove `SystemPermissionCache` field injection (line 49):

```java
    private final SystemPermissionCache systemPermissionCache;
```

**2b.** Add event publishing fields:

```java
    private final ApplicationEventPublisher eventPublisher;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;
```

Add imports:

```java
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import cn.projectan.strix.model.event.cache.PermissionChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
```

Remove:

```java
import cn.projectan.strix.core.cache.system.SystemPermissionCache;
```

**2c.** In the `add` method (around line 97), replace:

```java
        systemPermissionCache.updateRamAndRedis();
```

With:

```java
        eventPublisher.publishEvent(new PermissionChangedEvent(this, cacheInvalidationSubscriber.getInstanceId()));
```

**2d.** In the `update` method (around lines 118-120), replace:

```java
        systemPermissionCache.updateRamAndRedis();

        systemManagerService.refreshLoginInfoByPermission(permissionId);
```

With:

```java
        eventPublisher.publishEvent(new PermissionChangedEvent(this, cacheInvalidationSubscriber.getInstanceId()));
```

**2e.** Remove `SystemManagerService` field injection if only used for `refreshLoginInfoByPermission()`.

- [ ] **Step 3: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
cd Strix && git add -A && git commit -m "refactor(cache): permission domain — event-driven cache invalidation

- SystemPermissionService: remove cache class deps, publish PermissionChangedEvent
  FIX: remove erroneous systemMenuCache eviction on permission delete
- SystemPermissionController: remove SystemPermissionCache dep,
  replace manual cache + refreshLoginInfo with PermissionChangedEvent

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 7: Refactor Role Domain — SystemRoleService + SystemRoleController

**Files:**
- Modify: `Strix/src/main/java/cn/projectan/strix/service/system/SystemRoleService.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/controller/system/SystemRoleController.java`

- [ ] **Step 1: Refactor SystemRoleService**

In `Strix/src/main/java/cn/projectan/strix/service/system/SystemRoleService.java`:

**1a.** Add event publishing fields:

```java
    private final ApplicationEventPublisher eventPublisher;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;
```

Add imports:

```java
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import cn.projectan.strix.model.event.cache.RoleChangedEvent;
import cn.projectan.strix.model.event.cache.RoleMenuChangedEvent;
import cn.projectan.strix.model.event.cache.RolePermissionChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
```

**1b.** In `deleteRoleWithRelations()` (around line 152), add event publishing after the existing delete logic. Find the end of the method (before the closing `}`) and add:

```java
        String instanceId = cacheInvalidationSubscriber.getInstanceId();
        eventPublisher.publishEvent(new RoleChangedEvent(this, instanceId, role.getId()));
        eventPublisher.publishEvent(new RoleMenuChangedEvent(this, instanceId, role.getId()));
        eventPublisher.publishEvent(new RolePermissionChangedEvent(this, instanceId, role.getId()));
```

- [ ] **Step 2: Refactor SystemRoleController**

In `Strix/src/main/java/cn/projectan/strix/controller/system/SystemRoleController.java`:

**2a.** Remove `SystemMenuCache` and `SystemPermissionCache` field injections (lines 61-62):

```java
    private final SystemMenuCache systemMenuCache;
    private final SystemPermissionCache systemPermissionCache;
```

**2b.** Add event publishing and service fields:

```java
    private final ApplicationEventPublisher eventPublisher;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;
    private final SystemMenuService systemMenuService;
```

Add imports:

```java
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import cn.projectan.strix.model.event.cache.RoleChangedEvent;
import cn.projectan.strix.model.event.cache.RoleMenuChangedEvent;
import cn.projectan.strix.model.event.cache.RolePermissionChangedEvent;
import cn.projectan.strix.model.event.cache.ManagerPermissionChangedEvent;
import cn.projectan.strix.service.system.SystemMenuService;
import org.springframework.context.ApplicationEventPublisher;
```

Remove old imports:

```java
import cn.projectan.strix.core.cache.system.SystemMenuCache;
import cn.projectan.strix.core.cache.system.SystemPermissionCache;
```

**2c.** In the `update` method (role basic info update, around line 132), remove the manual refresh:

Find and remove:

```java
        systemManagerService.refreshLoginInfoByRole(id);
```

And add event publishing:

```java
        String instanceId = cacheInvalidationSubscriber.getInstanceId();
        eventPublisher.publishEvent(new RoleChangedEvent(this, instanceId, id));
```

**2d.** In the `updateRoleMenu` method (KeyDiffUtil callback, around lines 165-189), find the `updatedFunc` callback that currently has cache calls and refreshLoginInfo. Replace the cache update lines and refreshLoginInfo:

Find lines like:

```java
            systemMenuCache.updateRamAndRedis();
            systemPermissionCache.updateRamAndRedis();
```

And:

```java
        systemManagerService.refreshLoginInfoByRole(roleId);
```

Replace all of these with a single event publish (in the `updatedFunc` or after the `KeyDiffUtil.handle` call):

```java
        String instanceId = cacheInvalidationSubscriber.getInstanceId();
        eventPublisher.publishEvent(new RoleMenuChangedEvent(this, instanceId, roleId));
```

**2e.** In `removeRoleMenu()` (around line 251), replace:

```java
        Set<String> allMenuIds = systemMenuCache.getIdListByParentMenu(menuId);
```

With:

```java
        Set<String> allMenuIds = systemMenuService.getMenuAndChildrenIds(menuId);
```

And remove the cache calls/refresh calls below it (around lines 256-258):

```java
        systemMenuCache.updateRamAndRedis();
        systemPermissionCache.updateRamAndRedis();
        systemManagerService.refreshLoginInfoByRole(roleId);
```

Replace with:

```java
        eventPublisher.publishEvent(new RoleMenuChangedEvent(this, cacheInvalidationSubscriber.getInstanceId(), roleId));
```

**2f.** In `updateRolePermission()` (KeyDiffUtil callback, around lines 274-284), replace the cache calls and refreshLoginInfo:

Find lines like:

```java
            systemMenuCache.updateRamAndRedis();
            systemPermissionCache.updateRamAndRedis();
```

And:

```java
        systemManagerService.refreshLoginInfoByRole(roleId);
```

Replace with:

```java
        String instanceId = cacheInvalidationSubscriber.getInstanceId();
        eventPublisher.publishEvent(new RolePermissionChangedEvent(this, instanceId, roleId));
```

**2g.** Remove `SystemManagerService` injection if no longer used directly (check if other methods still need it).

- [ ] **Step 3: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
cd Strix && git add -A && git commit -m "refactor(cache): role domain — event-driven cache invalidation

- SystemRoleService: publish RoleChanged + RoleMenuChanged + RolePermissionChanged
  on deleteRoleWithRelations (FIXES: zero @CacheEvict bug)
- SystemRoleController: remove SystemMenuCache/SystemPermissionCache deps,
  replace all manual cache/refresh with events, use SystemMenuService.getMenuAndChildrenIds()

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 8: Refactor Manager Domain — SystemManagerController

**Files:**
- Modify: `Strix/src/main/java/cn/projectan/strix/controller/system/SystemManagerController.java`

- [ ] **Step 1: Refactor SystemManagerController**

In `Strix/src/main/java/cn/projectan/strix/controller/system/SystemManagerController.java`:

**1a.** Add event publishing fields:

```java
    private final ApplicationEventPublisher eventPublisher;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;
```

Add imports:

```java
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import cn.projectan.strix.model.event.cache.ManagerPermissionChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
```

**1b.** At line 166 (update manager role assignment), replace:

```java
        systemManagerService.refreshLoginInfoByManager(managerId);
```

With:

```java
        eventPublisher.publishEvent(new ManagerPermissionChangedEvent(this, cacheInvalidationSubscriber.getInstanceId(), managerId));
```

**1c.** At line 235 (another place with refreshLoginInfoByManager), apply the same replacement:

```java
        eventPublisher.publishEvent(new ManagerPermissionChangedEvent(this, cacheInvalidationSubscriber.getInstanceId(), managerId));
```

- [ ] **Step 2: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
cd Strix && git add -A && git commit -m "refactor(cache): manager domain — event-driven cache invalidation

- SystemManagerController: replace manual refreshLoginInfoByManager
  with ManagerPermissionChangedEvent for role assignment changes

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 9: Refactor Region Domain — SystemRegionService + SystemRegionController

**Files:**
- Modify: `Strix/src/main/java/cn/projectan/strix/service/system/SystemRegionService.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/controller/system/SystemRegionController.java`

- [ ] **Step 1: Refactor SystemRegionService**

In `Strix/src/main/java/cn/projectan/strix/service/system/SystemRegionService.java`:

**1a.** Remove `SystemRegionCache` field injection (line 42):

Find and remove:

```java
    private final SystemRegionCache systemRegionCache;
```

**1b.** Add event publishing fields:

```java
    private final ApplicationEventPublisher eventPublisher;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;
```

Add imports:

```java
import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import cn.projectan.strix.model.event.cache.RegionChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
```

Remove:

```java
import cn.projectan.strix.core.cache.system.SystemRegionCache;
```

**1c.** Replace all `systemRegionCache.refreshRedisCacheById(regionId)` calls (lines 225, 246, 257, 259 area) with event publishing. For each occurrence, replace:

```java
        systemRegionCache.refreshRedisCacheById(regionId);
```

With:

```java
        eventPublisher.publishEvent(new RegionChangedEvent(this, cacheInvalidationSubscriber.getInstanceId(), List.of(regionId)));
```

If there's a `refreshRedisCache()` (all regions) call, replace with:

```java
        eventPublisher.publishEvent(new RegionChangedEvent(this, cacheInvalidationSubscriber.getInstanceId(), List.of()));
```

(Empty list means evict all region caches.)

**1d.** For the `refreshRelevantRegionCache()` method (around line 156), if it collects multiple region IDs, publish a single event with all IDs:

```java
        eventPublisher.publishEvent(new RegionChangedEvent(this, cacheInvalidationSubscriber.getInstanceId(), affectedRegionIds));
```

- [ ] **Step 2: Refactor SystemRegionController**

In `Strix/src/main/java/cn/projectan/strix/controller/system/SystemRegionController.java`:

**2a.** Remove `SystemRegionCache` field injection (line 54):

```java
    private final SystemRegionCache systemRegionCache;
```

Remove import:

```java
import cn.projectan.strix.core.cache.system.SystemRegionCache;
```

**2b.** Remove all `systemRegionCache.refreshRedisCacheById()` calls (lines 137-138, 234, 238 area). These are now handled by events published from the service layer.

- [ ] **Step 3: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
cd Strix && git add -A && git commit -m "refactor(cache): region domain — event-driven cache invalidation

- SystemRegionService: remove SystemRegionCache dep, publish RegionChangedEvent
- SystemRegionController: remove SystemRegionCache dep + manual cache calls

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 10: Delete Old Cache Classes + Fix References

**Files:**
- Delete: `Strix/src/main/java/cn/projectan/strix/core/cache/system/SystemMenuCache.java`
- Delete: `Strix/src/main/java/cn/projectan/strix/core/cache/system/SystemPermissionCache.java`
- Delete: `Strix/src/main/java/cn/projectan/strix/core/cache/system/SystemRegionCache.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/aot/LambdaRegistrationFeature.java`

- [ ] **Step 1: Delete SystemMenuCache.java**

```bash
rm Strix/src/main/java/cn/projectan/strix/core/cache/system/SystemMenuCache.java
```

- [ ] **Step 2: Delete SystemPermissionCache.java**

```bash
rm Strix/src/main/java/cn/projectan/strix/core/cache/system/SystemPermissionCache.java
```

- [ ] **Step 3: Delete SystemRegionCache.java**

```bash
rm Strix/src/main/java/cn/projectan/strix/core/cache/system/SystemRegionCache.java
```

- [ ] **Step 4: Fix LambdaRegistrationFeature**

In `Strix/src/main/java/cn/projectan/strix/aot/LambdaRegistrationFeature.java`, find and remove the import and reference to `SystemMenuCache` (line 47 area):

Find and remove:

```java
import cn.projectan.strix.core.cache.system.SystemMenuCache;
```

And remove any registration line that references `SystemMenuCache`, e.g.:

```java
        registerLambdaClass(access, SystemMenuCache.class);
```

- [ ] **Step 5: Search for any remaining references to deleted classes**

```bash
cd Strix && grep -rn "SystemMenuCache\|SystemPermissionCache\|SystemRegionCache" src/ --include="*.java"
```

Expected: No results. If any references remain, fix them.

- [ ] **Step 6: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
cd Strix && git add -A && git commit -m "refactor(cache): delete legacy RAM cache classes

- Delete SystemMenuCache (volatile RAM + Redis dual-layer)
- Delete SystemPermissionCache (volatile RAM, getByIds was dead code)
- Delete SystemRegionCache (Redis-only wrapper, replaced by events)
- Fix LambdaRegistrationFeature: remove SystemMenuCache reference

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 11: Refactor SystemConfigCache + Expand SystemConfigService + CRUD Controller

**Files:**
- Modify: `Strix/src/main/java/cn/projectan/strix/core/cache/system/SystemConfigCache.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/service/system/SystemConfigService.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/request/system/config/SystemConfigUpdateReq.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/response/system/config/SystemConfigListResp.java`
- Create: `Strix/src/main/java/cn/projectan/strix/controller/system/SystemConfigController.java`

- [ ] **Step 1: Rewrite SystemConfigCache — Replace ConcurrentHashMap with @Cacheable**

Replace the entire content of `Strix/src/main/java/cn/projectan/strix/core/cache/system/SystemConfigCache.java`:

```java
package cn.projectan.strix.core.cache.system;

import cn.projectan.strix.service.system.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 系统配置缓存
 * <p>
 * 对外提供 get / getBoolean / getLong API (保持向后兼容).
 * 内部通过 @Cacheable("strix:system:config") 缓存, TTL 1小时.
 * 缓存失效由 CacheEvictionService.evictConfigCache(key) 处理.
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemConfigCache {

    private final SystemConfigService systemConfigService;

    /**
     * 获取配置值
     *
     * @param key          配置 Key
     * @param defaultValue 默认值
     * @return 配置值, 如不存在返回默认值
     */
    public String get(String key, String defaultValue) {
        String value = getFromCache(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取 Boolean 配置值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getFromCache(key);
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 获取 Long 配置值
     */
    public long getLong(String key, long defaultValue) {
        String value = getFromCache(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("系统配置 {} 值 {} 无法转换为 Long, 使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    @Cacheable(value = "strix:system:config", key = "#key", unless = "#result == null")
    public String getFromCache(String key) {
        return systemConfigService.getValueByKey(key);
    }
}
```

- [ ] **Step 2: Expand SystemConfigService with full CRUD + getValueByKey**

Replace the entire content of `Strix/src/main/java/cn/projectan/strix/service/system/SystemConfigService.java`:

```java
package cn.projectan.strix.service.system;

import cn.projectan.strix.core.cache.CacheInvalidationSubscriber;
import cn.projectan.strix.mapper.system.SystemConfigMapper;
import cn.projectan.strix.model.db.system.SystemConfig;
import cn.projectan.strix.model.event.cache.ConfigChangedEvent;
import cn.projectan.strix.model.request.system.config.SystemConfigUpdateReq;
import cn.projectan.strix.model.response.system.config.SystemConfigListResp;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 系统配置服务
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    private final ApplicationEventPublisher eventPublisher;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;

    /**
     * 按 Key 获取配置值 (用于 SystemConfigCache @Cacheable)
     *
     * @param key 配置 Key
     * @return 配置值, 不存在返回 null
     */
    public String getValueByKey(String key) {
        SystemConfig config = lambdaQuery()
                .eq(SystemConfig::getKey, key)
                .one();
        return config != null ? config.getValue() : null;
    }

    /**
     * 分页查询配置列表
     */
    public Page<SystemConfig> listPage(String keyword, int pageIndex, int pageSize) {
        return lambdaQuery()
                .like(StringUtils.hasText(keyword), SystemConfig::getName, keyword)
                .or(StringUtils.hasText(keyword))
                .like(StringUtils.hasText(keyword), SystemConfig::getKey, keyword)
                .orderByAsc(SystemConfig::getKey)
                .page(new Page<>(pageIndex, pageSize));
    }

    /**
     * 新增配置
     */
    public void addConfig(SystemConfigUpdateReq req) {
        SystemConfig config = new SystemConfig()
                .setKey(req.getKey())
                .setName(req.getName())
                .setType(req.getType())
                .setValue(req.getValue())
                .setRemark(req.getRemark());

        UniqueChecker.check(config);
        Assert.isTrue(save(config), I18nUtil.get("error.save.failed"));

        eventPublisher.publishEvent(new ConfigChangedEvent(this, cacheInvalidationSubscriber.getInstanceId(), req.getKey()));
    }

    /**
     * 修改配置
     */
    public void updateConfig(String id, SystemConfigUpdateReq req) {
        SystemConfig config = getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.systemConfig"));

        String oldKey = config.getKey();

        config.setKey(req.getKey());
        config.setName(req.getName());
        config.setType(req.getType());
        config.setValue(req.getValue());
        config.setRemark(req.getRemark());

        UniqueChecker.check(config);
        Assert.isTrue(updateById(config), I18nUtil.get("error.update.failed"));

        // 如果 key 变了, 需要清除旧 key 和新 key 的缓存
        String instanceId = cacheInvalidationSubscriber.getInstanceId();
        eventPublisher.publishEvent(new ConfigChangedEvent(this, instanceId, oldKey));
        if (!oldKey.equals(req.getKey())) {
            eventPublisher.publishEvent(new ConfigChangedEvent(this, instanceId, req.getKey()));
        }
    }

    /**
     * 删除配置
     */
    public void deleteConfig(String id) {
        SystemConfig config = getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.systemConfig"));

        Assert.isTrue(removeById(id), I18nUtil.get("error.delete.failed"));

        eventPublisher.publishEvent(new ConfigChangedEvent(this, cacheInvalidationSubscriber.getInstanceId(), config.getKey()));
    }
}
```

- [ ] **Step 3: Create SystemConfigUpdateReq**

```java
package cn.projectan.strix.model.request.system.config;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统配置更新请求
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Schema(description = "系统配置更新请求")
@Data
public class SystemConfigUpdateReq {

    @Schema(description = "配置 Key")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.systemConfig.key}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 128)
    @UpdateField
    private String key;

    @Schema(description = "配置名称")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.systemConfig.name}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 128)
    @UpdateField
    private String name;

    @Schema(description = "配置类型")
    @UpdateField
    private Short type;

    @Schema(description = "配置值")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.systemConfig.value}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 2048)
    @UpdateField
    private String value;

    @Schema(description = "备注")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, max = 255)
    @UpdateField(allowEmpty = true)
    private String remark;
}
```

- [ ] **Step 4: Create SystemConfigListResp**

```java
package cn.projectan.strix.model.response.system.config;

import cn.projectan.strix.model.db.system.SystemConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 系统配置列表响应
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Schema(description = "系统配置列表响应")
@Data
@NoArgsConstructor
public class SystemConfigListResp {

    @Schema(description = "配置列表")
    private List<SystemConfigItem> items;

    @Schema(description = "总数")
    private long total;

    public SystemConfigListResp(List<SystemConfig> configs, long total) {
        this.items = configs.stream().map(SystemConfigItem::new).toList();
        this.total = total;
    }

    @Schema(description = "配置列表项")
    @Data
    @NoArgsConstructor
    public static class SystemConfigItem {
        private String id;
        private String key;
        private String name;
        private Short type;
        private String value;
        private String remark;
        private String createdTime;

        public SystemConfigItem(SystemConfig config) {
            this.id = config.getId();
            this.key = config.getKey();
            this.name = config.getName();
            this.type = config.getType();
            this.value = config.getValue();
            this.remark = config.getRemark();
            this.createdTime = config.getCreatedTime() != null ? config.getCreatedTime().toString() : null;
        }
    }
}
```

- [ ] **Step 5: Create SystemConfigController**

```java
package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemConfig;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.config.SystemConfigUpdateReq;
import cn.projectan.strix.model.response.system.config.SystemConfigListResp;
import cn.projectan.strix.service.system.SystemConfigService;
import cn.projectan.strix.util.common.I18nUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置管理
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@RestController
@RequestMapping("system/config")
@RequiredArgsConstructor
@Tag(name = "系统 - 配置管理")
public class SystemConfigController extends BaseSystemController {

    private final SystemConfigService systemConfigService;

    @Operation(summary = "配置列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:config')")
    @StrixLog(operationGroup = "系统配置", operationName = "查询配置列表")
    public RetResult<SystemConfigListResp> list(
            @RequestParam(defaultValue = "1") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Page<SystemConfig> page = systemConfigService.listPage(keyword, pageIndex, pageSize);
        return RetBuilder.success(new SystemConfigListResp(page.getRecords(), page.getTotal()));
    }

    @Operation(summary = "配置详情")
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:config')")
    @StrixLog(operationGroup = "系统配置", operationName = "查询配置详情")
    public RetResult<SystemConfig> detail(@Parameter(description = "配置 ID") @PathVariable String id) {
        SystemConfig config = systemConfigService.getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.systemConfig"));
        return RetBuilder.success(config);
    }

    @Operation(summary = "新增配置")
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:config:add')")
    @StrixLog(operationGroup = "系统配置", operationName = "新增配置", operationType = SystemLogOperType.ADD)
    public RetResult<Object> add(@RequestBody @Validated(InsertGroup.class) SystemConfigUpdateReq req) {
        systemConfigService.addConfig(req);
        return RetBuilder.success();
    }

    @Operation(summary = "修改配置")
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    @StrixLog(operationGroup = "系统配置", operationName = "修改配置", operationType = SystemLogOperType.UPDATE)
    public RetResult<Object> update(@Parameter(description = "配置 ID") @PathVariable String id,
                                    @RequestBody @Validated(UpdateGroup.class) SystemConfigUpdateReq req) {
        systemConfigService.updateConfig(id, req);
        return RetBuilder.success();
    }

    @Operation(summary = "删除配置")
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:config:delete')")
    @StrixLog(operationGroup = "系统配置", operationName = "删除配置", operationType = SystemLogOperType.DELETE)
    public RetResult<Object> remove(@Parameter(description = "配置 ID") @PathVariable String id) {
        systemConfigService.deleteConfig(id);
        return RetBuilder.success();
    }
}
```

- [ ] **Step 6: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(cache): refactor SystemConfigCache + add SystemConfig CRUD

- SystemConfigCache: replace ConcurrentHashMap with @Cacheable per-key
  Keep get/getBoolean/getLong API for backward compatibility
- SystemConfigService: full CRUD with event publishing
- SystemConfigUpdateReq: validation with InsertGroup/UpdateGroup
- SystemConfigListResp: list response DTO
- SystemConfigController: list/detail/add/update/delete with permissions

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 12: SystemConfig Frontend — API + Page + Route + Menu SQL

**Files:**
- Create: `StrixPage/src/api/system-config.ts`
- Create: `StrixPage/src/views/System/SystemConfig/SystemConfigIndex.vue`
- Modify: `StrixPage/src/router/index.ts`

- [ ] **Step 1: Create system-config.ts API module**

```typescript
import type { RetResult } from './types'
import { http } from '@/plugins/axios'

const _n = '系统配置'
const BASE = 'system/config'

/** 配置列表项 */
export interface SystemConfigItem {
  id: string
  key: string
  name: string
  type: number
  value: string
  remark: string
  createdTime: string
}

/** 配置列表响应 */
export interface SystemConfigListResp {
  items: SystemConfigItem[]
  total: number
}

/** 配置更新请求 */
export interface SystemConfigUpdateReq {
  key: string
  name: string
  type: number
  value: string
  remark: string
}

export const systemConfigApi = {
  urls: {
    list: BASE
  },

  list: (params: Record<string, any>) =>
    http.get<RetResult<SystemConfigListResp>>(BASE, { params, meta: { operate: `加载${_n}列表` } }),

  detail: (id: string) =>
    http.get<RetResult<SystemConfigItem>>(`${BASE}/${id}`, { meta: { operate: `加载${_n}详情` } }),

  create: (data: SystemConfigUpdateReq) =>
    http.post<RetResult>(`${BASE}/update`, data, { meta: { operate: `新增${_n}` } }),

  update: (id: string, data: SystemConfigUpdateReq) =>
    http.post<RetResult>(`${BASE}/update/${id}`, data, { meta: { operate: `编辑${_n}` } }),

  remove: (id: string) =>
    http.post<RetResult>(`${BASE}/remove/${id}`, null, { meta: { operate: `删除${_n}` } })
}
```

- [ ] **Step 2: Create SystemConfigIndex.vue**

```vue
<template>
  <div>
    <strix-block>
      <template #body>
        <n-grid :cols="6" :x-gap="20" :y-gap="10" item-responsive responsive="screen">
          <n-gi span="6 s:3 m:2">
            <n-input
              v-model:value="listParams.keyword"
              clearable
              placeholder="搜索配置名称 / Key"
              @keydown.enter="handleKeywordEnter"
            />
          </n-gi>
          <n-gi span="6 s:3 m:4" class="nebula-export__trigger-gi">
            <n-flex :size="4" align="center">
              <n-button
                v-auth="'system:config:add'"
                type="primary"
                @click="showAdd()"
              >
                新增配置
              </n-button>
            </n-flex>
          </n-gi>
        </n-grid>
      </template>
    </strix-block>
    <n-data-table
      :columns="dataColumns"
      :data="listParams._data?.items ?? []"
      :loading="listParams._loading"
      :bordered="false"
      :single-line="false"
      :row-key="rowKey"
      size="small"
      :pagination="pagination"
      remote
    />

    <!-- 新增模态框 -->
    <n-modal v-model:show="addModal" class="strix-form-modal" preset="card" title="新增配置" style="width: 600px" @after-leave="resetForms">
      <n-form ref="addFormRef" :model="addForm" :rules="formRules" label-placement="left" label-width="80">
        <n-form-item label="配置 Key" path="key">
          <n-input v-model:value="addForm.key" maxlength="128" placeholder="请输入配置 Key" />
        </n-form-item>
        <n-form-item label="配置名称" path="name">
          <n-input v-model:value="addForm.name" maxlength="128" placeholder="请输入配置名称" />
        </n-form-item>
        <n-form-item label="配置类型" path="type">
          <n-select v-model:value="addForm.type" :options="configTypeOptions" placeholder="请选择类型" />
        </n-form-item>
        <n-form-item label="配置值" path="value">
          <n-input
            v-model:value="addForm.value"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            maxlength="2048"
            show-count
            placeholder="请输入配置值"
          />
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input v-model:value="addForm.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" maxlength="255" placeholder="请输入备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-flex justify="end">
          <n-button @click="tryCloseAdd()">取消</n-button>
          <n-button type="primary" @click="submitAdd()">确定</n-button>
        </n-flex>
      </template>
    </n-modal>

    <!-- 编辑模态框 -->
    <n-modal v-model:show="editModal" class="strix-form-modal" preset="card" title="编辑配置" style="width: 600px" @after-leave="resetForms">
      <n-form ref="editFormRef" :model="editForm" :rules="formRules" label-placement="left" label-width="80">
        <n-form-item label="配置 Key" path="key">
          <n-input v-model:value="editForm.key" maxlength="128" placeholder="请输入配置 Key" />
        </n-form-item>
        <n-form-item label="配置名称" path="name">
          <n-input v-model:value="editForm.name" maxlength="128" placeholder="请输入配置名称" />
        </n-form-item>
        <n-form-item label="配置类型" path="type">
          <n-select v-model:value="editForm.type" :options="configTypeOptions" placeholder="请选择类型" />
        </n-form-item>
        <n-form-item label="配置值" path="value">
          <n-input
            v-model:value="editForm.value"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            maxlength="2048"
            show-count
            placeholder="请输入配置值"
          />
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input v-model:value="editForm.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" maxlength="255" placeholder="请输入备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-flex justify="end">
          <n-button @click="tryCloseEdit()">取消</n-button>
          <n-button type="primary" :loading="editLoading" @click="submitEdit()">确定</n-button>
        </n-flex>
      </template>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import { systemConfigApi } from '@/api/system-config'
import type { SystemConfigItem } from '@/api/system-config'
import { useCrud } from '@/composables/useCrud'
import { textField, selectField } from '@/utils/form-rules'
import { handleOperate } from '@/utils/strix-table-tool'
import type { DataTableColumns } from 'naive-ui'

const configTypeOptions = [
  { label: '文本', value: 1 },
  { label: '数字', value: 2 },
  { label: '布尔', value: 3 },
  { label: 'JSON', value: 4 }
]

const formRules = {
  key: textField('配置 Key'),
  name: textField('配置名称'),
  type: selectField('配置类型'),
  value: textField('配置值')
}

const {
  listParams,
  pagination,
  rowKey,
  addModal,
  addForm,
  addFormRef,
  editModal,
  editLoading,
  editForm,
  editFormRef,
  showAdd,
  showEdit,
  submitAdd,
  submitEdit,
  deleteRow,
  resetForms,
  tryCloseAdd,
  tryCloseEdit,
  handleKeywordEnter
} = useCrud({
  list: {
    keyword: null,
    pageIndex: 1,
    pageSize: 10
  },
  fetchList: () => systemConfigApi.list(listParams),
  addForm: {
    key: null,
    name: null,
    type: 1,
    value: null,
    remark: null
  },
  editForm: {
    key: null,
    name: null,
    type: null,
    value: null,
    remark: null
  },
  api: systemConfigApi,
  draftKey: 'SystemConfig',
  urlSync: true
})

const dataColumns: DataTableColumns<SystemConfigItem> = [
  { title: 'Key', key: 'key', width: 200, ellipsis: { tooltip: true } },
  { title: '名称', key: 'name', width: 180, ellipsis: { tooltip: true } },
  {
    title: '类型',
    key: 'type',
    width: 80,
    render: (row) => {
      const map: Record<number, string> = { 1: '文本', 2: '数字', 3: '布尔', 4: 'JSON' }
      return map[row.type] ?? '未知'
    }
  },
  { title: '值', key: 'value', ellipsis: { tooltip: true } },
  { title: '备注', key: 'remark', width: 150, ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: (row) =>
      handleOperate([
        {
          auth: 'system:config:update',
          label: '编辑',
          icon: 'edit',
          onClick: () => showEdit(row)
        },
        {
          auth: 'system:config:delete',
          type: 'error',
          label: '删除',
          icon: 'trash-2',
          popconfirm: true,
          popconfirmMessage: `确定删除配置「${row.name}」吗？`,
          onClick: () => deleteRow(row)
        }
      ])
  }
]
</script>
```

- [ ] **Step 3: Add SystemConfig route in router/index.ts**

In `StrixPage/src/router/index.ts`, add a new route after the dict route (after the dict route entry, before the closing of the system routes). Find the dict route entry (around line 120-122 area):

```typescript
          },
          {
            path: 'region',
```

Insert before the `region` route:

```typescript
          },
          {
            path: 'config',
            name: 'SystemConfigIndex',
            component: () => import('@/views/System/SystemConfig/SystemConfigIndex.vue'),
            meta: {
              title: '系统配置',
              empty: false,
              permission: 'system:config'
            }
          },
          {
            path: 'region',
```

- [ ] **Step 4: Verify frontend builds**

```bash
cd StrixPage && pnpm build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run frontend lint**

```bash
cd StrixPage && pnpm lint
```

Expected: No errors. Fix any lint issues found.

- [ ] **Step 6: Commit**

```bash
cd StrixPage && git add -A && git commit -m "feat(config): add SystemConfig management page

- API module: list, detail, create, update, remove
- SystemConfigIndex.vue: useCrud pattern, search by key/name
- Route: /system/config with permission system:config

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 13: Build Verification + DB Menu Entries + E2E Testing

**Files:** None (verification + SQL + testing)

- [ ] **Step 1: Run full backend build with tests**

```bash
cd Strix && ./gradlew build
```

Expected: BUILD SUCCESSFUL (3 pre-existing test failures are known and unrelated)

- [ ] **Step 2: Run full frontend build**

```bash
cd StrixPage && pnpm build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Restart backend**

Kill and restart the backend process:

```bash
cd Strix && ./gradlew bootRun
```

Expected: Application starts on port 9889. Check logs for:
- `SSE 心跳调度已启动` (from SseSessionManager if still present)
- No errors about missing beans or cache configuration

- [ ] **Step 4: Insert SystemConfig menu entries**

Execute SQL to add the 系统配置 menu and role assignment:

```sql
INSERT INTO strix.sys_system_menu (id, `key`, name, url, icon, parent_id, sort_value, deleted_status, created_time, created_by_type, created_by, updated_time, updated_by_type, updated_by)
VALUES (2044318632242098300, 'system:config', '系统配置', '/system/config', 'settings', 1506970733068779522, 7, 0, NOW(), 1, 1111111111111111111, NOW(), 1, 1111111111111111111);

INSERT INTO strix.sys_system_role_menu (system_role_id, system_menu_id)
VALUES (1000000000000000000, 2044318632242098300);
```

Note: `1506970733068779522` is the parent menu ID for "系统管理" group. Adjust the parent_id and sort_value based on the actual menu tree structure.

- [ ] **Step 5: Re-login to refresh permissions**

Log out and log back in with username `anjiongyi`, password `Zhangyi1024!`.

- [ ] **Step 6: E2E test — SystemConfig page**

Navigate to 系统管理 → 系统配置 (or `/system/config`). Verify:
- Page loads without errors
- Table shows existing config entries (or empty state)
- Click "新增配置", fill in Key: `TEST_CONFIG`, Name: `测试配置`, Type: 文本, Value: `test_value`, submit
- Verify new entry appears in table
- Click "编辑" on the new entry, change value, submit
- Verify value is updated
- Click "删除" on the test entry, confirm
- Verify entry is removed

- [ ] **Step 7: E2E test — Cache invalidation for menus**

Navigate to 系统管理 → 菜单管理. Edit a menu item (change name), save. Verify:
- No errors in browser console
- The menu tree reflects the change immediately
- Check browser DevTools → Network: no 500 errors
- The left sidebar menu updates after page refresh

- [ ] **Step 8: E2E test — Cache invalidation for roles**

Navigate to 系统管理 → 角色管理. Edit a role (change name), save. Verify:
- Success toast appears
- Role list refreshes
- No console errors

- [ ] **Step 9: E2E test — Cache invalidation for role-menu assignment**

Navigate to 系统管理 → 角色管理. Click on a role, modify its menu assignment (add or remove a menu), save. Verify:
- Success toast appears
- No console errors
- After re-login (or page refresh), the menu changes are reflected in the sidebar

- [ ] **Step 10: E2E test — Cache invalidation for permissions**

Navigate to 系统管理 → 权限管理. Edit a permission, save. Verify:
- Success toast
- No console errors

- [ ] **Step 11: E2E test — Backend logs verification**

Check backend logs for event processing messages:

```bash
grep -i "处理.*变更事件" Strix/logs/latest.log | tail -20
```

Expected: Log entries like:
- `处理菜单变更事件, remote=false`
- `处理角色变更事件, roleId=xxx, remote=false`
- `处理配置变更事件, configKey=TEST_CONFIG, remote=false`

This confirms the event-driven architecture is working end-to-end.

- [ ] **Step 12: Final git status check**

```bash
cd Strix && git status && cd ../StrixPage && git status
```

Expected: Clean working trees on both projects. All changes committed.
