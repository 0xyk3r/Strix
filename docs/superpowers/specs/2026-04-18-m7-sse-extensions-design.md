# M7: SSE 扩展 — session:kicked / dict:refresh / system:announce

**Date:** 2026-04-18
**Status:** Draft
**Milestone:** M7

---

## 1. 概述

在现有 SSE 基础设施上扩展 3 个新事件类型：

| 事件 | 用途 | 复杂度 |
|------|------|--------|
| `session:kicked` | 管理员强制踢出会话时实时通知被踢用户 | 低 |
| `dict:refresh` | 字典数据变更时通知前端热更新缓存 | 低 |
| `system:announce` | 系统公告实时推送 + 管理页面 | 中 |

所有事件复用现有 `SseController` (`/sse/stream`) + `SseSessionManager` 基础设施。

---

## 2. Feature 1: `session:kicked`

### 2.1 问题

当前管理员通过「在线会话管理」踢出用户时，被踢用户只有在下次 API 调用返回 401 时才会感知会话失效。用户体验差且无法区分「被踢出」与「token 自然过期」。

### 2.2 方案

在删除 Redis token 之前，通过 SSE 推送 `session:kicked` 事件给被踢用户，前端收到后立即跳转登录页并弹窗提示。

### 2.3 后端改动

**文件：** `TokenSessionService.java`

在 `kickManagerSession(managerId, token)` 方法中，**删除 Redis key 之前**注入 `SseSessionManager` 并推送事件：

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

对于 `invalidateManagerSession(managerId)` — 踢出某管理员的全部会话，同样先推送再删除。

**时序关键：** 必须先推送 SSE 再删除 Redis key。如果反过来，SSE 连接可能已在心跳校验中断开。

### 2.4 前端改动

**文件：** `stores/sse.ts`

新增事件监听器：

```typescript
eventSource.addEventListener('session:kicked', (event: MessageEvent) => {
  try {
    const data = JSON.parse(event.data)
    console.log('SSE: 收到强制下线事件', data.reason)
    handleKicked(data.message)
  } catch (e) {
    console.error('SSE: 解析 session:kicked 事件失败', e)
  }
})
```

`handleKicked(message)` 逻辑：
1. 断开 SSE 连接 (`disconnect()`)
2. 清除登录信息 (`loginInfoStore.clearLoginInfo()`)
3. 跳转登录页 (`router.replace({ path: '/login', query: { r: 'kicked' } })`)
4. **跳转完成后**使用 `window.$dialog.warning()` 弹窗显示被踢原因

UX 区别于普通 401：
- 普通 401：静默跳转 + 底部 toast 提示「登录失效」
- session:kicked：跳转后 **弹窗 dialog** 提示「您的会话已被管理员强制下线」，需用户点击确认

### 2.5 事件格式

```json
{
  "event": "session:kicked",
  "data": {
    "reason": "kicked_by_admin",
    "message": "您的会话已被管理员强制下线"
  }
}
```

---

## 3. Feature 2: `dict:refresh`

### 3.1 问题

字典数据通过 `useDictStore` 进行版本缓存 + localStorage 持久化。当管理员修改字典后，其他已登录用户的字典缓存不会更新，直到页面刷新或缓存版本校验触发。

### 3.2 方案

字典变更时通过 SSE 广播 `dict:refresh` 事件，前端收到后**立即请求该 key 的最新数据并覆盖缓存**（而非仅清除），避免已使用该字典的组件因响应式绑定出现选项为空的闪烁。

### 3.3 后端改动

**文件：** `CacheEvictionListener.java`

目前该文件没有 dict 相关的事件处理。需要：

