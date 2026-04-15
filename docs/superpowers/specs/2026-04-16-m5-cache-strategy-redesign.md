# M5: Cache Strategy Redesign — Design Specification

> **Status:** Draft  
> **Author:** Copilot + ProjectAn  
> **Date:** 2026-04-16

## Problem Statement

Strix 后端存在严重的缓存一致性问题。当管理员修改菜单、权限、角色等核心数据后，缓存不能及时刷新，导致用户看到过期数据。具体表现：

1. **SystemRoleService** 有 3 个 `@Cacheable` 但 **零个 `@CacheEvict`** — 角色删除后缓存在 Redis 中保留 30 天
2. **缓存失效逻辑散落在 Controller 层** — 违反分层原则，极易遗漏
3. **双层缓存（RAM + Redis）不一致** — `updateRedis()` 和 `updateRam()` 各自独立调用，存在竞态窗口
4. **SystemConfigCache 无更新入口** — 没有管理页面，修改数据库后 ConcurrentHashMap 永不刷新
5. **统一 30 天 TTL** — 无差异化，缓存堆积严重
6. **单实例假设** — 当前进程内 volatile 缓存在多实例部署时完全失效

## Goals

- 消除所有已知的缓存一致性 bug
- 将缓存失效逻辑从 Controller 下沉到 Service/Event 层，实现自动化
- 消除 RAM 缓存层，统一走 Redis（消除双层一致性问题）
- 支持多实例部署（通过 Redis Pub/Sub 广播缓存失效事件）
- 按业务设置差异化 TTL
- 新增系统配置管理功能（前后端 CRUD）

## Non-Goals

- 不考虑向前/向后兼容
- 不引入新的缓存中间件（如 Caffeine 本地缓存 + Redis 二级缓存）
- 不做缓存预热（启动时加载）

---

## Architecture

### Core Concept: Event-Driven Cache Invalidation

```
Service 修改数据
    │
    ├─ @Transactional 中完成 DB 操作
    └─ 发布 Spring ApplicationEvent（事务提交后）
         │
         ▼
CacheEvictionListener（@TransactionalEventListener）
    │
    ├─ 1. 调用 CacheEvictionService 的 @CacheEvict 方法
    │      （精确清除本实例的 Spring Cache）
    │
    ├─ 2. 级联刷新 LoginInfo（如涉及权限变更）
    │
    └─ 3. 发布 Redis Pub/Sub 消息
           （广播到其他 JVM 实例）
              │
              ▼
CacheInvalidationSubscriber（其他实例）
    └─ 调用同样的 CacheEvictionService @CacheEvict 方法
```

### What Gets Eliminated

| Before | After |
|--------|-------|
| `SystemMenuCache` — volatile `MenuCacheData` (RAM) + `updateRam()` + `updateRedis()` + `delLike()` | 删除 RAM 缓存，仅保留 `getIdListByParentMenu()` 改为 DB 查询 |
| `SystemPermissionCache` — volatile `List<SystemPermission>` (RAM) + 手动刷新 | 完全删除，权限查询直接走 DB |
| `SystemConfigCache` — `ConcurrentHashMap<String, String>` | 改为 `@Cacheable` + 事件驱动 |
| `SystemRegionCache` — Redis `delLike()` 封装 | 改为精确 `@CacheEvict` |
| Controller 中 `cache.updateRamAndRedis()` 调用 | Service 自动发事件，Controller 无感知 |
| `RedisUtil.delLike()` 通配符批量删除 | 精确 key 的 `@CacheEvict` 或 `allEntries=true` |

---

## Event System Design

### Event Hierarchy

```java
package cn.projectan.strix.core.event;

/**
 * 缓存失效事件基类
 * 所有缓存变更事件继承此类
 */
public abstract sealed class CacheInvalidationEvent extends ApplicationEvent
    permits MenuChangedEvent, PermissionChangedEvent, RoleChangedEvent,
            RoleMenuChangedEvent, RolePermissionChangedEvent,
            ManagerPermissionChangedEvent, ConfigChangedEvent,
            RegionChangedEvent {

    /** 事件来源实例 ID（用于 Pub/Sub 防回环） */
    private final String instanceId;

    /** 是否来自远程广播（true = 从 Pub/Sub 收到，跳过再次广播） */
    private final boolean remote;
}
```

### Event Types

