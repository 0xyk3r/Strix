# M6: SSE 权限实时刷新 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 复用现有 SSE 长连接, 在管理员修改菜单/权限后, 实时推送 `auth:refresh` 事件给已登录用户, 前端自动刷新权限和菜单, 无需重新登录.

**Architecture:** 将现有 `SseNotificationController` 重命名为 `SseController` 并调整路径为 `/sse/stream`, 使其成为通用 SSE 端点. 后端 `CacheEvictionListener` 在完成 LoginInfo 刷新后, 通过 `SseSessionManager.broadcast()` 广播 `auth:refresh` 事件. 前端新建 `stores/sse.ts` 接管 EventSource 连接, 收到 `auth:refresh` 后调用新增 API `GET /system/current-info` 获取最新权限, 更新 Pinia loginInfo 并触发菜单重载.

**Tech Stack:** Java 21 / Spring Boot 4.0.2 / SseEmitter / Redis (backend), Vue 3.5 / TypeScript / Pinia / EventSource / mitt EventBus (frontend)

**Design Pattern:** Signal-then-fetch — SSE 推送轻量信号, 前端通过加密 HTTP API 获取完整数据, 避免在未加密的 SSE 通道传输敏感权限数据.

---

## File Structure

**Backend — Modified files (2):**
- `controller/sse/SseNotificationController.java` → **Rename to** `controller/sse/SseController.java` — 通用 SSE 端点, 路径 `/sse/stream`
- `core/cache/CacheEvictionListener.java` — 注入 SseSessionManager, 权限刷新后广播 `auth:refresh`

**Backend — New file (1):**
- `controller/system/SystemController.java` — **新增方法** `GET /system/current-info` 返回当前用户的最新 `LoginManagerInfo`

**Frontend — New files (1):**
- `stores/sse.ts` — SSE 连接管理 store, 处理所有 SSE 事件分发

**Frontend — Modified files (3):**
- `stores/notification.ts` — 移除 SSE 连接代码, 仅保留通知 CRUD
- `api/auth.ts` — 新增 `currentInfo()` API 方法
- `components/notification/StrixNotification.vue` — 改为调用 SSE store

## Key Conventions

- **All controllers** must extend `BaseController` or `BaseSystemController`
- **`@Anonymous`** skips auth; **`@IgnoreEncryption`** skips SM2/SM4 encryption
- **SSE endpoint** uses `@Anonymous` + `@IgnoreEncryption` (EventSource 无法发自定义 Header)
- **EventBus**: `import { EventBus } from '@/plugins/event-bus'`; 菜单刷新: `EventBus.emit('refresh-menu')`
- **Auto-imports**: `ref`, `computed`, `watch`, `onMounted` 等 Vue API 无需手动 import
- **Axios**: `import { http } from '@/plugins/axios'`; API 路径相对于 baseURL `/api/`
- **Nginx SSE**: `location ^~ /api/sse/` 已配置, rewrite 去除 `/api` 前缀, 代理到 `http://127.0.0.1:9889/sse/...`
- **`LoginManagerInfo.permissionKeys`**: 包含 `menusKeys` + `permissionKeys` 的合集 (参见 `SystemLoginService.buildLoginResp()`)

---

### Task 1: Backend — Rename SSE Controller + Change Endpoint Path

**Files:**
- Rename: `Strix/src/main/java/cn/projectan/strix/controller/sse/SseNotificationController.java` → `SseController.java`

- [ ] **Step 1: Rename the controller file and update class/content**

Rename the file from `SseNotificationController.java` to `SseController.java`. Update the content to:

```java
package cn.projectan.strix.controller.sse;

import cn.projectan.strix.controller.BaseController;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.core.sse.SseSessionManager;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import cn.projectan.strix.service.system.NotificationReceiverService;
import cn.projectan.strix.util.common.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * SSE 通用推送端点
 * <p>
 * EventSource API 不支持自定义 Header, 因此通过 query param 传递 token,
 * 使用 @Anonymous 跳过 Spring Security 过滤器, 在方法内部手动验证 token.
 * <p>
 * 支持的事件类型:
 * - notification:new — 新通知
 * - notification:count — 未读通知数量变更
 * - auth:refresh — 权限/菜单变更, 前端应刷新 loginInfo
 *
 * @author ProjectAn
 * @since 2026-03-26
 */
@Slf4j
@RestController
@RequestMapping("sse")
@RequiredArgsConstructor
@IgnoreEncryption
@Tag(name = "SSE - 实时推送")
public class SseController extends BaseController {

    private final SseSessionManager sseSessionManager;
    private final RedisUtil redisUtil;
    private final NotificationReceiverService notificationReceiverService;

    @Anonymous
    @GetMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立 SSE 连接")
    public SseEmitter connect(@RequestParam String token) {
        // 内部验证 token
        Object loginInfo = redisUtil.get(LoginRedisKeys.MANAGER_TOKEN_PREFIX + token);
        if (!(loginInfo instanceof LoginSystemManager lsm) || lsm.getSystemManager() == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", "Unauthorized")));
            } catch (IOException ignored) {
            }
            emitter.complete();
            return emitter;
        }

        String managerId = lsm.getSystemManager().getId();
        SseEmitter emitter = sseSessionManager.createEmitter(managerId);

        // 发送初始未读数量
        try {
            long unreadCount = notificationReceiverService.getUnreadCountByReceiverId(managerId);
            emitter.send(SseEmitter.event()
                    .name("notification:count")
                    .data(Map.of("unreadCount", unreadCount)));
        } catch (IOException e) {
            log.warn("发送初始未读数量失败: managerId={}", managerId, e);
        }

        log.info("SSE 连接已建立: managerId={}", managerId);
        return emitter;
    }
}
```

Key changes from original:
- Class name: `SseNotificationController` → `SseController`
- `@RequestMapping("sse/notification")` → `@RequestMapping("sse")`
- `@GetMapping(produces = ...)` → `@GetMapping(value = "stream", produces = ...)`
- `@Tag(name = "SSE - 通知推送")` → `@Tag(name = "SSE - 实时推送")`
- Javadoc: updated to list all supported event types

- [ ] **Step 2: Delete the old file**

```bash
cd Strix && git rm src/main/java/cn/projectan/strix/controller/sse/SseNotificationController.java
```

- [ ] **Step 3: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL. The old `SseNotificationController` is only referenced by Spring component scan (no other class imports it directly).

- [ ] **Step 4: Commit**

```bash
cd Strix && git add -A && git commit -m "refactor(sse): rename SseNotificationController → SseController

- Path: /sse/notification → /sse/stream
- Generalizes the SSE endpoint for multi-event support
- Supported events: notification:new, notification:count, auth:refresh

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 2: Backend — Add `GET /system/current-info` Endpoint

**Files:**
- Modify: `Strix/src/main/java/cn/projectan/strix/controller/system/SystemController.java`

This endpoint returns the current user's `LoginManagerInfo` (with fresh `permissionKeys` from Redis). The security filter re-reads `LoginSystemManager` from Redis on every request, so after `refreshLoginInfoByManager()` updates Redis, this endpoint always returns fresh data.

- [ ] **Step 1: Add the `currentInfo` method to SystemController**

In `Strix/src/main/java/cn/projectan/strix/controller/system/SystemController.java`, add this method after the `getMenuList()` method (after line 78, before the closing `}`):

```java
    /**
     * 获取当前登录管理员信息 (含最新权限)
     * <p>
     * 安全过滤器每次请求都从 Redis 读取 LoginSystemManager, 因此在后台刷新 LoginInfo 后,
     * 本接口返回的 permissionKeys 始终是最新的.
     */
    @Operation(summary = "获取当前管理员信息")
    @GetMapping("current-info")
    public RetResult<SystemManagerLoginResp.LoginManagerInfo> currentInfo() {
        LoginSystemManager lsm = SecurityUtil.getSystemManagerLoginInfo();
        Assert.notNull(lsm, I18nUtil.get("assert.auth.notLogin"));

        var sm = lsm.getSystemManager();
        List<String> permissionKeys = new ArrayList<>();
        permissionKeys.addAll(lsm.getMenusKeys());
        permissionKeys.addAll(lsm.getPermissionKeys());

        return RetBuilder.success(
                new SystemManagerLoginResp.LoginManagerInfo(
                        sm.getId(), sm.getNickname(), sm.getType(), sm.getRegionId(), permissionKeys
                )
        );
    }