1. 创建 `DictChangedEvent` 事件类（或复用已有的缓存事件机制）
2. 在 `SystemDictDataService` 的增/删/改操作后发布事件
3. 在 `CacheEvictionListener` 添加处理方法：

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onDictChanged(DictChangedEvent event) {
    cacheEvictionService.evictDictCache(event.getDictKey());
    broadcaster.broadcastDictChange(event.getDictKey());
    sseSessionManager.broadcast("dict:refresh", Map.of(
        "dictKey", event.getDictKey(),
        "reason", event.getReason()  // "data_added", "data_updated", "data_deleted"
    ));
    log.debug("SSE 广播 dict:refresh, dictKey={}, reason={}", event.getDictKey(), event.getReason());
}
```

**新增文件：**
- `model/event/DictChangedEvent.java` — 携带 `dictKey` 和 `reason`

**修改文件：**
- `SystemDictDataService.java` — 在字典数据 CRUD 操作的事务内发布 `DictChangedEvent`

### 3.4 前端改动

**文件：** `stores/dict.ts`

新增 `refreshDictByKey(key)` 方法，跳过缓存校验直接获取最新数据：

```typescript
async function refreshDictByKey(key: string): Promise<void> {
  // 直接请求最新数据，覆盖写入 dictMap
  await fetchDictData(key)
  console.log('Dict: 已刷新字典数据, key=', key)
}
```

**文件：** `stores/sse.ts`

新增事件监听器：

```typescript
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
```

### 3.5 事件格式

```json
{
  "event": "dict:refresh",
  "data": {
    "dictKey": "SystemUserStatus",
    "reason": "data_updated"
  }
}
```

### 3.6 行为细节

- 仅刷新 SSE 事件指定的 key，不全量清除
- `fetchDictData()` 会更新 `dictMap` 中对应 key 的值，已挂载组件通过 `shallowRef` 响应式自动获取新数据
- 如果该 key 当前没有被任何组件使用（`dictMap` 中不存在），也会写入缓存，供后续使用
- `pendingMap` 去重机制仍然生效：如果恰好有并发请求，不会重复 fetch

---

## 4. Feature 3: `system:announce`

### 4.1 问题

系统需要向所有在线管理员推送公告（如维护计划、紧急通知），当前没有此能力。公告与通知 (Notification) 不同：
- 通知是一对一/一对多的消息，需要已读追踪
- 公告是全局性的，面向所有在线用户，生命周期由发布者控制

### 4.2 方案

独立的公告子系统，不复用通知表。公告通过 SSE 实时推送，前端以 Banner 或 Modal 形式展示。

### 4.3 数据模型

**新表：** `sys_system_announcement`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) PK | Snowflake ID |
| title | varchar(200) | 公告标题 |
| content | text | 公告内容 |
| level | varchar(20) | 级别: INFO / WARNING / URGENT |
| display_type | varchar(20) | 展示方式: BANNER / MODAL |
| status | smallint | 1=有效 0=已终止 |
| start_time | datetime | 生效时间（可为 null 表示立即生效） |
| end_time | datetime | 失效时间（可为 null 表示不自动失效） |
| end_by | varchar(32) | 终止人 ID |
| end_reason | varchar(200) | 终止原因 |
| deleted_status | smallint | 逻辑删除 |
| created_time | datetime | 创建时间 |
| created_by | varchar(32) | 创建人 |
| created_by_type | smallint | 创建人类型 |
| updated_time | datetime | 更新时间 |
| updated_by | varchar(32) | 更新人 |
| updated_by_type | smallint | 更新人类型 |

继承 `BaseModel<SystemAnnouncement>`，自动获得 id/deleted_status/created*/updated* 字段。

### 4.4 后端 API

**Controller:** `SystemAnnouncementController` — 挂载在 `system/monitor/announcement`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/` | `system:monitor:announcement` | 公告管理列表（分页、关键字搜索、状态筛选） |
| GET | `/{id}` | `system:monitor:announcement` | 公告详情 |
| POST | `/publish` | `system:monitor:announcement:publish` | 发布公告 |
| POST | `/{id}/terminate` | `system:monitor:announcement:terminate` | 终止公告 |
| POST | `/batch-terminate` | `system:monitor:announcement:terminate` | 批量终止 |
| GET | `/active` | `@Anonymous` (SSE 内部调用) | 获取所有活跃公告（供 SSE 连接时初始推送） |

**Service:** `SystemAnnouncementService`

- `publish()` — 保存公告 + SSE 广播 `system:announce`
- `terminate()` — 更新状态 + SSE 广播 `system:announce:dismiss`
- `getActiveAnnouncements()` — 查询当前有效公告（start_time <= now, end_time > now or null, status=1）

### 4.5 SSE 集成

**新公告发布：**

```java
sseSessionManager.broadcast("system:announce", Map.of(
    "id", announcement.getId(),
    "title", announcement.getTitle(),
    "content", announcement.getContent(),
    "level", announcement.getLevel(),
    "displayType", announcement.getDisplayType(),
    "startTime", announcement.getStartTime(),
    "endTime", announcement.getEndTime()
));
```

**公告终止：**

```java
sseSessionManager.broadcast("system:announce:dismiss", Map.of(
    "id", announcement.getId()
));
```

**SSE 连接时初始推送：**

在 `SseController.connect()` 中，创建 emitter 后，除了推送 `notification:count`，还推送所有活跃公告：

```java
List<SystemAnnouncement> active = announcementService.getActiveAnnouncements();
for (SystemAnnouncement a : active) {
    emitter.send(SseEmitter.event()
        .name("system:announce")
        .data(Map.of("id", a.getId(), "title", a.getTitle(), ...)));
}
```

### 4.6 前端组件

**全局组件：** `StrixAnnouncement.vue`

位置：挂载在 `App.vue` 或主布局组件中，所有页面共享。