| Event | Trigger | Payload | Cache Eviction |
|-------|---------|---------|----------------|
| `MenuChangedEvent` | 菜单增/改/删 | — | `strix:system:role:menu_by_rid` (allEntries), `strix:system:manager:menu_by_mid` (allEntries) |
| `PermissionChangedEvent` | 权限增/改/删 | — | `strix:system:role:permission_by_rid` (allEntries), `strix:system:manager:permission_by_mid` (allEntries) |
| `RoleChangedEvent` | 角色基本信息增/改/删 | `roleId` | `strix:system:role:select_data` (allEntries) |
| `RoleMenuChangedEvent` | 角色-菜单关联变更 | `roleId` | `strix:system:role:menu_by_rid` (key=roleId), 级联: 该角色下所有 manager 的 menu_by_mid |
| `RolePermissionChangedEvent` | 角色-权限关联变更 | `roleId` | `strix:system:role:permission_by_rid` (key=roleId), 级联: 该角色下所有 manager 的 permission_by_mid |
| `ManagerPermissionChangedEvent` | 管理员角色分配变更 | `managerId` | `strix:system:manager:menu_by_mid` (key=managerId), `strix:system:manager:permission_by_mid` (key=managerId) |
| `ConfigChangedEvent` | 系统配置变更 | `configKey` | `strix:system:config` (key=configKey) |
| `RegionChangedEvent` | 地区增/改/删 | `List<String> regionIds` | `strix:system:region:getRegionById` (key=each id), `strix:system:region:getChildrenIdList` (key=each id) |

### Event Publishing Points

在以下 Service 方法中，**事务提交后**发布事件：

| Service | Method | Event |
|---------|--------|-------|
| `SystemMenuService` | `save()`, `update()`, `deleteByIds()` | `MenuChangedEvent` + `PermissionChangedEvent` |
| `SystemPermissionService` | `save()`, `update()`, `deleteByIds()` | `PermissionChangedEvent` |
| `SystemRoleService` | `save()`, `update()`, `deleteRoleWithRelations()` | `RoleChangedEvent` |
| `SystemRoleMenuService` | 角色菜单关联变更 | `RoleMenuChangedEvent(roleId)` |
| `SystemRolePermissionService` | 角色权限关联变更 | `RolePermissionChangedEvent(roleId)` |
| `SystemManagerService` | 管理员角色分配变更 | `ManagerPermissionChangedEvent(managerId)` |
| `SystemConfigService` | `save()`, `update()`, `delete()` | `ConfigChangedEvent(key)` |
| `SystemRegionService` | `save()`, `update()`, `delete()` | `RegionChangedEvent(ids)` |

### CacheEvictionService

集中所有 `@CacheEvict` 方法到一个 Service 中，方便 Event Listener 和 Pub/Sub Subscriber 统一调用：

```java
@Service
public class CacheEvictionService {

    @CacheEvict(value = "strix:system:role:menu_by_rid", allEntries = true)
    public void evictAllRoleMenuCache() {}

    @CacheEvict(value = "strix:system:role:menu_by_rid", key = "#roleId")
    public void evictRoleMenuCache(String roleId) {}

    @CacheEvict(value = "strix:system:role:permission_by_rid", allEntries = true)
    public void evictAllRolePermissionCache() {}

    @CacheEvict(value = "strix:system:role:permission_by_rid", key = "#roleId")
    public void evictRolePermissionCache(String roleId) {}

    @CacheEvict(value = "strix:system:role:select_data", allEntries = true)
    public void evictRoleSelectCache() {}

    @CacheEvict(value = "strix:system:manager:menu_by_mid", allEntries = true)
    public void evictAllManagerMenuCache() {}

    @CacheEvict(value = "strix:system:manager:menu_by_mid", key = "#managerId")
    public void evictManagerMenuCache(String managerId) {}

    @CacheEvict(value = "strix:system:manager:permission_by_mid", allEntries = true)
    public void evictAllManagerPermissionCache() {}

    @CacheEvict(value = "strix:system:manager:permission_by_mid", key = "#managerId")
    public void evictManagerPermissionCache(String managerId) {}

    @CacheEvict(value = "strix:system:config", key = "#configKey")
    public void evictConfigCache(String configKey) {}

    @CacheEvict(value = "strix:system:config", allEntries = true)
    public void evictAllConfigCache() {}

    @CacheEvict(value = "strix:system:region:getRegionById", key = "#regionId")
    public void evictRegionByIdCache(String regionId) {}

    @CacheEvict(value = "strix:system:region:getChildrenIdList", key = "#regionId")
    public void evictRegionChildrenCache(String regionId) {}
}
```

### CacheEvictionListener

使用 `@TransactionalEventListener(phase = AFTER_COMMIT)` 确保只在事务成功提交后才执行缓存失效：