```

Also add the missing import at the top of the file (after the existing imports):

```java
import java.util.ArrayList;
```

- [ ] **Step 2: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(auth): add GET /system/current-info endpoint

- Returns fresh LoginManagerInfo with permissionKeys from Redis
- Used by frontend after receiving SSE auth:refresh signal
- Signal-then-fetch pattern: SSE signals, HTTP delivers encrypted data

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 3: Backend — Push SSE `auth:refresh` from CacheEvictionListener

**Files:**
- Modify: `Strix/src/main/java/cn/projectan/strix/core/cache/CacheEvictionListener.java`

After `CacheEvictionListener` finishes refreshing LoginInfo (via `SystemManagerService`), broadcast an `auth:refresh` SSE event to all connected managers. We use broadcast (not targeted) because:
1. It's simpler — no need to resolve which managers are affected by a role change
2. The overhead is minimal — frontends make one lightweight API call
3. If a manager's permissions didn't change, the store comparison short-circuits

- [ ] **Step 1: Add SseSessionManager injection and broadcast calls**

Replace the entire content of `Strix/src/main/java/cn/projectan/strix/core/cache/CacheEvictionListener.java` with:

```java
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
```

Key changes from original:
- Added `SseSessionManager` field injection
- Added `broadcastAuthRefresh(reason)` helper method
- Called `broadcastAuthRefresh()` after every permission-affecting refresh (6 event handlers)
- `onConfigChanged` and `onRegionChanged` do NOT broadcast `auth:refresh` (they don't affect permissions)

- [ ] **Step 2: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(sse): broadcast auth:refresh on permission/menu changes

- CacheEvictionListener injects SseSessionManager
- Broadcasts auth:refresh after LoginInfo refresh for all permission events:
  menu, permission, role, role-menu, role-permission, manager-permission
- Uses signal-then-fetch: SSE sends reason string, frontend fetches fresh data

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 4: Frontend — Create SSE Store + Refactor Notification Store

**Files:**
- Create: `StrixPage/src/stores/sse.ts`
- Modify: `StrixPage/src/stores/notification.ts`
- Modify: `StrixPage/src/api/auth.ts`
- Modify: `StrixPage/src/components/notification/StrixNotification.vue`

- [ ] **Step 1: Add `currentInfo` API method to `auth.ts`**

In `StrixPage/src/api/auth.ts`, add this method to the `authApi` object, after the `menus` method (after line 59, before the closing `}`):

```typescript
  currentInfo: () =>
    http.get<RetResult<LoginManagerInfo>>('system/current-info', {
      meta: { operate: '获取当前管理员信息', notify: false }
    })
```

- [ ] **Step 2: Create `stores/sse.ts`**

Create `StrixPage/src/stores/sse.ts`:

```typescript
import { authApi } from '@/api/auth'
import { EventBus } from '@/plugins/event-bus'
import { useLoginInfoStore } from '@/stores/login-info'
import { useNotificationStore } from '@/stores/notification'
import { defineStore } from 'pinia'

/**
 * SSE 连接管理 Store
 *
 * 管理与后端的 SSE 长连接, 分发所有 SSE 事件:
 * - notification:new — 新通知到达, 递增未读数
 * - notification:count — 未读通知数量同步
 * - auth:refresh — 权限/菜单变更, 刷新 loginInfo 和菜单
 */
