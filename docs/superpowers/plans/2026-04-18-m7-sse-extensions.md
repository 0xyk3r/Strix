# M7: SSE 扩展 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the SSE infrastructure with 3 new event types: `session:kicked` (force logout notification), `dict:refresh` (dictionary cache sync), and `system:announce` (system announcements with Banner/Modal display).

**Architecture:** All 3 features share the existing `SseController` (`/sse/stream`) and `SseSessionManager`. Feature 1 modifies `TokenSessionService` to push a kick event before Redis key deletion. Feature 2 creates a standalone `DictChangedEvent` (not extending sealed `CacheInvalidationEvent`) and publishes it from `DictService` CRUD methods; `CacheEvictionListener` handles SSE broadcast. Feature 3 is a full subsystem: new `sys_system_announcement` table, entity/mapper/service/controller, a global `StrixAnnouncement.vue` component mounted in `HomePage.vue`, and an admin management page.

**Tech Stack:** Java 21 / Spring Boot 4.0.2 / MyBatis Plus 3.5.16 / Redis / SseEmitter (backend), Vue 3.5 / TypeScript / Naive UI / EventSource (frontend)

**Design Spec:** `docs/superpowers/specs/2026-04-18-m7-sse-extensions-design.md`

---

## File Structure

### Backend — New files (7):

- `model/event/DictChangedEvent.java` — Dict data changed event (standalone ApplicationEvent, NOT sealed CacheInvalidationEvent)
- `model/db/system/SystemAnnouncement.java` — Announcement entity extending BaseModel
- `mapper/system/SystemAnnouncementMapper.java` — MyBatis Plus Mapper
- `service/system/SystemAnnouncementService.java` — Announcement CRUD + SSE broadcast
- `controller/system/monitor/SystemAnnouncementController.java` — Announcement management API
- `model/request/system/announcement/PublishAnnouncementReq.java` — Publish announcement request DTO
- `model/response/system/announcement/AnnouncementListResp.java` — Announcement list response DTO

### Backend — Modified files (4):

- `service/system/TokenSessionService.java` — Inject SseSessionManager, push `session:kicked` before Redis key deletion
- `service/system/DictService.java` — Inject ApplicationEventPublisher, publish DictChangedEvent in all dict data mutations
- `core/cache/CacheEvictionListener.java` — Add `onDictChanged()` handler for SSE broadcast
- `controller/sse/SseController.java` — Push active announcements on SSE connect

### Frontend — New files (3):

- `api/announcement.ts` — Announcement management API module
- `components/common/StrixAnnouncement.vue` — Global announcement display (Banner + Modal)
- `views/System/SystemMonitor/Announcement/SystemMonitorAnnouncementIndex.vue` — Admin management page

### Frontend — Modified files (4):

- `stores/sse.ts` — Add `session:kicked`, `dict:refresh`, `system:announce`, `system:announce:dismiss` event handlers + `activeAnnouncements` state
- `stores/dict.ts` — Add `refreshDictByKey()` method
- `views/System/LoginPage.vue` — Handle `r=kicked` query param with dialog instead of toast
- `views/System/HomePage.vue` — Mount `StrixAnnouncement` component
- `router/index.ts` — Add announcement management route

### Database:

- CREATE TABLE `sys_system_announcement`
- INSERT menu entry + role-menu assignment

## Key Conventions

- **ObjectMapper import:** `tools.jackson.databind.ObjectMapper` (Spring Boot 4 / Jackson 3.x)
- **All controllers** must extend `BaseController` or a subclass (`BaseSystemController`)
- **`@Anonymous`** annotation: `cn.projectan.strix.model.annotation.Anonymous`
- **CommonFlag:** `YES=1` (active), `NO=0` (terminated/deleted)
- **HTTP export:** `import { http } from '@/plugins/axios'` — named export
- **Axios baseURL:** `/api/` — API paths are relative
- **LoginPage kicked UX:** Jump to login with `r=kicked`, then show `window.$dialog.warning()` (NOT toast)
- **Existing `createStrixMessage`:** Used for toasts — `createStrixMessage(type, title, content)` from `@/utils/strix-message`
- **DictService** is where ALL dict data mutations happen (`saveDictData`, `updateDictData`, `deleteDictData`, etc.) — the event should be published there
- **SSE URL:** `/api/sse/stream?token=xxx` (browser → Nginx → `http://127.0.0.1:9889/sse/stream`)

---

### Task 1: Backend — session:kicked SSE push in TokenSessionService

**Files:**
- Modify: `Strix/src/main/java/cn/projectan/strix/service/system/TokenSessionService.java`

- [ ] **Step 1: Add SseSessionManager dependency**

In `Strix/src/main/java/cn/projectan/strix/service/system/TokenSessionService.java`, add the import and field:

Find (lines 8-12):
```java
import cn.projectan.strix.model.response.system.monitor.session.SessionMeta;
import cn.projectan.strix.util.common.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
```

Replace with:
```java
import cn.projectan.strix.core.sse.SseSessionManager;
import cn.projectan.strix.model.response.system.monitor.session.SessionMeta;
import cn.projectan.strix.util.common.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
```

Find (lines 35-36):
```java
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
```

Replace with:
```java
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final SseSessionManager sseSessionManager;
```

- [ ] **Step 2: Modify kickManagerSession to push SSE before Redis deletion**

Find (lines 86-89):
```java
    public void kickManagerSession(String managerId, String token) {
        redisUtil.del(LoginRedisKeys.MANAGER_TOKEN_PREFIX + token);
        redisUtil.hDel(LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId, token);
    }
```

Replace with:
```java
    public void kickManagerSession(String managerId, String token) {
        // 先推送 SSE 事件（token 尚未删除，SSE 连接仍存活）
        sseSessionManager.sendToManager(managerId, "session:kicked", Map.of(
                "reason", "kicked_by_admin",
                "message", "您的会话已被管理员强制下线"
        ));
        // 再删除 Redis key
        redisUtil.del(LoginRedisKeys.MANAGER_TOKEN_PREFIX + token);
        redisUtil.hDel(LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId, token);
    }
```

- [ ] **Step 3: Modify invalidateManagerSession to push SSE before Redis deletion**

Find (lines 61-66):
```java
    public void invalidateManagerSession(String managerId) {
        invalidateAllSessions(
                LoginRedisKeys.MANAGER_TOKEN_PREFIX,
                LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId
        );
    }
```

Replace with:
```java
    public void invalidateManagerSession(String managerId) {
        // 先推送 SSE 事件（token 尚未删除，SSE 连接仍存活）
        sseSessionManager.sendToManager(managerId, "session:kicked", Map.of(
                "reason", "kicked_by_admin",
                "message", "您的会话已被管理员强制下线"
        ));
        invalidateAllSessions(
                LoginRedisKeys.MANAGER_TOKEN_PREFIX,
                LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId
        );
    }
```

- [ ] **Step 4: Add Map import if not present**

The file already imports `java.util.*` (line 15), so `Map` is available. No import change needed.

- [ ] **Step 5: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(sse): push session:kicked event before Redis key deletion

- TokenSessionService: inject SseSessionManager
- kickManagerSession: SSE push before delete (single session)
- invalidateManagerSession: SSE push before delete (all sessions)
- Time-critical: SSE must fire while connection still alive

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 2: Frontend — session:kicked handler + LoginPage

**Files:**
- Modify: `StrixPage/src/stores/sse.ts`
- Modify: `StrixPage/src/views/System/LoginPage.vue`

- [ ] **Step 1: Add session:kicked event handler in sse.ts**

In `StrixPage/src/stores/sse.ts`, add the handler after the `auth:refresh` handler block.

Find (lines 63-77):
```typescript
    })

    // 服务器端错误事件 (如 Unauthorized)
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
```

Replace with:
```typescript
    })

    // 强制踢出事件
    eventSource.addEventListener('session:kicked', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        console.log('SSE: 收到强制下线事件', data.reason)
        handleKicked(data.message)
      } catch (e) {
        console.error('SSE: 解析 session:kicked 事件失败', e)
      }
    })

    // 服务器端错误事件 (如 Unauthorized)
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
```

- [ ] **Step 2: Add handleKicked function in sse.ts**

Find (lines 90-96):
```typescript
  function disconnect() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    connected.value = false
  }
```

Replace with:
```typescript
  function disconnect() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    connected.value = false
  }

  /**
   * 处理被踢出事件：断开 SSE → 清除登录信息 → 跳转登录页(带 kicked 标记)
   * 使用 window.location 而非 useRouter()，因为 SSE 事件回调不在 Vue 组件 setup 上下文中
   */
  function handleKicked(message: string) {
    disconnect()
    const loginInfoStore = useLoginInfoStore()
    loginInfoStore.clearLoginInfo()
    window.location.href = `/login?r=kicked&msg=${encodeURIComponent(message)}`
  }
```