```java
@Component
@RequiredArgsConstructor
public class CacheEvictionListener {

    private final CacheEvictionService evictionService;
    private final CacheInvalidationBroadcaster broadcaster;
    private final SystemManagerService systemManagerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMenuChanged(MenuChangedEvent event) {
        evictionService.evictAllRoleMenuCache();
        evictionService.evictAllManagerMenuCache();
        if (!event.isRemote()) {
            broadcaster.broadcast(event);
        }
        // 级联刷新所有在线管理员的 LoginInfo
        // （如果需要实时生效的话）
    }

    // ... 其他事件处理方法
}
```

---

## Redis Pub/Sub 多实例广播

### Channel Design

使用单一 Redis channel `strix:cache:invalidation`，消息格式为 JSON：

```json
{
  "instanceId": "strix-node-1",
  "eventType": "MENU_CHANGED",
  "payload": {},
  "timestamp": 1713234567890
}
```

### CacheInvalidationBroadcaster

```java
@Component
@RequiredArgsConstructor
public class CacheInvalidationBroadcaster {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHANNEL = "strix:cache:invalidation";

    public void broadcast(CacheInvalidationEvent event) {
        CacheInvalidationMessage msg = CacheInvalidationMessage.from(event);
        redisTemplate.convertAndSend(CHANNEL, msg);
    }
}
```

### CacheInvalidationSubscriber

```java
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber implements MessageListener {
    private final ApplicationEventPublisher eventPublisher;
    private final String instanceId;  // 本实例唯一标识

    @Override
    public void onMessage(Message message, byte[] pattern) {
        CacheInvalidationMessage msg = deserialize(message);
        // 防回环: 忽略自己发出的消息
        if (instanceId.equals(msg.getInstanceId())) return;

        // 重新发布为 Spring Event（标记 remote=true，防止再次广播）
        CacheInvalidationEvent event = msg.toEvent(remote=true);
        eventPublisher.publishEvent(event);
    }
}
```

### Redis Configuration

注册 `MessageListenerContainer`：

```java
@Bean
public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory factory,
        CacheInvalidationSubscriber subscriber) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(factory);
    container.addMessageListener(subscriber,
            new ChannelTopic("strix:cache:invalidation"));
    return container;
}
```

---

## TTL Configuration

在 `RedisConfig.getRedisCacheConfigurationMap()` 中启用差异化 TTL：

```java
private Map<String, RedisCacheConfiguration> getRedisCacheConfigurationMap() {
    Map<String, RedisCacheConfiguration> map = new HashMap<>();

    // 字典: 7 天 (稳定数据，有版本控制)
    map.put("strix:dict:versionMap", getRedisCacheConfigurationWithTtl(7 * DAY));
    map.put("strix:dict:dictResp", getRedisCacheConfigurationWithTtl(7 * DAY));

    // 权限/菜单/角色: 1 天 (敏感数据，快速兜底)
    map.put("strix:system:role:select_data", getRedisCacheConfigurationWithTtl(DAY));
    map.put("strix:system:role:menu_by_rid", getRedisCacheConfigurationWithTtl(DAY));
    map.put("strix:system:role:permission_by_rid", getRedisCacheConfigurationWithTtl(DAY));
    map.put("strix:system:manager:menu_by_mid", getRedisCacheConfigurationWithTtl(DAY));
    map.put("strix:system:manager:permission_by_mid", getRedisCacheConfigurationWithTtl(DAY));

    // 系统配置: 1 小时 (可能频繁调整)
    map.put("strix:system:config", getRedisCacheConfigurationWithTtl(HOUR));

    // 地区: 7 天 (极少变化)
    map.put("strix:system:region:getRegionById", getRedisCacheConfigurationWithTtl(7 * DAY));
    map.put("strix:system:region:getChildrenIdList", getRedisCacheConfigurationWithTtl(7 * DAY));

    // 人气配置: 1 天
    map.put("strix:popularity:config", getRedisCacheConfigurationWithTtl(DAY));

    // 用户关联: 1 天
    map.put("strix:system:user:userRelation", getRedisCacheConfigurationWithTtl(DAY));

    return map;
}

// 默认 TTL 从 30 天改为 1 天
@Bean
public CacheManager cacheManager(RedisConnectionFactory factory) {
    return RedisCacheManager.builder(factory)
            .cacheDefaults(getRedisCacheConfigurationWithTtl(DAY))  // 1 天默认
            .withInitialCacheConfigurations(getRedisCacheConfigurationMap())
            .build();
}
```

---

## SystemConfig Management

### Backend

**New Controller:** `SystemConfigController`