export const useSseStore = defineStore('sse', () => {
  const connected = ref(false)
  let eventSource: EventSource | null = null

  function connect() {
    if (eventSource) return

    const loginInfoStore = useLoginInfoStore()
    const token = loginInfoStore.loginToken
    if (!token) {
      console.warn('SSE: 无登录 token, 无法建立连接')
      return
    }

    const url = `/api/sse/stream?token=${token}`
    eventSource = new EventSource(url)

    // ============ 通知事件 ============

    eventSource.addEventListener('notification:new', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        const notificationStore = useNotificationStore()
        notificationStore.unreadCount++
        console.log('SSE: 新通知', data.title)
      } catch (e) {
        console.error('SSE: 解析 notification:new 事件失败', e)
      }
    })

    eventSource.addEventListener('notification:count', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        const notificationStore = useNotificationStore()
        notificationStore.unreadCount = data.unreadCount
      } catch (e) {
        console.error('SSE: 解析 notification:count 事件失败', e)
      }
    })

    // ============ 权限刷新事件 ============

    eventSource.addEventListener('auth:refresh', async (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        console.log('SSE: 权限刷新信号, reason:', data.reason)
        await refreshAuth()
      } catch (e) {
        console.error('SSE: 处理 auth:refresh 事件失败', e)
      }
    })

    // ============ 错误处理 ============

    eventSource.addEventListener('error', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        if (data.message === 'Unauthorized') {
          console.error('SSE: 认证失败, 断开连接')
          disconnect()
          return
        }
      } catch {
        // 非 JSON error 事件 (如连接断开), 忽略
      }
    })

    eventSource.onopen = () => {
      connected.value = true
      console.log('SSE: 连接已建立')
    }

    eventSource.onerror = () => {
      connected.value = false
      // EventSource 内置自动重连 (服务端设置 retry: 3000ms)
    }
  }

  function disconnect() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    connected.value = false
  }

  /**
   * 从后端获取最新的 LoginManagerInfo, 更新 Pinia store 并刷新菜单
   */
  async function refreshAuth() {
    try {
      const loginInfoStore = useLoginInfoStore()
      const { data: res } = await authApi.currentInfo()
      if (res.data) {
        loginInfoStore.loginInfo = res.data
        console.log('SSE: loginInfo 已更新, permissionKeys 数量:', res.data.permissionKeys?.length ?? 0)
      }
      // 触发菜单树重载 (useHomeMenu 监听此事件)
      EventBus.emit('refresh-menu')
    } catch (e) {
      console.error('SSE: 刷新权限失败, 将在下次操作时重试', e)
    }
  }

  return {
    connected,
    connect,
    disconnect
  }
})
```

- [ ] **Step 3: Simplify `stores/notification.ts` — remove SSE connection code**

Replace the entire content of `StrixPage/src/stores/notification.ts` with:

```typescript
import { notificationApi } from '@/api/notification'
import type { ListNotificationReq } from '@/api/notification'
import type { NotificationListResp } from '@/@types/components/notification'
import { defineStore } from 'pinia'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  /**
   * 获取未读通知数量 (HTTP fallback)
   */
  async function fetchUnreadCount() {
    try {
      const { data: res } = await notificationApi.unreadCount()
      if (res.data) {
        unreadCount.value = res.data.unreadCount
      }
    } catch (error) {
      console.error('获取未读通知数量失败:', error)
    }
  }

  /**
   * 获取通知列表
   */
  async function fetchNotifications(params: ListNotificationReq) {
    try {
      const { data: res } = await notificationApi.list(params)
      return res.data as NotificationListResp
    } catch (error) {
      console.error('获取通知列表失败:', error)
      throw error
    }
  }

  /**
   * 标记单个通知为已读
   */
  async function markAsRead(notificationId: string) {
    try {
      await notificationApi.markRead(notificationId)
      // SSE store 会通过 notification:count 事件更新 unreadCount
    } catch (error) {
      console.error('标记通知为已读失败:', error)
      throw error
    }
  }

  /**
   * 标记全部通知为已读
   */
  async function markAllAsRead() {
    try {
      await notificationApi.markAllRead()
      // 本地即时反馈, SSE 也会推送 unreadCount=0
      unreadCount.value = 0
    } catch (error) {
      console.error('标记全部通知为已读失败:', error)
      throw error
    }
  }

  return {
    unreadCount,
    fetchUnreadCount,
    fetchNotifications,
    markAsRead,
    markAllAsRead
  }
})
```

Key changes:
- Removed `sseConnected`, `eventSource`, `connectSSE()`, `disconnectSSE()` — moved to `stores/sse.ts`
- Removed `useLoginInfoStore` import (no longer needed)
- `unreadCount` is now updated by the SSE store via direct property access

- [ ] **Step 4: Update `StrixNotification.vue` lifecycle hooks**

In `StrixPage/src/components/notification/StrixNotification.vue`, find and update the lifecycle hooks.

Find (around lines 352-360):
```typescript
onMounted(() => {
  notificationStore.connectSSE()
  document.addEventListener('keydown', handleKeydown, true)
})

onUnmounted(() => {
  notificationStore.disconnectSSE()
  document.removeEventListener('keydown', handleKeydown, true)
})
```

Replace with:
```typescript
const sseStore = useSseStore()

onMounted(() => {
  sseStore.connect()
  document.addEventListener('keydown', handleKeydown, true)
})