**Banner 模式（INFO / WARNING / URGENT）：**
- 固定在页面顶部（header 上方）
- 支持多条公告同时展示（垂直堆叠）
- 颜色方案：
  - INFO — 蓝色背景 `#e8f4fd`，蓝色文字/图标
  - WARNING — 橙色背景 `#fef3e2`，橙色文字/图标，显示倒计时（到 endTime）
  - URGENT — 红色背景 `#fde8e8`，红色文字/图标，标题 pulse 动画
- 用户可点击关闭（记入 `sessionStorage`，刷新后若公告仍有效则重新显示）
- 关闭按钮仅 INFO/WARNING 级别可用，URGENT Banner 不可关闭

**Modal 模式（仅 URGENT）：**
- 全屏半透明遮罩 + 居中卡片
- 显示公告标题、内容、级别标签
- 用户必须点击「我已知悉」按钮关闭
- 关闭后记入 `sessionStorage`

**状态管理：**

在 `stores/sse.ts` 中维护活跃公告列表：

```typescript
const activeAnnouncements = ref<Announcement[]>([])

eventSource.addEventListener('system:announce', (event) => {
  const data = JSON.parse(event.data)
  // 去重：如果已存在则更新，否则添加
  const idx = activeAnnouncements.value.findIndex(a => a.id === data.id)
  if (idx >= 0) {
    activeAnnouncements.value[idx] = data
  } else {
    activeAnnouncements.value.push(data)
  }
})

eventSource.addEventListener('system:announce:dismiss', (event) => {
  const data = JSON.parse(event.data)
  activeAnnouncements.value = activeAnnouncements.value.filter(a => a.id !== data.id)
})
```

### 4.7 管理页面

**路由：** `/system/monitor/announcement`
**菜单：** 系统信息管理 → 系统公告

页面布局与通知管理页类似：
- 统计卡片：总公告数 / 活跃公告数 / 已终止数
- 搜索栏：标题关键字 + 状态筛选 + 级别筛选
- 数据表格：标题、级别 Tag、展示方式、状态、创建时间、操作（详情/终止）
- 发布公告模态框：标题、内容、级别选择、展示方式选择、生效/失效时间
- 批量终止

### 4.8 事件格式

**system:announce:**

```json
{
  "event": "system:announce",
  "data": {
    "id": "2044318632242098300",
    "title": "系统维护通知",
    "content": "系统将于今晚 22:00-23:00 进行维护升级...",
    "level": "WARNING",
    "displayType": "BANNER",
    "startTime": "2026-04-18T22:00:00",
    "endTime": "2026-04-18T23:00:00"
  }
}
```

**system:announce:dismiss:**

```json
{
  "event": "system:announce:dismiss",
  "data": {
    "id": "2044318632242098300"
  }
}
```

---

## 5. 文件变更汇总

### 5.1 后端

**新增文件：**
- `model/db/system/SystemAnnouncement.java` — 公告实体
- `mapper/system/SystemAnnouncementMapper.java` — MyBatis Plus Mapper
- `service/system/SystemAnnouncementService.java` — 公告服务
- `controller/system/monitor/SystemAnnouncementController.java` — 公告管理 API
- `model/request/system/announcement/PublishAnnouncementReq.java` — 发布请求 DTO
- `model/response/system/announcement/AnnouncementListResp.java` — 列表响应 DTO
- `model/event/DictChangedEvent.java` — 字典变更事件

**修改文件：**
- `service/system/TokenSessionService.java` — 注入 SseSessionManager，kickManagerSession 前推送 session:kicked
- `core/cache/CacheEvictionListener.java` — 添加 onDictChanged 处理器
- `controller/sse/SseController.java` — connect 时推送活跃公告
- `service/system/SystemDictDataService.java` — CRUD 操作后发布 DictChangedEvent

### 5.2 前端

**新增文件：**
- `api/announcement.ts` — 公告管理 API 模块
- `components/common/StrixAnnouncement.vue` — 全局公告展示组件
- `views/System/SystemMonitor/Announcement/SystemMonitorAnnouncementIndex.vue` — 公告管理页面

**修改文件：**
- `stores/sse.ts` — 添加 session:kicked / dict:refresh / system:announce 事件监听
- `stores/dict.ts` — 添加 refreshDictByKey() 方法
- `router/index.ts` — 添加公告管理路由

### 5.3 数据库

- 新建 `sys_system_announcement` 表
- 插入菜单数据 `sys_system_menu` + 角色菜单 `sys_system_role_menu`

---

## 6. 实现顺序

建议按以下顺序实现：

1. **session:kicked** — 最小改动，只需修改 TokenSessionService + sse.ts
2. **dict:refresh** — 新增事件类 + CacheEvictionListener + dict store + sse.ts
3. **system:announce** — 完整子系统（实体/Mapper/Service/Controller/前端组件/管理页面）

每个 feature 独立可测试，不互相依赖。