```
GET  /system/config              — 分页查询配置列表
GET  /system/config/{id}         — 查询配置详情
POST /system/config/update       — 新增配置 (@Validated InsertGroup)
POST /system/config/update/{id}  — 修改配置 (@Validated UpdateGroup)
POST /system/config/remove/{id}  — 删除配置
```

**Permission:** `system:config` / `system:config:add` / `system:config:update` / `system:config:remove`

**ConfigService Changes:**
- `SystemConfigCache` 的 `ConcurrentHashMap` 改为 `@Cacheable(value = "strix:system:config", key = "#key")`
- CRUD 操作后发布 `ConfigChangedEvent`
- `@PostConstruct` 初始化改为首次访问时触发 `@Cacheable`（懒加载）

### Frontend

**New Files:**
- `api/system-config.ts` — API 模块
- `views/System/SystemConfig/SystemConfigIndex.vue` — 管理页面（标准 useCrud 模式）
- 路由注册 + 菜单 SQL

**Features:**
- 配置列表（key/value/remark）
- 新增/编辑/删除配置
- key 唯一性校验

---

## File Change Summary

### New Files (Backend ~10)

| File | Purpose |
|------|---------|
| `core/event/CacheInvalidationEvent.java` | 事件基类（sealed） |
| `core/event/MenuChangedEvent.java` | 菜单变更事件 |
| `core/event/PermissionChangedEvent.java` | 权限变更事件 |
| `core/event/RoleChangedEvent.java` | 角色变更事件 |
| `core/event/RoleMenuChangedEvent.java` | 角色菜单关联变更事件 |
| `core/event/RolePermissionChangedEvent.java` | 角色权限关联变更事件 |
| `core/event/ManagerPermissionChangedEvent.java` | 管理员权限变更事件 |
| `core/event/ConfigChangedEvent.java` | 配置变更事件 |
| `core/event/RegionChangedEvent.java` | 地区变更事件 |
| `core/event/CacheInvalidationMessage.java` | Pub/Sub 消息 DTO |
| `core/cache/CacheEvictionService.java` | 集中 @CacheEvict 方法 |
| `core/cache/CacheEvictionListener.java` | 事件监听 + 级联处理 |
| `core/cache/CacheInvalidationBroadcaster.java` | Redis Pub/Sub 发布 |
| `core/cache/CacheInvalidationSubscriber.java` | Redis Pub/Sub 订阅 |
| `controller/system/SystemConfigController.java` | 配置管理 CRUD |
| `model/request/system/config/SystemConfigUpdateReq.java` | 配置请求 DTO |

### Modified Files (Backend ~10)

| File | Change |
|------|--------|
| `config/RedisConfig.java` | 启用差异化 TTL，注册 Pub/Sub listener |
| `core/cache/system/SystemMenuCache.java` | 移除 volatile RAM 缓存，保留 `getIdListByParentMenu()` 改为 DB 查询 |
| `core/cache/system/SystemPermissionCache.java` | 完全移除（或重构为纯工具类） |
| `core/cache/system/SystemConfigCache.java` | ConcurrentHashMap → @Cacheable + 事件 |
| `core/cache/system/SystemRegionCache.java` | delLike → @CacheEvict via CacheEvictionService |
| `service/system/SystemRoleService.java` | 添加事件发布 |
| `service/system/SystemMenuService.java` | 移除手动 cache 调用，改为事件发布 |
| `service/system/SystemPermissionService.java` | 同上 |
| `service/system/SystemConfigService.java` | 添加 CRUD + 事件发布 |
| `controller/system/SystemMenuController.java` | 移除所有 cache 引用 |
| `controller/system/SystemRoleController.java` | 移除所有 cache 引用 |
| `controller/system/SystemPermissionController.java` | 移除 cache 引用 |
| `controller/system/SystemRegionController.java` | 移除 cache 引用 |

### New Files (Frontend ~2)

| File | Purpose |
|------|---------|
| `api/system-config.ts` | 配置管理 API 模块 |
| `views/System/SystemConfig/SystemConfigIndex.vue` | 配置管理页面 |

### Modified Files (Frontend ~1)

| File | Change |
|------|--------|
| `router/index.ts` | 添加配置管理路由 |

### SQL

- 系统配置管理菜单 + 权限 + 角色关联

---

## Testing Strategy

1. **单元测试** — CacheEvictionService 的 @CacheEvict 方法生效验证
2. **E2E 测试**:
   - 修改菜单 → 验证相关缓存被清除
   - 删除角色 → 验证 `select_data` 缓存不再返回已删除角色
   - 修改系统配置 → 验证缓存刷新
   - 登录后修改权限 → 验证 LoginInfo 实时更新
3. **多实例验证** — 双进程启动，一个实例修改数据，另一个验证缓存失效