No additional imports needed — `window.location` is globally available.

- [ ] **Step 4: Add kicked query param handling in LoginPage.vue**

In `StrixPage/src/views/System/LoginPage.vue`, find the existing `r=e` handler:

Find (lines 178-183):
```typescript
onMounted(() => {
  if (route.query.r === 'e') {
    createStrixMessage('error', '登录状态失效', '由于在其他设备上登录或凭据过期，登录状态已失效，请重新登录')
  }
  initGrid()
})
```

Replace with:
```typescript
onMounted(() => {
  if (route.query.r === 'kicked') {
    const msg = (route.query.msg as string) || '您的会话已被管理员强制下线'
    window.$dialog?.warning({
      title: '会话已终止',
      content: msg,
      positiveText: '我知道了',
      closable: false,
      maskClosable: false
    })
  } else if (route.query.r === 'e') {
    createStrixMessage('error', '登录状态失效', '由于在其他设备上登录或凭据过期，登录状态已失效，请重新登录')
  }
  initGrid()
})
```

- [ ] **Step 5: Verify frontend type-checks**

```bash
cd StrixPage && pnpm type-check
```

Expected: No errors. If `useRouter` is auto-imported and causes a duplicate import error, remove the manual import added in Step 3.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(sse): add session:kicked frontend handler

- sse.ts: listen for session:kicked, disconnect + clear auth + redirect
- LoginPage.vue: handle r=kicked with dialog warning (distinct from 401 toast)
- UX: kicked shows modal dialog, expired shows bottom toast

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 3: Backend — dict:refresh event + DictService + CacheEvictionListener

**Files:**
- Create: `Strix/src/main/java/cn/projectan/strix/model/event/DictChangedEvent.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/service/system/DictService.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/core/cache/CacheEvictionListener.java`

- [ ] **Step 1: Create DictChangedEvent.java**

This is a standalone `ApplicationEvent` (NOT extending sealed `CacheInvalidationEvent`) because dict cache is already handled via `@CacheEvict` annotations — we only need SSE broadcast.

```java
package cn.projectan.strix.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 字典数据变更事件
 * <p>
 * 独立事件类 (不继承 CacheInvalidationEvent), 因为字典缓存已通过 @CacheEvict 清除.
 * 此事件仅用于触发 SSE 广播通知前端刷新字典数据.
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Getter
public class DictChangedEvent extends ApplicationEvent {

    /** 变更的字典 key */
    private final String dictKey;

    /** 变更原因: dict_saved, data_added, data_updated, data_deleted, dict_updated, dict_deleted */
    private final String reason;

    public DictChangedEvent(Object source, String dictKey, String reason) {
        super(source);
        this.dictKey = dictKey;
        this.reason = reason;
    }
}
```

- [ ] **Step 2: Inject ApplicationEventPublisher into DictService**

In `Strix/src/main/java/cn/projectan/strix/service/system/DictService.java`:

Find (lines 27-29):
```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
```

Replace with:
```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
```

Find (line 47):
```java
    private final DictDataService dictDataService;
```

Replace with:
```java
    private final DictDataService dictDataService;
    private final ApplicationEventPublisher eventPublisher;
```

- [ ] **Step 3: Publish DictChangedEvent from saveDict**

Find (lines 120-123):
```java
    public void saveDict(Dict dict) {
        UniqueChecker.check(dict);
        Assert.isTrue(save(dict), "保存失败");
    }
```

Replace with:
```java
    public void saveDict(Dict dict) {
        UniqueChecker.check(dict);
        Assert.isTrue(save(dict), "保存失败");
        eventPublisher.publishEvent(new DictChangedEvent(this, dict.getKey(), "dict_saved"));
    }
```

- [ ] **Step 4: Publish DictChangedEvent from updateDict**

Find (lines 148-151):
```java
        UniqueChecker.check(dict);
        updateWrapper.set(Dict::getVersion, dict.getVersion() + 1);
        Assert.isTrue(update(updateWrapper), "保存失败");
    }
```

Replace with:
```java
        UniqueChecker.check(dict);
        updateWrapper.set(Dict::getVersion, dict.getVersion() + 1);
        Assert.isTrue(update(updateWrapper), "保存失败");
        String effectiveKey = StringUtils.hasText(req.getKey()) ? req.getKey() : dict.getKey();
        eventPublisher.publishEvent(new DictChangedEvent(this, effectiveKey, "dict_updated"));
    }
```

- [ ] **Step 5: Publish DictChangedEvent from deleteDict**

Find (lines 164-168):
```java
    public void deleteDict(Dict dict) {
        Assert.isTrue(dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dict.getKey())
                .remove(), "删除失败");
    }
```

Replace with:
```java
    public void deleteDict(Dict dict) {
        Assert.isTrue(dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dict.getKey())
                .remove(), "删除失败");
        eventPublisher.publishEvent(new DictChangedEvent(this, dict.getKey(), "dict_deleted"));
    }
```

- [ ] **Step 6: Publish DictChangedEvent from saveDictData**

Find (lines 182-186):
```java
    public void saveDictData(DictData dictData) {
        UniqueChecker.check(dictData);
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.save(dictData), "保存失败");
    }
```

Replace with:
```java
    public void saveDictData(DictData dictData) {
        UniqueChecker.check(dictData);
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.save(dictData), "保存失败");
        eventPublisher.publishEvent(new DictChangedEvent(this, dictData.getKey(), "data_added"));
    }
```

- [ ] **Step 7: Publish DictChangedEvent from updateDictData**

Find (lines 201-206):
```java
    public void updateDictData(DictData dictData, DictDataUpdateReq req) {
        LambdaUpdateWrapper<DictData> updateWrapper = UpdateBuilder.build(dictData, req);
        UniqueChecker.check(dictData);
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.update(updateWrapper), "保存失败");
    }
```

Replace with:
```java
    public void updateDictData(DictData dictData, DictDataUpdateReq req) {
        LambdaUpdateWrapper<DictData> updateWrapper = UpdateBuilder.build(dictData, req);
        UniqueChecker.check(dictData);
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.update(updateWrapper), "保存失败");
        eventPublisher.publishEvent(new DictChangedEvent(this, dictData.getKey(), "data_updated"));
    }
```

- [ ] **Step 8: Publish DictChangedEvent from updateDictDataById**

Find (lines 220-223):
```java
    public void updateDictDataById(DictData dictData) {
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.updateById(dictData), "保存失败");
    }
```

Replace with:
```java
    public void updateDictDataById(DictData dictData) {
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.updateById(dictData), "保存失败");
        eventPublisher.publishEvent(new DictChangedEvent(this, dictData.getKey(), "data_updated"));
    }
```

- [ ] **Step 9: Publish DictChangedEvent from deleteDictData**

Find (lines 237-244):
```java
    public void deleteDictData(DictData dictData) {
        incrementDictVersion(dictData.getKey());

        dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dictData.getKey())
                .eq(DictData::getValue, dictData.getValue())
                .remove();
    }
```

Replace with:
```java
    public void deleteDictData(DictData dictData) {
        incrementDictVersion(dictData.getKey());

        dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dictData.getKey())
                .eq(DictData::getValue, dictData.getValue())
                .remove();
        eventPublisher.publishEvent(new DictChangedEvent(this, dictData.getKey(), "data_deleted"));
    }
```

- [ ] **Step 10: Add DictChangedEvent import to DictService**

Find (line 4):
```java
import cn.projectan.strix.model.db.system.Dict;
```

Replace with:
```java
import cn.projectan.strix.model.db.system.Dict;
import cn.projectan.strix.model.event.DictChangedEvent;
```

- [ ] **Step 11: Add onDictChanged handler in CacheEvictionListener**

In `Strix/src/main/java/cn/projectan/strix/core/cache/CacheEvictionListener.java`:

Find (line 4):
```java
import cn.projectan.strix.model.event.cache.*;
```

Replace with:
```java
import cn.projectan.strix.model.event.DictChangedEvent;
import cn.projectan.strix.model.event.cache.*;
```

Find (lines 138-146):
```java
    }

    /**
     * 通过 SSE 广播 auth:refresh 事件, 通知所有已连接的前端刷新权限
     */
    private void broadcastAuthRefresh(String reason) {
        sseSessionManager.broadcast("auth:refresh", Map.of("reason", reason));
        log.debug("SSE 广播 auth:refresh, reason={}", reason);
    }
```