onUnmounted(() => {
  sseStore.disconnect()
  document.removeEventListener('keydown', handleKeydown, true)
})
```

Also add the import at the top of the `<script>` section (after the existing store imports):
```typescript
import { useSseStore } from '@/stores/sse'
```

- [ ] **Step 5: Check for any remaining references to removed API**

```bash
cd StrixPage && grep -rn "connectSSE\|disconnectSSE\|sseConnected\|startPolling\|stopPolling" src/ --include="*.ts" --include="*.vue"
```

Expected: No results. If any file still references removed methods, update them.

- [ ] **Step 6: Verify frontend type-checks**

```bash
cd StrixPage && pnpm type-check
```

Expected: No type errors.

- [ ] **Step 7: Commit**

```bash
cd StrixPage && git add -A && git commit -m "feat(sse): extract SSE store + add auth:refresh handler

- New stores/sse.ts: manages EventSource, dispatches all SSE events
- SSE events: notification:new, notification:count, auth:refresh
- auth:refresh handler: calls GET /system/current-info → updates loginInfo
  store → emits 'refresh-menu' via EventBus
- notification store simplified: removed SSE connection ownership
- StrixNotification.vue: uses useSseStore() for SSE lifecycle
- auth.ts: added currentInfo() API method

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 5: Build Verification + Backend Restart

**Files:** None (verification only)

- [ ] **Step 1: Run backend build**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run frontend build + lint**

```bash
cd StrixPage && pnpm build && pnpm lint
```

Expected: BUILD SUCCESSFUL, no lint errors (warnings acceptable).

- [ ] **Step 3: Restart backend**

Kill the current backend process and restart:

```bash
cd Strix && ./gradlew bootRun
```

Expected: Application starts on port 9889. Check logs for "SSE 心跳调度已启动".

- [ ] **Step 4: Smoke-test SSE endpoint path change**

```bash
curl -s -N -H "Accept: text/event-stream" "http://localhost:9889/sse/stream?token=INVALID" --max-time 3
```

Expected: Receives an `error` event with `{"message":"Unauthorized"}` and connection closes. This confirms the new endpoint path `/sse/stream` works.

- [ ] **Step 5: Verify old path returns 404**

```bash
curl -s -o /dev/null -w "%{http_code}" "http://localhost:9889/sse/notification?token=INVALID"
```

Expected: 404 (old path no longer exists).

---

### Task 6: E2E Testing — SSE Connection + Auth Refresh

**Prerequisites:** Backend running on 9889, frontend on 19889, Nginx on 13232.

- [ ] **Step 1: Login via Playwright**

Navigate to `http://localhost:13232/`, login with credentials `anjiongyi` / `Zhangyi1024!`.

- [ ] **Step 2: Verify SSE connection**

After login, check browser DevTools → Network → filter "sse". Verify:
- An EventSource connection to `/api/sse/stream?token=xxx` is established
- Response status: 200, content-type: `text/event-stream`
- Initial `notification:count` event received with `unreadCount`
- Heartbeat comments arrive every ~30 seconds

- [ ] **Step 3: Test auth:refresh — modify a menu**

Open a second tab/window, navigate to `系统管理 → 菜单管理`. Edit any menu item (e.g., change sort value). Save.

In the first tab, verify:
- The SSE connection received an `auth:refresh` event with `{"reason":"menu_changed"}`
- Console log shows "SSE: loginInfo 已更新"
- Console log shows permissionKeys count
- Menu tree automatically refreshed (check for visual changes if the menu structure changed)

- [ ] **Step 4: Test auth:refresh — modify role permissions**

Navigate to `系统管理 → 角色管理`. Edit a role's permissions (add or remove a permission). Save.

Verify:
- SSE `auth:refresh` event received with `{"reason":"role_permission_changed"}`
- loginInfo updated
- Menu refreshed

- [ ] **Step 5: Verify `v-auth` directive reacts**

If you removed a permission from the current user's role in step 4, verify that any button/element guarded by `v-auth` for that permission is now hidden/disabled. If you added a permission, verify the element becomes visible.

- [ ] **Step 6: Verify SSE reconnection**

Restart the backend:
```bash
cd Strix && ./gradlew bootRun
```

Watch the frontend console. The EventSource should:
1. Detect the connection loss (`onerror` fires, `connected` becomes `false`)
2. Automatically attempt reconnection (retry: 3000ms)
3. Successfully reconnect and log "SSE: 连接已建立"
4. Receive a new `notification:count` event on reconnection