Replace with:
```java
    }

    /**
     * 字典数据变更 → SSE 广播 dict:refresh, 前端立即拉取最新数据
     * <p>
     * 字典缓存已通过 DictService 的 @CacheEvict 清除, 此处仅负责 SSE 广播.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDictChanged(DictChangedEvent event) {
        log.info("处理字典变更事件, dictKey={}, reason={}", event.getDictKey(), event.getReason());
        sseSessionManager.broadcast("dict:refresh", Map.of(
                "dictKey", event.getDictKey(),
                "reason", event.getReason()
        ));
    }

    /**
     * 通过 SSE 广播 auth:refresh 事件, 通知所有已连接的前端刷新权限
     */
    private void broadcastAuthRefresh(String reason) {
        sseSessionManager.broadcast("auth:refresh", Map.of("reason", reason));
        log.debug("SSE 广播 auth:refresh, reason={}", reason);
    }
```

- [ ] **Step 12: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 13: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(sse): add dict:refresh SSE broadcast on dictionary changes

- DictChangedEvent: standalone ApplicationEvent with dictKey + reason
- DictService: publish event from all 7 dict mutation methods
- CacheEvictionListener: onDictChanged handler broadcasts dict:refresh via SSE
- Dict cache already cleared by @CacheEvict — event is SSE-only

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 4: Frontend — dict:refresh handler + dict store

**Files:**
- Modify: `StrixPage/src/stores/dict.ts`
- Modify: `StrixPage/src/stores/sse.ts`

- [ ] **Step 1: Add refreshDictByKey method to dict store**

In `StrixPage/src/stores/dict.ts`:

Find (lines 93-99):
```typescript
    return {
      versionMap,
      dictMap,
      refreshVersion,
      getDictData
    }
```

Replace with:
```typescript
    /**
     * SSE 触发的字典刷新：跳过缓存校验，直接拉取最新数据覆盖写入
     */
    async function refreshDictByKey(key: string): Promise<void> {
      await fetchDictData(key)
      console.log('Dict: 已刷新字典数据, key=', key)
    }

    return {
      versionMap,
      dictMap,
      refreshVersion,
      getDictData,
      refreshDictByKey
    }
```

- [ ] **Step 2: Add dict:refresh event handler in sse.ts**

In `StrixPage/src/stores/sse.ts`, add the handler after the `session:kicked` handler.

Find the `session:kicked` handler we added in Task 2:
```typescript
    // 强制踢出事件
    eventSource.addEventListener('session:kicked', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        console.log('SSE: 收到强制下线事件', data.reason)
        handleKicked(data.message)
      } catch (e) {
        console.error('SSE: 解析 session:kicked 事件失败', e)
      }
    })

    // 服务器端错误事件 (如 Unauthorized)
```

Replace with:
```typescript
    // 强制踢出事件
    eventSource.addEventListener('session:kicked', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        console.log('SSE: 收到强制下线事件', data.reason)
        handleKicked(data.message)
      } catch (e) {
        console.error('SSE: 解析 session:kicked 事件失败', e)
      }
    })

    // 字典刷新事件
    eventSource.addEventListener('dict:refresh', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        console.log('SSE: 收到字典刷新事件, dictKey=', data.dictKey)
        const dictStore = useDictStore()
        dictStore.refreshDictByKey(data.dictKey)
      } catch (e) {
        console.error('SSE: 解析 dict:refresh 事件失败', e)
      }
    })

    // 服务器端错误事件 (如 Unauthorized)
```

- [ ] **Step 3: Add useDictStore import in sse.ts**

Find (line 4):
```typescript
import { useNotificationStore } from '@/stores/notification'
```

Replace with:
```typescript
import { useDictStore } from '@/stores/dict'
import { useNotificationStore } from '@/stores/notification'
```

- [ ] **Step 4: Verify frontend type-checks**

```bash
cd StrixPage && pnpm type-check
```

Expected: No errors.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(sse): add dict:refresh frontend handler

- dict.ts: add refreshDictByKey() — skips cache validation, direct fetch
- sse.ts: listen for dict:refresh, call dictStore.refreshDictByKey(key)
- pendingMap dedup still prevents concurrent fetches

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 5: Backend — SystemAnnouncement entity + mapper + DB table

**Files:**
- Create: `Strix/src/main/java/cn/projectan/strix/model/db/system/SystemAnnouncement.java`
- Create: `Strix/src/main/java/cn/projectan/strix/mapper/system/SystemAnnouncementMapper.java`
- Create: Mapper XML (empty, per convention)

- [ ] **Step 1: Create SystemAnnouncement.java entity**

```java
package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 系统公告
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_system_announcement")
public class SystemAnnouncement extends BaseModel<SystemAnnouncement> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 级别: INFO / WARNING / URGENT
     */
    @TableField("`level`")
    private String level;

    /**
     * 展示方式: BANNER / MODAL
     */
    private String displayType;

    /**
     * 公告状态 (0=已终止, 1=有效)
     */
    @TableField("`status`")
    private Short status;

    /**
     * 生效时间 (null = 立即生效)
     */
    private LocalDateTime startTime;

    /**
     * 失效时间 (null = 不自动失效)
     */
    private LocalDateTime endTime;

    /**
     * 终止人 ID
     */
    private String endBy;

    /**
     * 终止原因
     */
    private String endReason;
}
```

- [ ] **Step 2: Create SystemAnnouncementMapper.java**

```java
package cn.projectan.strix.mapper.system;

import cn.projectan.strix.model.db.system.SystemAnnouncement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统公告 Mapper
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Mapper
public interface SystemAnnouncementMapper extends BaseMapper<SystemAnnouncement> {
}
```

- [ ] **Step 3: Create empty Mapper XML**

Create file at `Strix/src/main/resources/mapper/system/SystemAnnouncementMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="cn.projectan.strix.mapper.system.SystemAnnouncementMapper">
</mapper>
```

- [ ] **Step 4: Create database table**

Execute the following SQL:

```sql
CREATE TABLE IF NOT EXISTS strix.sys_system_announcement (
    id              VARCHAR(32)  NOT NULL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL COMMENT '公告标题',
    content         TEXT         NULL     COMMENT '公告内容',
    `level`         VARCHAR(20)  NOT NULL DEFAULT 'INFO' COMMENT '级别: INFO / WARNING / URGENT',
    display_type    VARCHAR(20)  NOT NULL DEFAULT 'BANNER' COMMENT '展示方式: BANNER / MODAL',
    `status`        SMALLINT     NOT NULL DEFAULT 1 COMMENT '1=有效 0=已终止',
    start_time      DATETIME     NULL     COMMENT '生效时间 (null=立即生效)',
    end_time        DATETIME     NULL     COMMENT '失效时间 (null=不自动失效)',
    end_by          VARCHAR(32)  NULL     COMMENT '终止人 ID',
    end_reason      VARCHAR(200) NULL     COMMENT '终止原因',
    deleted_status  SMALLINT     NOT NULL DEFAULT 0 COMMENT '0=正常 1=删除',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type SMALLINT     NULL,
    created_by      VARCHAR(32)  NULL,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by_type SMALLINT     NULL,
    updated_by      VARCHAR(32)  NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告';
```

- [ ] **Step 5: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(announce): add SystemAnnouncement entity, mapper, DB table

- Entity: title, content, level, displayType, status, startTime, endTime
- Mapper: extends BaseMapper<SystemAnnouncement>
- SQL: CREATE TABLE sys_system_announcement

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 6: Backend — SystemAnnouncementService + DTOs

**Files:**
- Create: `Strix/src/main/java/cn/projectan/strix/model/request/system/announcement/PublishAnnouncementReq.java`
- Create: `Strix/src/main/java/cn/projectan/strix/model/response/system/announcement/AnnouncementListResp.java`
- Create: `Strix/src/main/java/cn/projectan/strix/service/system/SystemAnnouncementService.java`

- [ ] **Step 1: Create PublishAnnouncementReq.java**

```java
package cn.projectan.strix.model.request.system.announcement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布公告请求
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Schema(description = "发布公告请求")
@Data
public class PublishAnnouncementReq {

    @Schema(description = "公告标题")
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过200字")
    private String title;

    @Schema(description = "公告内容")
    @NotBlank(message = "公告内容不能为空")
    private String content;

    @Schema(description = "级别: INFO / WARNING / URGENT")
    @NotBlank(message = "公告级别不能为空")
    private String level;

    @Schema(description = "展示方式: BANNER / MODAL")
    @NotBlank(message = "展示方式不能为空")
    private String displayType;

    @Schema(description = "生效时间 (null = 立即生效)")
    private LocalDateTime startTime;

    @Schema(description = "失效时间 (null = 不自动失效)")
    private LocalDateTime endTime;
}
```

- [ ] **Step 2: Create AnnouncementListResp.java**

```java
package cn.projectan.strix.model.response.system.announcement;

import cn.projectan.strix.model.db.system.SystemAnnouncement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告管理列表响应
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Schema(description = "公告管理列表响应")
@Data
@NoArgsConstructor
public class AnnouncementListResp {

    @Schema(description = "公告列表")
    private List<AnnouncementItem> items;

    @Schema(description = "分页总数")
    private long total;

    @Schema(description = "总公告数")
    private long totalCount;

    @Schema(description = "活跃公告数")
    private long activeCount;

    @Schema(description = "已终止公告数")
    private long terminatedCount;

    @Schema(description = "公告列表项")
    @Data
    @NoArgsConstructor
    public static class AnnouncementItem {

        @Schema(description = "公告 ID")
        private String id;

        @Schema(description = "公告标题")
        private String title;

        @Schema(description = "级别")
        private String level;

        @Schema(description = "展示方式")
        private String displayType;

        @Schema(description = "状态: 1=有效, 0=已终止")
        private Short status;

        @Schema(description = "生效时间")
        private LocalDateTime startTime;

        @Schema(description = "失效时间")
        private LocalDateTime endTime;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

        @Schema(description = "终止原因")
        private String endReason;

        public AnnouncementItem(SystemAnnouncement a) {
            this.id = a.getId();
            this.title = a.getTitle();
            this.level = a.getLevel();
            this.displayType = a.getDisplayType();
            this.status = a.getStatus();
            this.startTime = a.getStartTime();
            this.endTime = a.getEndTime();
            this.createdTime = a.getCreatedTime();
            this.endReason = a.getEndReason();
        }
    }
}
```

- [ ] **Step 3: Create SystemAnnouncementService.java**

```java
package cn.projectan.strix.service.system;

import cn.projectan.strix.core.sse.SseSessionManager;
import cn.projectan.strix.mapper.system.SystemAnnouncementMapper;
import cn.projectan.strix.model.db.system.SystemAnnouncement;
import cn.projectan.strix.model.dict.common.CommonFlag;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.request.system.announcement.PublishAnnouncementReq;
import cn.projectan.strix.model.response.system.announcement.AnnouncementListResp;
import cn.projectan.strix.model.response.system.announcement.AnnouncementListResp.AnnouncementItem;
import cn.projectan.strix.util.common.I18nUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统公告服务
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemAnnouncementService extends ServiceImpl<SystemAnnouncementMapper, SystemAnnouncement> {

    private final SseSessionManager sseSessionManager;

    /**
     * 发布公告
     */
    public void publish(PublishAnnouncementReq req) {
        SystemAnnouncement announcement = new SystemAnnouncement()
                .setTitle(req.getTitle())
                .setContent(req.getContent())
                .setLevel(req.getLevel())
                .setDisplayType(req.getDisplayType())
                .setStatus(CommonFlag.YES)
                .setStartTime(req.getStartTime())
                .setEndTime(req.getEndTime());

        Assert.isTrue(save(announcement), "发布公告失败");
        log.info("公告已发布: id={}, title={}, level={}", announcement.getId(), announcement.getTitle(), announcement.getLevel());

        // 仅当公告当前有效时才立即推送
        if (isCurrentlyActive(announcement)) {
            sseSessionManager.broadcast("system:announce", buildAnnouncementData(announcement));
        }
    }

    /**
     * 终止公告
     */
    public void terminate(String id, String operatorId, String reason) {
        SystemAnnouncement announcement = getById(id);
        Assert.notNull(announcement, I18nUtil.notFound("field.announcement"));

        announcement.setStatus(CommonFlag.NO)
                .setEndBy(operatorId)
                .setEndReason(reason);
        Assert.isTrue(updateById(announcement), "终止公告失败");

        // SSE 广播公告终止
        sseSessionManager.broadcast("system:announce:dismiss", Map.of("id", id));
        log.info("公告已终止: id={}, reason={}", id, reason);
    }

    /**
     * 获取当前所有活跃公告 (供 SSE 连接时初始推送)
     */
    public List<SystemAnnouncement> getActiveAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        return lambdaQuery()
                .eq(SystemAnnouncement::getStatus, CommonFlag.YES)
                .and(w -> w
                        .isNull(SystemAnnouncement::getStartTime)
                        .or()
                        .le(SystemAnnouncement::getStartTime, now))
                .and(w -> w
                        .isNull(SystemAnnouncement::getEndTime)
                        .or()
                        .gt(SystemAnnouncement::getEndTime, now))
                .orderByDesc(SystemAnnouncement::getCreatedTime)
                .list();
    }

    /**
     * 获取公告管理列表
     */
    public AnnouncementListResp getManageList(BasePageReq<SystemAnnouncement> req, String keyword, Short status, String level) {
        Page<SystemAnnouncement> page = lambdaQuery()
                .like(StringUtils.hasText(keyword), SystemAnnouncement::getTitle, keyword)
                .eq(status != null, SystemAnnouncement::getStatus, status)
                .eq(StringUtils.hasText(level), SystemAnnouncement::getLevel, level)
                .orderByDesc(SystemAnnouncement::getCreatedTime)
                .page(req.getPage());

        List<AnnouncementItem> items = page.getRecords().stream()
                .map(AnnouncementItem::new)
                .toList();

        long totalCount = count();
        long activeCount = lambdaQuery().eq(SystemAnnouncement::getStatus, CommonFlag.YES).count();

        AnnouncementListResp resp = new AnnouncementListResp();
        resp.setItems(items);
        resp.setTotal(page.getTotal());
        resp.setTotalCount(totalCount);
        resp.setActiveCount(activeCount);
        resp.setTerminatedCount(totalCount - activeCount);
        return resp;
    }

    /**
     * 获取公告详情
     */
    public SystemAnnouncement getDetail(String id) {
        SystemAnnouncement announcement = getById(id);
        Assert.notNull(announcement, I18nUtil.notFound("field.announcement"));
        return announcement;
    }

    private boolean isCurrentlyActive(SystemAnnouncement a) {
        LocalDateTime now = LocalDateTime.now();
        boolean started = a.getStartTime() == null || !a.getStartTime().isAfter(now);
        boolean notExpired = a.getEndTime() == null || a.getEndTime().isAfter(now);
        return a.getStatus() == CommonFlag.YES && started && notExpired;
    }

    private Map<String, Object> buildAnnouncementData(SystemAnnouncement a) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", a.getId());
        data.put("title", a.getTitle());
        data.put("content", Optional.ofNullable(a.getContent()).orElse(""));
        data.put("level", a.getLevel());
        data.put("displayType", a.getDisplayType());
        data.put("startTime", a.getStartTime() != null ? a.getStartTime().toString() : null);
        data.put("endTime", a.getEndTime() != null ? a.getEndTime().toString() : null);
        return data;
    }
}
```

- [ ] **Step 4: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(announce): add announcement service + DTOs

- PublishAnnouncementReq: title, content, level, displayType, start/endTime
- AnnouncementListResp: paginated list with stats
- SystemAnnouncementService: publish, terminate, getActiveAnnouncements, getManageList
- SSE broadcast on publish (system:announce) and terminate (system:announce:dismiss)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 7: Backend — SystemAnnouncementController + SseController integration

**Files:**
- Create: `Strix/src/main/java/cn/projectan/strix/controller/system/monitor/SystemAnnouncementController.java`
- Modify: `Strix/src/main/java/cn/projectan/strix/controller/sse/SseController.java`

- [ ] **Step 1: Create SystemAnnouncementController.java**

```java
package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.system.SystemAnnouncement;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.request.system.announcement.PublishAnnouncementReq;
import cn.projectan.strix.model.response.system.announcement.AnnouncementListResp;
import cn.projectan.strix.service.system.SystemAnnouncementService;
import cn.projectan.strix.util.system.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统公告管理
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/announcement")
@RequiredArgsConstructor
@Tag(name = "系统监控 - 系统公告")
public class SystemAnnouncementController extends BaseSystemController {

    private final SystemAnnouncementService systemAnnouncementService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement')")
    @Operation(summary = "公告管理列表")
    public RetResult<AnnouncementListResp> list(
            BasePageReq<SystemAnnouncement> pageReq,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) String level) {
        return RetBuilder.success(systemAnnouncementService.getManageList(pageReq, keyword, status, level));
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement')")
    @Operation(summary = "公告详情")
    public RetResult<SystemAnnouncement> detail(@PathVariable String id) {
        return RetBuilder.success(systemAnnouncementService.getDetail(id));
    }

    @PostMapping("publish")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement:publish')")
    @Operation(summary = "发布公告")
    public RetResult<Object> publish(@RequestBody @Valid PublishAnnouncementReq req) {
        systemAnnouncementService.publish(req);
        return RetBuilder.success();
    }

    @PostMapping("{id}/terminate")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement:terminate')")
    @Operation(summary = "终止公告")
    public RetResult<Object> terminate(@PathVariable String id, @RequestBody(required = false) TerminateReq req) {
        String operatorId = SecurityUtil.getOperatorId();
        String reason = req != null ? req.reason() : "管理员手动终止";
        systemAnnouncementService.terminate(id, operatorId, reason);
        return RetBuilder.success();
    }

    @PostMapping("batch-terminate")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement:terminate')")
    @Operation(summary = "批量终止公告")
    public RetResult<Object> batchTerminate(@RequestBody BatchTerminateReq req) {
        String operatorId = SecurityUtil.getOperatorId();
        for (String id : req.ids()) {
            systemAnnouncementService.terminate(id, operatorId, "批量终止");
        }
        return RetBuilder.success();
    }

    record TerminateReq(String reason) {}
    record BatchTerminateReq(List<String> ids) {}
}
```

- [ ] **Step 2: Add initial announcement push in SseController.connect()**

In `Strix/src/main/java/cn/projectan/strix/controller/sse/SseController.java`:

Find (line 10):
```java
import cn.projectan.strix.service.system.NotificationReceiverService;
```

Replace with:
```java
import cn.projectan.strix.model.db.system.SystemAnnouncement;
import cn.projectan.strix.service.system.NotificationReceiverService;
import cn.projectan.strix.service.system.SystemAnnouncementService;
```

Find (lines 48-49):
```java
    private final RedisUtil redisUtil;
    private final NotificationReceiverService notificationReceiverService;
```

Replace with:
```java
    private final RedisUtil redisUtil;
    private final NotificationReceiverService notificationReceiverService;
    private final SystemAnnouncementService systemAnnouncementService;
```

Find (lines 78-81):
```java
        } catch (IOException e) {
            log.warn("发送初始未读数量失败: managerId={}", managerId, e);
        }

        log.info("SSE 连接已建立: managerId={}", managerId);
```

Replace with:
```java
        } catch (IOException e) {
            log.warn("发送初始未读数量失败: managerId={}", managerId, e);
        }

        // 发送所有活跃公告
        try {
            List<SystemAnnouncement> activeAnnouncements = systemAnnouncementService.getActiveAnnouncements();
            for (SystemAnnouncement a : activeAnnouncements) {
                emitter.send(SseEmitter.event()
                        .name("system:announce")
                        .data(Map.of(
                                "id", a.getId(),
                                "title", a.getTitle(),
                                "content", a.getContent() != null ? a.getContent() : "",
                                "level", a.getLevel(),
                                "displayType", a.getDisplayType(),
                                "startTime", a.getStartTime() != null ? a.getStartTime().toString() : "",
                                "endTime", a.getEndTime() != null ? a.getEndTime().toString() : ""
                        )));
            }
        } catch (IOException e) {
            log.warn("发送活跃公告失败: managerId={}", managerId, e);
        }

        log.info("SSE 连接已建立: managerId={}", managerId);
```

Add `List` import if not present. The file already imports `java.util.Map` — check for `java.util.List`:

Find (line 22):
```java
import java.util.Map;
```

Replace with:
```java
import java.util.List;
import java.util.Map;
```

- [ ] **Step 3: Verify backend compiles**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
cd Strix && git add -A && git commit -m "feat(announce): add announcement controller + SSE initial push

- SystemAnnouncementController: list, detail, publish, terminate, batch-terminate
- SseController: push all active announcements on SSE connect
- Permission: system:monitor:announcement[:publish|:terminate]

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 8: Frontend — announcement API module + SSE handlers

**Files:**
- Create: `StrixPage/src/api/announcement.ts`
- Modify: `StrixPage/src/stores/sse.ts`

- [ ] **Step 1: Create announcement.ts API module**

```typescript
import type { RetResult } from './types'
import { http } from '@/plugins/axios'

const BASE = 'system/monitor/announcement'

/** 公告列表项 */
export interface AnnouncementItem {
  id: string
  title: string
  level: string
  displayType: string
  status: number
  startTime: string | null
  endTime: string | null
  createdTime: string
  endReason: string | null
}

/** 公告列表响应 */
export interface AnnouncementListResp {
  items: AnnouncementItem[]
  total: number
  totalCount: number
  activeCount: number
  terminatedCount: number
}

/** 公告详情 */
export interface AnnouncementDetail {
  id: string
  title: string
  content: string
  level: string
  displayType: string
  status: number
  startTime: string | null
  endTime: string | null
  createdTime: string
  endBy: string | null
  endReason: string | null
}

/** 发布公告请求 */
export interface PublishAnnouncementReq {
  title: string
  content: string
  level: string
  displayType: string
  startTime?: string | null
  endTime?: string | null
}

/** SSE 推送的公告数据 */
export interface SseAnnouncement {
  id: string
  title: string
  content: string
  level: string
  displayType: string
  startTime: string | null
  endTime: string | null
}

export const announcementApi = {
  list: (params: { pageSize?: number; pageIndex?: number; keyword?: string; status?: number | null; level?: string | null }) =>
    http.get<RetResult<AnnouncementListResp>>(BASE, {
      params,
      meta: { operate: '加载公告列表' }
    }),

  detail: (id: string) =>
    http.get<RetResult<AnnouncementDetail>>(`${BASE}/${id}`, {
      meta: { operate: '加载公告详情' }
    }),

  publish: (data: PublishAnnouncementReq) =>
    http.post<RetResult<object>>(`${BASE}/publish`, data, {
      meta: { operate: '发布公告', notify: true }
    }),

  terminate: (id: string, reason?: string) =>
    http.post<RetResult<object>>(`${BASE}/${id}/terminate`, { reason }, {
      meta: { operate: '终止公告', notify: true }
    }),

  batchTerminate: (ids: string[]) =>
    http.post<RetResult<object>>(`${BASE}/batch-terminate`, { ids }, {
      meta: { operate: '批量终止公告', notify: true }
    })
}
```

- [ ] **Step 2: Add announcement SSE event handlers + state in sse.ts**

In `StrixPage/src/stores/sse.ts`:

Add import at the top:

Find:
```typescript
import { useDictStore } from '@/stores/dict'
```

Replace with:
```typescript
import type { SseAnnouncement } from '@/api/announcement'
import { useDictStore } from '@/stores/dict'
```

Add `activeAnnouncements` state:

Find:
```typescript
  const connected = ref(false)
  let eventSource: EventSource | null = null
```

Replace with:
```typescript
  const connected = ref(false)
  const activeAnnouncements = ref<SseAnnouncement[]>([])
  let eventSource: EventSource | null = null
```

Add announcement event handlers after the `dict:refresh` handler:

Find:
```typescript
    // 字典刷新事件
    eventSource.addEventListener('dict:refresh', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        console.log('SSE: 收到字典刷新事件, dictKey=', data.dictKey)
        const dictStore = useDictStore()
        dictStore.refreshDictByKey(data.dictKey)
      } catch (e) {
        console.error('SSE: 解析 dict:refresh 事件失败', e)
      }
    })

    // 服务器端错误事件 (如 Unauthorized)
```

Replace with:
```typescript
    // 字典刷新事件
    eventSource.addEventListener('dict:refresh', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        console.log('SSE: 收到字典刷新事件, dictKey=', data.dictKey)
        const dictStore = useDictStore()
        dictStore.refreshDictByKey(data.dictKey)
      } catch (e) {
        console.error('SSE: 解析 dict:refresh 事件失败', e)
      }
    })

    // 系统公告推送
    eventSource.addEventListener('system:announce', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data) as SseAnnouncement
        console.log('SSE: 收到系统公告', data.title)
        const idx = activeAnnouncements.value.findIndex((a) => a.id === data.id)
        if (idx >= 0) {
          activeAnnouncements.value[idx] = data
        } else {
          activeAnnouncements.value.push(data)
        }
      } catch (e) {
        console.error('SSE: 解析 system:announce 事件失败', e)
      }
    })

    // 系统公告撤除
    eventSource.addEventListener('system:announce:dismiss', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        console.log('SSE: 收到公告撤除', data.id)
        activeAnnouncements.value = activeAnnouncements.value.filter((a) => a.id !== data.id)
      } catch (e) {
        console.error('SSE: 解析 system:announce:dismiss 事件失败', e)
      }
    })

    // 服务器端错误事件 (如 Unauthorized)
```

Clear announcements on disconnect:

Find:
```typescript
  function disconnect() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    connected.value = false
  }
```

Replace with:
```typescript
  function disconnect() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    connected.value = false
    activeAnnouncements.value = []
  }
```

Export `activeAnnouncements`:

Find:
```typescript
  return {
    connected,
    connect,
    disconnect
  }
```

Replace with:
```typescript
  return {
    connected,
    activeAnnouncements,
    connect,
    disconnect
  }
```

- [ ] **Step 3: Verify frontend type-checks**

```bash
cd StrixPage && pnpm type-check
```

Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(announce): add announcement API module + SSE handlers

- announcement.ts: list, detail, publish, terminate, batchTerminate
- sse.ts: system:announce (add/update) + system:announce:dismiss (remove)
- sse.ts: activeAnnouncements reactive state, cleared on disconnect

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 9: Frontend — StrixAnnouncement.vue global component + HomePage mount

**Files:**
- Create: `StrixPage/src/components/common/StrixAnnouncement.vue`
- Modify: `StrixPage/src/views/System/HomePage.vue`

- [ ] **Step 1: Create StrixAnnouncement.vue**

```vue
<template>
  <!-- Banner 公告 -->
  <div v-if="visibleBanners.length > 0" class="strix-announce-banners">
    <div
      v-for="announcement in visibleBanners"
      :key="announcement.id"
      :class="['strix-announce-banner', `strix-announce-banner--${announcement.level.toLowerCase()}`]"
    >
      <div class="strix-announce-banner__icon">
        <strix-icon :icon="levelIcon(announcement.level)" :size="16" />
      </div>
      <div class="strix-announce-banner__content">
        <span class="strix-announce-banner__title">{{ announcement.title }}</span>
        <span v-if="announcement.endTime" class="strix-announce-banner__countdown">
          {{ formatCountdown(announcement.endTime) }}
        </span>
      </div>
      <button
        v-if="announcement.level !== 'URGENT'"
        class="strix-announce-banner__close"
        @click="dismissBanner(announcement.id)"
      >
        <strix-icon icon="x" :size="14" />
      </button>
    </div>
  </div>

  <!-- Modal 公告 (URGENT + MODAL) -->
  <n-modal
    v-model:show="showUrgentModal"
    preset="card"
    :title="urgentModalData?.title ?? '紧急公告'"
    style="width: 520px"
    :closable="false"
    :mask-closable="false"
  >
    <div v-if="urgentModalData" class="strix-announce-modal">
      <n-tag type="error" size="small" :bordered="false" style="margin-bottom: 12px">
        {{ urgentModalData.level }}
      </n-tag>
      <div class="strix-announce-modal__content">{{ urgentModalData.content }}</div>
      <div v-if="urgentModalData.endTime" class="strix-announce-modal__time">
        失效时间: {{ formatTime(urgentModalData.endTime) }}
      </div>
    </div>
    <template #footer>
      <n-flex justify="end">
        <n-button type="primary" @click="acknowledgeModal">我已知悉</n-button>
      </n-flex>
    </template>
  </n-modal>
</template>

<script lang="ts" setup>
import type { SseAnnouncement } from '@/api/announcement'
import StrixIcon from '@/components/icon/StrixIcon.vue'
import { useSseStore } from '@/stores/sse'

const sseStore = useSseStore()

// 已关闭的 Banner ID 集合 (sessionStorage, 刷新后重新显示)
const dismissedIds = ref<Set<string>>(new Set())

// 从 sessionStorage 恢复
onMounted(() => {
  try {
    const stored = sessionStorage.getItem('strix-dismissed-announcements')
    if (stored) {
      dismissedIds.value = new Set(JSON.parse(stored))
    }
  } catch {
    // ignore
  }
})

// Banner 过滤：排除已关闭的 + 仅 BANNER 类型
const visibleBanners = computed(() => {
  return sseStore.activeAnnouncements.filter(
    (a) => a.displayType === 'BANNER' && !dismissedIds.value.has(a.id)
  )
})

// Modal 队列：MODAL 类型 + 未确认
const pendingModals = computed(() => {
  return sseStore.activeAnnouncements.filter(
    (a) => a.displayType === 'MODAL' && !dismissedIds.value.has(a.id)
  )
})

// 当前显示的 Modal
const showUrgentModal = ref(false)
const urgentModalData = ref<SseAnnouncement | null>(null)

// 监听待显示的 Modal 队列
watch(
  pendingModals,
  (modals) => {
    if (modals.length > 0 && !showUrgentModal.value) {
      urgentModalData.value = modals[0]
      showUrgentModal.value = true
    }
  },
  { immediate: true }
)

function dismissBanner(id: string) {
  dismissedIds.value.add(id)
  saveDismissed()
}

function acknowledgeModal() {
  if (urgentModalData.value) {
    dismissedIds.value.add(urgentModalData.value.id)
    saveDismissed()
  }
  showUrgentModal.value = false
  urgentModalData.value = null
  // 检查是否还有待显示的 Modal
  nextTick(() => {
    if (pendingModals.value.length > 0) {
      urgentModalData.value = pendingModals.value[0]
      showUrgentModal.value = true
    }
  })
}

function saveDismissed() {
  sessionStorage.setItem('strix-dismissed-announcements', JSON.stringify([...dismissedIds.value]))
}

function levelIcon(level: string): string {
  switch (level) {
    case 'WARNING':
      return 'alert-triangle'
    case 'URGENT':
      return 'alert-octagon'
    default:
      return 'info'
  }
}

function formatCountdown(endTime: string): string {
  if (!endTime) return ''
  const end = new Date(endTime).getTime()
  const now = Date.now()
  const diff = end - now
  if (diff <= 0) return '已结束'
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  if (hours > 0) return `剩余 ${hours}h${minutes}m`
  return `剩余 ${minutes}m`
}

function formatTime(time: string): string {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}
</script>

<style lang="scss" scoped>
.strix-announce-banners {
  position: relative;
  z-index: 100;
  width: 100%;
}

.strix-announce-banner {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  font-size: 13px;
  line-height: 1.5;

  &--info {
    color: #1a6fb5;
    background: #e8f4fd;
  }

  &--warning {
    color: #b57a1a;
    background: #fef3e2;
  }

  &--urgent {
    color: #b51a1a;
    background: #fde8e8;

    .strix-announce-banner__title {
      font-weight: 600;
      animation: pulse-text 2s ease-in-out infinite;
    }
  }

  &__icon {
    flex-shrink: 0;
    margin-right: 8px;
    display: flex;
    align-items: center;
  }

  &__content {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__title {
    font-weight: 500;
  }

  &__countdown {
    font-size: 12px;
    opacity: 0.8;
  }

  &__close {
    flex-shrink: 0;
    margin-left: 8px;
    padding: 2px;
    border: none;
    background: transparent;
    cursor: pointer;
    opacity: 0.5;
    display: flex;
    align-items: center;
    color: inherit;
    border-radius: 4px;
    transition: opacity 0.2s;

    &:hover {
      opacity: 1;
    }
  }
}

.strix-announce-modal {
  &__content {
    white-space: pre-wrap;
    line-height: 1.6;
    margin-bottom: 12px;
  }

  &__time {
    font-size: 13px;
    opacity: 0.7;
  }
}

@keyframes pulse-text {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}
</style>
```

- [ ] **Step 2: Mount StrixAnnouncement in HomePage.vue**

In `StrixPage/src/views/System/HomePage.vue`:

Find (lines 2-4):
```html
  <div class="nebula-app" @click="clickContainer">
    <!-- 极光背景 -->
    <nebula-bg />
```

Replace with:
```html
  <div class="nebula-app" @click="clickContainer">
    <!-- 系统公告 Banner -->
    <strix-announcement />

    <!-- 极光背景 -->
    <nebula-bg />
```

Add import:

Find (line 79):
```typescript
import NebulaBg from '@/components/system/NebulaBg.vue'
```

Replace with:
```typescript
import StrixAnnouncement from '@/components/common/StrixAnnouncement.vue'
import NebulaBg from '@/components/system/NebulaBg.vue'
```

- [ ] **Step 3: Verify frontend type-checks**

```bash
cd StrixPage && pnpm type-check
```

Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(announce): add StrixAnnouncement global component

- Banner mode: INFO (blue), WARNING (orange), URGENT (red, pulse animation)
- Modal mode: full overlay with 'I acknowledge' button
- sessionStorage dismissal tracking (reset on page refresh)
- Mounted in HomePage.vue above header
- URGENT banners cannot be closed; MODAL requires acknowledgement

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 10: Frontend — Announcement management page

**Files:**
- Create: `StrixPage/src/views/System/SystemMonitor/Announcement/SystemMonitorAnnouncementIndex.vue`

- [ ] **Step 1: Create directory**

```bash
mkdir -p StrixPage/src/views/System/SystemMonitor/Announcement
```

- [ ] **Step 2: Create SystemMonitorAnnouncementIndex.vue**

```vue
<template>
  <div>
    <!-- 统计卡片 -->
    <n-grid :x-gap="12" :y-gap="12" cols="3" style="margin-bottom: 12px">
      <n-gi>
        <n-card size="small">
          <n-spin :show="loading">
            <n-statistic label="总公告数">
              <n-number-animation :from="0" :to="listData?.totalCount ?? 0" />
            </n-statistic>
          </n-spin>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-spin :show="loading">
            <n-statistic label="活跃公告">
              <n-number-animation :from="0" :to="listData?.activeCount ?? 0" />
            </n-statistic>
          </n-spin>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-spin :show="loading">
            <n-statistic label="已终止">
              <n-number-animation :from="0" :to="listData?.terminatedCount ?? 0" />
            </n-statistic>
          </n-spin>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 搜索与操作栏 -->
    <strix-block>
      <template #body>
        <n-grid :cols="6" :x-gap="20" :y-gap="10" item-responsive responsive="screen">
          <n-gi span="6 s:3 m:2">
            <n-input v-model:value="keyword" clearable placeholder="按标题搜索" @keydown.enter="loadData(1)" />
          </n-gi>
          <n-gi span="3 s:2 m:1">
            <n-select
              v-model:value="statusFilter"
              :options="statusOptions"
              clearable
              placeholder="状态"
              @update:value="loadData(1)"
            />
          </n-gi>
          <n-gi span="3 s:2 m:1">
            <n-select
              v-model:value="levelFilter"
              :options="levelOptions"
              clearable
              placeholder="级别"
              @update:value="loadData(1)"
            />
          </n-gi>
          <n-gi span="6 s:5 m:2" class="nebula-export__trigger-gi">
            <n-space align="center" :size="4">
              <n-button
                v-auth="'system:monitor:announcement:terminate'"
                :disabled="checkedRowKeys.length === 0"
                type="error"
                @click="handleBatchTerminate"
              >
                批量终止 ({{ checkedRowKeys.length }})
              </n-button>
              <n-button v-auth="'system:monitor:announcement:publish'" type="primary" @click="showPublishModal = true">
                发布公告
              </n-button>
              <n-button :loading="loading" quaternary type="primary" @click="loadData(1)">
                <template #icon>
                  <strix-icon icon="refresh-cw" :size="16" />
                </template>
                刷新
              </n-button>
            </n-space>
          </n-gi>
        </n-grid>
      </template>
    </strix-block>

    <!-- 公告列表表格 -->
    <n-data-table
      v-model:checked-row-keys="checkedRowKeys"
      :columns="columns"
      :data="listData?.items ?? []"
      :loading="loading"
      :bordered="false"
      :single-line="false"
      :row-key="(row: AnnouncementItem) => row.id"
      size="small"
      :pagination="pagination"
      remote
      @update:page="loadData"
      @update:page-size="handlePageSizeChange"
    />

    <!-- 发布公告模态框 -->
    <n-modal
      v-model:show="showPublishModal"
      class="strix-form-modal"
      preset="card"
      title="发布公告"
      style="width: 600px"
    >
      <n-form
        ref="publishFormRef"
        :model="publishForm"
        :rules="publishFormRules"
        label-placement="left"
        label-width="80"
      >
        <n-form-item label="公告标题" path="title">
          <n-input v-model:value="publishForm.title" maxlength="200" show-count placeholder="请输入公告标题" />
        </n-form-item>
        <n-form-item label="公告级别" path="level">
          <n-radio-group v-model:value="publishForm.level">
            <n-radio value="INFO">普通 (INFO)</n-radio>
            <n-radio value="WARNING">警告 (WARNING)</n-radio>
            <n-radio value="URGENT">紧急 (URGENT)</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="展示方式" path="displayType">
          <n-radio-group v-model:value="publishForm.displayType">
            <n-radio value="BANNER">顶部横幅</n-radio>
            <n-radio value="MODAL">弹窗提醒</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="公告内容" path="content">
          <n-input
            v-model:value="publishForm.content"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 10 }"
            placeholder="请输入公告内容"
          />
        </n-form-item>
        <n-form-item label="生效时间">
          <n-date-picker
            v-model:value="publishForm.startTimeTs"
            type="datetime"
            clearable
            placeholder="留空则立即生效"
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item label="失效时间">
          <n-date-picker
            v-model:value="publishForm.endTimeTs"
            type="datetime"
            clearable
            placeholder="留空则不自动失效"
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-flex justify="end">
          <n-button @click="showPublishModal = false">取消</n-button>
          <n-button type="primary" :loading="publishLoading" @click="handlePublish">发布</n-button>
        </n-flex>
      </template>
    </n-modal>

    <!-- 公告详情模态框 -->
    <n-modal
      v-model:show="showDetailModal"
      class="strix-form-modal"
      preset="card"
      title="公告详情"
      style="width: 600px"
    >
      <template v-if="detailData">
        <n-descriptions :column="2" bordered size="small">
          <n-descriptions-item label="标题" :span="2">{{ detailData.title }}</n-descriptions-item>
          <n-descriptions-item label="级别">
            <n-tag :type="levelTagType(detailData.level)" size="small" :bordered="false">
              {{ detailData.level }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="展示方式">
            {{ detailData.displayType === 'BANNER' ? '顶部横幅' : '弹窗提醒' }}
          </n-descriptions-item>
          <n-descriptions-item label="状态">
            <n-tag :type="detailData.status === 1 ? 'success' : 'error'" size="small" :bordered="false">
              {{ detailData.status === 1 ? '有效' : '已终止' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="创建时间">{{ formatTime(detailData.createdTime) }}</n-descriptions-item>
          <n-descriptions-item v-if="detailData.startTime" label="生效时间">
            {{ formatTime(detailData.startTime) }}
          </n-descriptions-item>
          <n-descriptions-item v-if="detailData.endTime" label="失效时间">
            {{ formatTime(detailData.endTime) }}
          </n-descriptions-item>
          <n-descriptions-item v-if="detailData.endReason" label="终止原因" :span="2">
            {{ detailData.endReason }}
          </n-descriptions-item>
          <n-descriptions-item label="内容" :span="2">
            <div style="white-space: pre-wrap">{{ detailData.content }}</div>
          </n-descriptions-item>
        </n-descriptions>
      </template>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import { announcementApi } from '@/api/announcement'
import type { AnnouncementItem, AnnouncementListResp, AnnouncementDetail, PublishAnnouncementReq } from '@/api/announcement'
import { handleOperate } from '@/utils/strix-table-tool'
import { textField, selectField } from '@/utils/form-rules'
import type { DataTableColumn, DataTableRowKey, FormInst } from 'naive-ui'

const dialog = useDialog()

// 列表状态
const loading = ref(false)
const listData = ref<AnnouncementListResp | null>(null)
const keyword = ref('')
const statusFilter = ref<number | null>(null)
const levelFilter = ref<string | null>(null)
const checkedRowKeys = ref<DataTableRowKey[]>([])
const currentPage = ref(1)
const pageSize = ref(10)

const statusOptions = [
  { label: '有效', value: 1 },
  { label: '已终止', value: 0 }
]

const levelOptions = [
  { label: 'INFO', value: 'INFO' },
  { label: 'WARNING', value: 'WARNING' },
  { label: 'URGENT', value: 'URGENT' }
]

const pagination = computed(() => ({
  page: currentPage.value,
  pageSize: pageSize.value,
  pageCount: Math.ceil((listData.value?.total ?? 0) / pageSize.value),
  showSizePicker: true,
  pageSizes: [10, 20, 50]
}))

// 发布模态框
const showPublishModal = ref(false)
const publishLoading = ref(false)
const publishFormRef = ref<FormInst | null>(null)
const publishForm = ref({
  title: '',
  content: '',
  level: 'INFO',
  displayType: 'BANNER',
  startTimeTs: null as number | null,
  endTimeTs: null as number | null
})

const publishFormRules = {
  title: textField('公告标题'),
  content: textField('公告内容'),
  level: selectField('公告级别'),
  displayType: selectField('展示方式')
}

// 详情模态框
const showDetailModal = ref(false)
const detailData = ref<AnnouncementDetail | null>(null)

// 加载数据
const loadData = async (page?: number) => {
  if (page) currentPage.value = page
  try {
    loading.value = true
    const { data: res } = await announcementApi.list({
      pageSize: pageSize.value,
      pageIndex: currentPage.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value,
      level: levelFilter.value
    })
    listData.value = res.data
  } catch (e) {
    console.error('加载公告列表失败', e)
  } finally {
    loading.value = false
  }
}

const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  loadData(1)
}

// 查看详情
const handleDetail = async (row: AnnouncementItem) => {
  try {
    const { data: res } = await announcementApi.detail(row.id)
    detailData.value = res.data
    showDetailModal.value = true
  } catch (e) {
    console.error('加载公告详情失败', e)
  }
}

// 终止公告
const handleTerminate = async (row: AnnouncementItem) => {
  await announcementApi.terminate(row.id)
  await loadData()
}

// 批量终止
const handleBatchTerminate = () => {
  const ids = checkedRowKeys.value.map(String)
  dialog.warning({
    title: '批量终止确认',
    content: `确定终止选中的 ${ids.length} 条公告吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await announcementApi.batchTerminate(ids)
      checkedRowKeys.value = []
      await loadData()
    }
  })
}

// 发布公告
const handlePublish = async () => {
  try {
    await publishFormRef.value?.validate()
  } catch {
    return
  }

  try {
    publishLoading.value = true
    const reqData: PublishAnnouncementReq = {
      title: publishForm.value.title,
      content: publishForm.value.content,
      level: publishForm.value.level,
      displayType: publishForm.value.displayType,
      startTime: publishForm.value.startTimeTs ? new Date(publishForm.value.startTimeTs).toISOString() : null,
      endTime: publishForm.value.endTimeTs ? new Date(publishForm.value.endTimeTs).toISOString() : null
    }
    await announcementApi.publish(reqData)
    showPublishModal.value = false
    resetPublishForm()
    await loadData(1)
  } catch (e) {
    console.error('发布公告失败', e)
  } finally {
    publishLoading.value = false
  }
}

const resetPublishForm = () => {
  publishForm.value = {
    title: '',
    content: '',
    level: 'INFO',
    displayType: 'BANNER',
    startTimeTs: null,
    endTimeTs: null
  }
}

const formatTime = (time: string | null) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const levelTagType = (level: string) => {
  switch (level) {
    case 'WARNING':
      return 'warning'
    case 'URGENT':
      return 'error'
    default:
      return 'info'
  }
}

// 表格列定义
const columns: DataTableColumn<AnnouncementItem>[] = [
  { type: 'selection' },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  {
    title: '级别',
    key: 'level',
    width: 100,
    render: (row) =>
      h(NTag, { type: levelTagType(row.level), size: 'small', bordered: false }, () => row.level)
  },
  {
    title: '展示方式',
    key: 'displayType',
    width: 100,
    render: (row) => (row.displayType === 'BANNER' ? '顶部横幅' : '弹窗提醒')
  },
  {
    title: '状态',
    key: 'status',
    width: 70,
    render: (row) =>
      h(
        NTag,
        { type: row.status === 1 ? 'success' : 'error', size: 'small', bordered: false },
        () => (row.status === 1 ? '有效' : '已终止')
      )
  },
  {
    title: '生效时间',
    key: 'startTime',
    width: 160,
    render: (row) => (row.startTime ? formatTime(row.startTime) : '立即生效')
  },
  {
    title: '失效时间',
    key: 'endTime',
    width: 160,
    render: (row) => (row.endTime ? formatTime(row.endTime) : '不自动失效')
  },
  {
    title: '创建时间',
    key: 'createdTime',
    width: 160,
    render: (row) => formatTime(row.createdTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: (row) =>
      handleOperate([
        {
          label: '详情',
          icon: 'file-text',
          onClick: () => handleDetail(row)
        },
        ...(row.status === 1
          ? [
              {
                type: 'error' as const,
                label: '终止',
                icon: 'x-circle',
                popconfirm: true,
                popconfirmMessage: `确定终止「${row.title}」吗？`,
                onClick: () => handleTerminate(row)
              }
            ]
          : [])
      ])
  }
]

onMounted(() => loadData(1))
</script>
```

- [ ] **Step 3: Verify frontend type-checks**

```bash
cd StrixPage && pnpm type-check
```

Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(announce): add announcement management page

- Stats cards: totalCount, activeCount, terminatedCount
- Search by title, filter by status + level
- Data table with level tags, display type, time fields
- Publish modal: title, content, level, displayType, start/endTime
- Detail modal with full announcement info
- Batch terminate support

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 11: Frontend route + DB menu entries + build verification

**Files:**
- Modify: `StrixPage/src/router/index.ts`

- [ ] **Step 1: Add announcement management route**

In `StrixPage/src/router/index.ts`:

Find (lines 193-195):
```typescript
          },
          {
            path: 'server',
```

Replace with:
```typescript
          },
          {
            path: 'announcement',
            name: 'SystemMonitorAnnouncementIndex',
            component: () => import('@/views/System/SystemMonitor/Announcement/SystemMonitorAnnouncementIndex.vue'),
            meta: {
              title: '系统公告',
              empty: false,
              permission: 'system:monitor:announcement'
            }
          },
          {
            path: 'server',
```

- [ ] **Step 2: Insert menu entries into database**

Execute the following SQL to add the announcement management menu:

```sql
INSERT INTO strix.sys_system_menu (id, `key`, name, url, icon, parent_id, sort_value, deleted_status, created_time, created_by_type, created_by, updated_time, updated_by_type, updated_by)
VALUES (2044318632242098400, 'system:monitor:announcement', '系统公告', '/system/monitor/announcement', 'megaphone', 1575861681379188738, 6, 0, NOW(), 1, '1111111111111111111', NOW(), 1, '1111111111111111111');
```

Insert role-menu assignment for the super admin role:

```sql
INSERT INTO strix.sys_system_role_menu (system_role_id, system_menu_id)
VALUES ('1000000000000000000', '2044318632242098400');
```

- [ ] **Step 3: Run frontend build**

```bash
cd StrixPage && pnpm build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run frontend lint**

```bash
cd StrixPage && pnpm lint
```

Expected: No errors (warnings acceptable). Fix any lint issues found.

- [ ] **Step 5: Run backend build**

```bash
cd Strix && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(announce): add announcement route + DB menu entries

- Route: /system/monitor/announcement
- Permission: system:monitor:announcement
- Menu: 系统信息管理 → 系统公告 (icon: megaphone)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 12: E2E Testing — All 3 features

**Files:** None (testing only)

- [ ] **Step 1: Restart backend**

Kill the current backend process and restart to pick up all changes:

```bash
cd Strix && ./gradlew bootRun
```

Expected: Application starts successfully on port 9889.

- [ ] **Step 2: Re-login to refresh permissions**

Log out from the frontend, then log back in to pick up the new menu/permission. Credentials: username `anjiongyi`, password `Zhangyi1024!`.

- [ ] **Step 3: E2E test — session:kicked**

1. Open browser at `http://localhost:13232/`
2. Verify SSE connection in DevTools → Network tab (search for `stream`)
3. Open a second browser/incognito window and log in with same credentials
4. In the first browser, navigate to `系统信息管理 → 在线会话管理`
5. Find the current user and click "踢出"
6. In the second browser: verify it redirects to login page and shows a warning dialog "您的会话已被管理员强制下线" (NOT a toast)
7. The dialog should have "我知道了" button and be non-closable by clicking outside

- [ ] **Step 4: E2E test — dict:refresh**

1. Log in and open any page that uses dictionary data (e.g., user management with status dropdown)
2. In another tab, navigate to `系统设置 → 字典管理`
3. Edit a dict data item (e.g., change a label text)
4. After saving, check the SSE EventSource messages in DevTools: verify a `dict:refresh` event was received with the correct `dictKey`
5. Navigate back to the page using that dictionary — verify the updated data is reflected without page refresh

- [ ] **Step 5: E2E test — system:announce (Banner)**

1. Navigate to `系统信息管理 → 系统公告`
2. Verify the page loads with stats cards and empty table
3. Click "发布公告"
4. Fill in:
   - 标题: "测试公告 — INFO"
   - 级别: 普通 (INFO)
   - 展示方式: 顶部横幅
   - 内容: "这是一条测试信息公告"
5. Click "发布"
6. Verify: a blue INFO banner appears at the top of the page
7. Click the X button on the banner to dismiss it
8. Refresh the page — the banner should reappear (sessionStorage clears)

- [ ] **Step 6: E2E test — system:announce (URGENT Banner)**

1. Publish another announcement:
   - 标题: "紧急维护通知"
   - 级别: 紧急 (URGENT)
   - 展示方式: 顶部横幅
   - 内容: "系统将于今晚进行紧急维护"
2. Verify: a red URGENT banner appears with pulse animation on the title
3. Verify the X button is NOT present (URGENT banners cannot be dismissed)

- [ ] **Step 7: E2E test — system:announce (Modal)**

1. Publish another announcement:
   - 标题: "重要公告"
   - 级别: 紧急 (URGENT)
   - 展示方式: 弹窗提醒
   - 内容: "这是一条需要确认的紧急公告"
2. Verify: a modal dialog appears with the announcement content
3. Verify clicking outside the modal does NOT close it
4. Click "我已知悉" to close
5. Verify the modal does not reappear until page refresh

- [ ] **Step 8: E2E test — Terminate announcement**

1. In the announcement management page, click "终止" on an active announcement
2. Verify: the banner/modal is removed from the page
3. Verify: the announcement status changes to "已终止" in the table
4. Check SSE messages: verify `system:announce:dismiss` event was received

- [ ] **Step 9: E2E test — SSE reconnection with announcements**

1. With active announcements, refresh the page
2. After SSE reconnects, verify all active announcements reappear (pushed on connect)
