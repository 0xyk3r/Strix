# Strix 工作流 (Workflow) 系统设计文档

> **日期**: 2026-04-04  
> **状态**: 已确认  
> **版本**: 1.0

---

## 1. 概述

### 1.1 目标

为 Strix 平台设计并实现一个通用的、与具体业务完全解耦的工作流引擎，支持在线流程设计、多版本管理、高级审批操作、自定义触发器和条件、多渠道通知、子流程嵌套，以及完整的监控管理能力。

### 1.2 范围

- **后端**：工作流引擎核心 + REST API + 通知集成 + 定时器 + 事件系统
- **前端**：卡片编排式流程设计器 + 审批工作台 + 监控管理台
- **参与者**：仅 SystemManager，SystemUser 不参与工作流

### 1.3 架构模式

**DAG 执行引擎** — 流程定义为有向无环图 (DAG)，引擎沿图执行。通过 Token 机制追踪执行位置，天然支持并行分支和子流程。

### 1.4 业务关联

**业务键关联模式** — 每个流程实例通过 `bizType` + `bizId` 关联到任意业务实体，引擎不持有业务数据。与现有 Notification 系统风格一致。

### 1.5 模块启用

通过 Strix 模块配置启用：

```yaml
strix:
  module:
    workflow: true
```

---

## 2. 数据模型

### 2.1 ER 关系

```
wf_definition  ──1:N──▶  wf_definition_version (含 DAG Graph JSON)
wf_instance    ──1:N──▶  wf_token
wf_instance    ──1:N──▶  wf_task    ──1:N──▶  wf_task_assignee
wf_instance    ──1:N──▶  wf_comment
wf_instance    ──1:N──▶  wf_timer
wf_instance    ──1:N──▶  wf_instance_var
wf_delegation  (独立表)
```

### 2.2 表结构

#### 2.2.1 wf_definition — 工作流定义

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | 雪花 ID |
| key | String UNIQUE | 流程唯一标识（如 `leave_approval`） |
| name | String | 流程名称 |
| description | String | 流程描述 |
| icon | String | 图标标识 |
| category | String | 分类 |
| status | Short | 1=启用, 2=停用 |
| current_version | Integer | 当前发布版本号 |
| published_version_id | String FK | 当前已发布版本 ID |
| + BaseModel 字段 | | deleted_status, created_time/by, updated_time/by |

#### 2.2.2 wf_definition_version — 版本快照

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | 雪花 ID |
| definition_id | String FK | 所属流程定义 |
| version | Integer | 版本号 (1, 2, 3...) |
| status | Short | 1=草稿, 2=已发布, 3=已废弃 |
| graph_json | JSON (LONGTEXT) | 完整 DAG 图定义 |
| change_log | String | 变更说明 |
| published_at | LocalDateTime | 发布时间 |
| published_by | String | 发布人 |
| + BaseModel 字段 | | |

#### 2.2.3 wf_instance — 流程实例

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | 雪花 ID |
| definition_id | String FK | 流程定义 |
| version_id | String FK | 绑定的版本快照（发起时锁定） |
| title | String | 实例标题 |
| biz_type | String | 业务类型标识 |
| biz_id | String | 业务实体 ID |
| status | Short | 1=进行中, 2=已完成, 3=已拒绝, 4=已撤销, 5=已终止 |
| priority | Short | 优先级 1=低 2=中 3=高 4=紧急 |
| initiator_id | String | 发起人 (SystemManager ID) |
| parent_instance_id | String | 父实例 ID（子流程场景） |
| parent_node_id | String | 父实例中的子流程节点 ID |
| started_at | LocalDateTime | 开始时间 |
| ended_at | LocalDateTime | 结束时间 |
| end_reason | String | 终止原因 |
| + BaseModel 字段 | | |

#### 2.2.4 wf_token — 执行令牌

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | 雪花 ID |
| instance_id | String FK | 所属实例 |
| parent_token_id | String | 父令牌（并行分支场景） |
| current_node_id | String | 当前停留节点 ID（引用 DAG JSON 中的节点 ID） |
| status | Short | 1=活跃, 2=等待, 3=已完成, 4=已终止 |
| arrived_at | LocalDateTime | 到达当前节点时间 |
| completed_at | LocalDateTime | 离开当前节点时间 |
| + BaseModel 字段 | | |

**Token 机制说明**：主流程一个主 Token；遇到并行分支时，主 Token 挂起，产生 N 个子 Token 各走一条分支；所有子 Token 在 Join 节点汇合后，主 Token 恢复。

#### 2.2.5 wf_task — 待办任务

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | 雪花 ID |
| instance_id | String FK | 所属实例 |
| token_id | String FK | 所属 Token |
| node_id | String | DAG 节点 ID |
| type | Short | 1=审批, 2=抄送, 3=自定义任务 |
| approval_mode | Short | 1=任一(OR), 2=同时(AND), 3=顺序(SEQ) |
| status | Short | 1=待处理, 2=处理中, 3=已完成, 4=已取消 |
| result | Short | 0=待定, 1=通过, 2=拒绝, 3=回退, 4=转办 |
| seq_order | Integer | 顺序模式下的当前执行序号 |
| deadline | LocalDateTime | 截止时间 |
| + BaseModel 字段 | | |

#### 2.2.6 wf_task_assignee — 任务处理人

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | 雪花 ID |
| task_id | String FK | 所属任务 |
| assignee_id | String | 处理人 ID (SystemManager) |
| assignee_type | Short | 1=指定人, 2=角色展开, 3=加签, 4=转办, 5=代理 |
| seq_order | Integer | 顺序模式下的序号 |
| status | Short | 1=待处理, 2=已处理, 3=已跳过, 4=已取消 |
| action | Short | 操作：通过/拒绝/回退/转办/加签/降签 |
| comment | String | 审批意见 |
| operated_at | LocalDateTime | 操作时间 |
| delegated_from | String | 代理来源人 ID |
| + BaseModel 字段 | | |

#### 2.2.7 wf_comment — 评论/意见

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | |
| instance_id | String FK | 所属实例 |
| task_id | String | 可选，关联任务 |
| author_id | String | 评论人 |
| content | String | 评论内容 |
| type | Short | 1=审批意见 2=讨论 3=系统 |
| + BaseModel 字段 | | |

#### 2.2.8 wf_timer — 定时器/SLA

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | |
| instance_id | String FK | |
| token_id | String FK | |
| node_id | String | |
| type | Short | 1=延迟 2=超时 3=催办 4=SLA |
| fire_at | LocalDateTime | 触发时间 |
| action | String | 超时动作（AUTO_APPROVE / AUTO_REJECT / DELEGATE / REMIND） |
| action_config | JSON | 动作配置（如转办目标人） |
| repeat_interval | Long | 催办间隔（秒） |
| max_repeat | Integer | 最大催办次数 |
| current_repeat | Integer | 当前已催办次数 |
| status | Short | 1=等待 2=已触发 3=已取消 |
| + BaseModel 字段 | | |

#### 2.2.9 wf_instance_var — 流程实例变量

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | |
| instance_id | String FK | |
| var_key | String | 变量名 |
| var_value | String (JSON) | 变量值 |
| var_type | String | 数据类型 (STRING/NUMBER/BOOLEAN/JSON) |
| + BaseModel 字段 | | |

#### 2.2.10 wf_delegation — 代理人设置

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String PK | |
| delegator_id | String | 委托人 (SystemManager ID) |
| delegate_id | String | 代理人 (SystemManager ID) |
| start_time | LocalDateTime | 有效期开始 |
| end_time | LocalDateTime | 有效期结束 |
| definition_id | String | 可选，限定特定流程 |
| status | Short | 1=有效 2=已过期 3=已撤销 |
| + BaseModel 字段 | | |

### 2.3 DAG Graph JSON 结构

```json
{
  "nodes": [
    {
      "id": "start_1",
      "type": "START",
      "name": "开始",
      "position": { "x": 0, "y": 0 },
      "config": {}
    },
    {
      "id": "approve_1",
      "type": "APPROVAL",
      "name": "部门主管审批",
      "position": { "x": 0, "y": 1 },
      "config": {
        "approvalMode": "ANY",
        "assignees": [
          { "type": "ROLE", "id": "dept_manager" },
          { "type": "MANAGER", "id": "12345" }
        ],
        "timeout": { "duration": "PT24H", "action": "AUTO_APPROVE" },
        "reminder": { "interval": "PT4H", "maxCount": 3 },
        "triggers": {
          "onBeforeEnter": "prepare_review_data",
          "onAfterLeave": "sync_approval_result"
        }
      }
    },
    {
      "id": "condition_1",
      "type": "CONDITION",
      "name": "金额判断",
      "config": {
        "rules": [
          {
            "conditions": [{ "field": "amount", "op": "GT", "value": 10000 }],
            "logic": "AND",
            "targetNodeId": "approve_2"
          }
        ],
        "defaultTargetNodeId": "cc_1"
      }
    }
  ],
  "edges": [
    { "id": "e1", "source": "start_1", "target": "approve_1" },
    { "id": "e2", "source": "approve_1", "target": "condition_1" }
  ]
}
```

---

## 3. 节点类型体系

### 3.1 节点类型总览

| 节点类型 | 标识 | 行为 | 阻塞 |
|----------|------|------|------|
| 开始 | START | 流程入口，自动流转 | 否 |
| 结束 | END | 流程终点，可配置结束状态 | 否 |
| 审批 | APPROVAL | 人工审批，产生待办任务 | 是 |
| 抄送 | CC | 发送通知，非阻塞自动流转 | 否 |
| 条件分支 | CONDITION | 基于规则评估选择分支 | 否 |
| 条件组 | CONDITION_GROUP | 多条件分支（switch/case） | 否 |
| 并行分支 | PARALLEL | Fork/Join 并行执行 | 否* |
| 延迟 | DELAY | 阻塞等待（固定时长/指定时间） | 是 |
| 触发器 | TRIGGER | 调用外部 Spring Bean 方法 | 否** |
| 跳转 | JUMP | 无条件跳转到指定节点 | 否 |
| 子流程 | SUB_PROCESS | 嵌入另一个已发布流程 | 是 |

*并行分支的 Join 节点等待所有/任一子 Token 完成  
**触发器可配置为异步执行

### 3.2 审批节点详细设计

**人员指定方式**：
- `MANAGER` — 指定具体管理人员 (SystemManager)
- `ROLE` — 指定角色 (SystemRole)，运行时展开为该角色下所有人员
- 支持多选组合

**审批模式**：
- `ANY` — 任一审批人通过即通过
- `ALL` — 所有审批人通过才通过
- `SEQ` — 按顺序依次审批

**可执行操作**：
| 操作 | 说明 |
|------|------|
| 通过 | 同意审批，根据模式决定是否流转 |
| 拒绝 | 拒绝审批，任一拒绝即终止（可配置） |
| 回退 | 退回到指定上游节点重新审批 |
| 转办 | 将任务转给他人，自己不再需要操作 |
| 加签 | 增加新审批人（前加签：先审再回到我；后加签：我审完交给新增人） |
| 降签 | 移除审批环节中的某个审批人（仅多人场景） |
| 撕回 | 撤回已提交的审批意见（仅下一节点尚未完成时可操作） |
| 催办 | 手动触发催办通知发送给当前审批人 |

### 3.3 条件评估器

**结构化 JSON 规则**，前端可视化编辑，后端解析执行。

**支持的运算符**：EQ / NEQ / GT / GTE / LT / LTE / IN / NOT_IN / CONTAINS / IS_NULL / IS_NOT_NULL

**条件组合**：支持 AND / OR 逻辑组合，支持嵌套条件组。

**自定义条件**：通过 `@WorkflowCondition` 注解标记的 Spring Bean 方法可作为条件使用。

### 3.4 并行分支

- **Fork**：主 Token 挂起，为每条分支创建子 Token
- **Join 模式**：
  - `ALL` — 所有分支完成后汇合
  - `ANY` — 任一分支完成后汇合，其余分支 Token 标记终止

### 3.5 延迟节点

- **固定时长**：如 `PT24H` (ISO 8601 Duration)
- **指定时间点**：如 `2026-04-05T09:00:00`
- **超时动作**：AUTO_APPROVE / AUTO_REJECT / DELEGATE / REMIND
- **催办周期**：可配置间隔和最大次数
- **SLA 升级**：超时后自动升级处理

### 3.6 子流程

- 嵌入另一个已发布的流程定义
- 创建子 `wf_instance`（通过 `parent_instance_id` 关联）
- 变量传递：父→子（输入映射）/ 子→父（输出映射）
- 子流程独立运行，有自己的 Token 体系
- 子流程完成后回调恢复父流程

---

## 4. 执行引擎

### 4.1 引擎执行流程

```
WorkflowEngine.execute(token):
  1. 获取 Token 当前节点 → currentNode
  2. 查找节点处理器 → NodeHandler handler = registry.get(type)
  3. 触发 onBeforeEnter 事件
  4. handler.enter(context)
  5. 触发 onAfterEnter 事件
  6. 根据节点类型分支处理:
     - [自动节点] → handler.execute() → 确定下一节点 → 递归推进
     - [阻塞节点] → 创建任务/定时器 → Token 挂起 → 返回
     - [并行节点] → 主 Token 挂起 → 创建子 Token → 各自执行
     - [子流程]   → 创建子实例 → 变量映射 → Token 挂起
     - [结束节点] → Token 完成 → 检查是否所有 Token 完成
```

### 4.2 核心类结构

```
cn.projectan.strix.core.module.workflow/
├── engine/
│   ├── WorkflowEngine.java              — 核心引擎
│   ├── ExecutionContext.java            — 执行上下文
│   ├── NodeHandlerRegistry.java         — 节点处理器注册表
│   └── ConditionEvaluator.java          — 条件规则评估器
├── handler/
│   ├── NodeHandler.java                 — 接口
│   ├── StartNodeHandler.java
│   ├── EndNodeHandler.java
│   ├── ApprovalNodeHandler.java
│   ├── CcNodeHandler.java
│   ├── ConditionNodeHandler.java
│   ├── ConditionGroupNodeHandler.java
│   ├── ParallelNodeHandler.java
│   ├── DelayNodeHandler.java
│   ├── TriggerNodeHandler.java
│   ├── JumpNodeHandler.java
│   └── SubProcessNodeHandler.java
├── event/
│   ├── WorkflowEventPublisher.java      — 事件发布器
│   ├── WorkflowEvent.java               — 事件模型
│   ├── WorkflowTrigger.java             — @Annotation
│   ├── WorkflowListener.java            — @Annotation
│   ├── WorkflowCondition.java           — @Annotation
│   └── WorkflowTriggerRegistry.java     — 注解扫描 + 注册
├── timer/
│   ├── WorkflowTimerService.java        — 定时器管理
│   └── WorkflowTimerExecutor.java       — 定时器执行
├── notification/
│   └── WorkflowNotificationService.java — 通知集成层
└── model/
    ├── graph/
    │   ├── WorkflowGraph.java
    │   ├── WorkflowNode.java
    │   ├── WorkflowEdge.java
    │   └── NodeConfig.java
    └── enums/
        ├── NodeType.java
        ├── ApprovalMode.java
        ├── TaskAction.java
        └── InstanceStatus.java
```

### 4.3 节点处理器接口

```java
public interface NodeHandler {
    NodeType getType();
    void enter(ExecutionContext context);
    NodeExecutionResult execute(ExecutionContext context);
    void leave(ExecutionContext context);
}
```

---

## 5. 事件与触发器体系

### 5.1 三层事件模型

**流程级事件**：PROCESS_STARTED / PROCESS_COMPLETED / PROCESS_REJECTED / PROCESS_CANCELLED / PROCESS_TERMINATED / PROCESS_ERROR

**节点级事件**：NODE_BEFORE_ENTER / NODE_AFTER_ENTER / NODE_BEFORE_LEAVE / NODE_AFTER_LEAVE / NODE_ERROR

**任务级事件**：TASK_CREATED / TASK_APPROVED / TASK_REJECTED / TASK_DELEGATED / TASK_COUNTERSIGNED / TASK_WITHDRAWN / TASK_TIMEOUT / TASK_REMINDED

### 5.2 注解驱动

**@WorkflowTrigger** — 标记自定义触发器方法，TRIGGER 节点通过 `key` 调用：

```java
@WorkflowTrigger(key = "check_order_amount", name = "校验订单金额")
public Map<String, Object> checkAmount(WorkflowTriggerContext ctx) {
    // 业务逻辑...
    return Map.of("overLimit", true);  // 返回值写入流程变量
}
```

**@WorkflowListener** — 监听流程/节点/任务事件：

```java
@WorkflowListener(event = WorkflowEventType.PROCESS_COMPLETED, definitionKey = "leave_approval")
public void onLeaveApproved(WorkflowEvent event) {
    // 更新业务状态...
}
```

**@WorkflowCondition** — 自定义条件评估器：

```java
@WorkflowCondition(key = "is_weekend", name = "是否周末")
public boolean isWeekend(WorkflowTriggerContext ctx) {
    return LocalDate.now().getDayOfWeek().getValue() >= 6;
}
```

### 5.3 自动发现机制

启动时通过 `WorkflowTriggerRegistry` (BeanPostProcessor) 扫描所有 Spring Bean，查找上述三种注解并注册。前端设计器通过 API 获取已注册列表：

```
GET /workflow/triggers    → [{ key, name, description }]
GET /workflow/conditions  → [{ key, name, description }]
```

### 5.4 节点级触发器绑定

任何节点都可在 config 中绑定生命周期触发器：

```json
{
  "triggers": {
    "onBeforeEnter": "prepare_review_data",
    "onAfterLeave": "sync_approval_result"
  }
}
```

---

## 6. 通知集成

### 6.1 多渠道通知

| 渠道 | 实现 | 说明 |
|------|------|------|
| 站内通知 | 复用 `NotificationService` | 创建 Notification + NotificationReceiver |
| WebSocket | 复用现有 WebSocket | 实时推送待办/催办/审批结果 |
| SMS | 复用 `StrixSmsClient` | 可选，在节点配置中选择启用 |

### 6.2 通知时机

- 任务创建时通知审批人
- 审批完成时通知发起人
- 催办时通知审批人
- 超时时通知审批人和管理员
- 抄送时通知抄送人
- 流程完成/拒绝时通知发起人

### 6.3 通知模板

支持在节点配置中定义通知标题和内容模板，支持变量占位符：

```json
{
  "notifyTemplate": {
    "title": "${initiatorName}的${definitionName}",
    "content": "请尽快处理${instanceTitle}，当前节点：${nodeName}"
  },
  "notifyChannels": ["SITE", "SMS"]
}
```

---

## 7. 定时器体系

### 7.1 定时器类型

| 类型 | 说明 | 驱动方式 |
|------|------|----------|
| DELAY | 延迟节点的固定等待 | Redisson 延迟队列 |
| TIMEOUT | 审批超时 | Quartz 调度 |
| REMINDER | 催办提醒 | Quartz 周期任务 |
| SLA | SLA 超时升级 | Quartz 调度 |

### 7.2 超时动作

- `AUTO_APPROVE` — 自动通过
- `AUTO_REJECT` — 自动拒绝
- `DELEGATE` — 自动转办给指定人
- `REMIND` — 仅发送提醒通知

### 7.3 催办机制

可配置催办间隔和最大次数，超过最大次数后执行超时动作。

---

## 8. 版本控制

### 8.1 版本模型

- **内部版本号**：每次保存递增 (v1, v2, v3...)
- **草稿/发布状态**：编辑时为草稿，显式发布后生效
- **版本快照**：发布时将 DAG Graph JSON 锁定在 `wf_definition_version` 中

### 8.2 版本行为

- 编辑中的草稿不影响已发布版本
- 新发起的流程使用当前已发布版本
- 已发起的流程始终绑定发起时的版本快照，不受后续版本更新影响
- 发布新版本时，旧版本标记为"已废弃"
- 支持查看历史版本对比

---

## 9. 委派与代理

### 9.1 委派（转办）

审批人在处理任务时，可将任务转给他人处理，自己不再需要操作。

### 9.2 代理

- 管理人员可提前设置代理人，配置有效期
- 有效期内，新产生的审批任务自动分配给代理人
- 可限定特定流程或全部流程
- 代理记录在 `wf_delegation` 表中

---

## 10. 监控与管理

### 10.1 监控仪表盘

- 实时统计：进行中/已完成/已超时/平均耗时
- 流程实例列表：支持按状态/类型/发起人筛选
- 实例详情：执行路径可视化、时间线、评论

### 10.2 人工干预

| 操作 | 说明 |
|------|------|
| 强制跳转 | 将流程跳转到指定节点 |
| 强制终止 | 终止运行中的流程 |
| 重新分配 | 将当前任务重新分配给其他人 |
| 挂起/恢复 | 暂停/恢复流程执行 |

### 10.3 审计日志

所有操作（审批/转办/加签/干预等）记录在 `wf_task_assignee` 和 `wf_comment` 中，支持完整的审计追踪。

---

## 11. REST API 设计

### 11.1 流程定义管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/workflow/definitions` | 分页列表 |
| POST | `/workflow/definitions` | 创建定义 |
| GET | `/workflow/definitions/{id}` | 获取详情 |
| PUT | `/workflow/definitions/{id}` | 更新元数据 |
| DELETE | `/workflow/definitions/{id}` | 删除（仅无实例时） |
| PUT | `/workflow/definitions/{id}/status` | 启用/停用 |

### 11.2 版本管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/workflow/definitions/{id}/versions` | 版本列表 |
| POST | `/workflow/definitions/{id}/versions` | 保存草稿 |
| GET | `/workflow/definitions/{id}/versions/{vid}` | 版本详情 |
| PUT | `/workflow/definitions/{id}/versions/{vid}` | 更新草稿 |
| POST | `/workflow/definitions/{id}/versions/{vid}/publish` | 发布版本 |

### 11.3 流程实例

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/workflow/instances` | 发起流程 |
| GET | `/workflow/instances` | 实例列表 |
| GET | `/workflow/instances/{id}` | 实例详情 |
| POST | `/workflow/instances/{id}/cancel` | 撤销（发起人） |
| POST | `/workflow/instances/{id}/terminate` | 强制终止（管理员） |
| POST | `/workflow/instances/{id}/jump` | 强制跳转（管理员） |
| GET | `/workflow/instances/{id}/timeline` | 执行时间线 |
| GET | `/workflow/instances/{id}/comments` | 评论列表 |
| POST | `/workflow/instances/{id}/comments` | 添加评论 |

### 11.4 任务操作

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/workflow/tasks/mine` | 我的待办 |
| GET | `/workflow/tasks/done` | 我已处理 |
| GET | `/workflow/tasks/initiated` | 我发起的 |
| GET | `/workflow/tasks/cc` | 抄送给我 |
| POST | `/workflow/tasks/{id}/approve` | 通过 |
| POST | `/workflow/tasks/{id}/reject` | 拒绝 |
| POST | `/workflow/tasks/{id}/return` | 回退 |
| POST | `/workflow/tasks/{id}/delegate` | 转办 |
| POST | `/workflow/tasks/{id}/countersign` | 加签 |
| POST | `/workflow/tasks/{id}/remove-sign` | 降签 |
| POST | `/workflow/tasks/{id}/withdraw` | 撕回 |
| POST | `/workflow/tasks/{id}/urge` | 催办 |

### 11.5 系统配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/workflow/triggers` | 已注册触发器列表 |
| GET | `/workflow/conditions` | 已注册条件列表 |
| GET | `/workflow/delegations` | 代理设置列表 |
| POST | `/workflow/delegations` | 设置代理人 |
| DELETE | `/workflow/delegations/{id}` | 撤销代理 |
| GET | `/workflow/stats` | 统计数据 |

---

## 12. 前端设计

### 12.1 技术方案

| 方面 | 方案 |
|------|------|
| 画布渲染 | 纯 DOM/CSS 渲染（卡片布局） |
| 拖拽排序 | 复用 `vue-draggable-plus` |
| 连线绘制 | CSS/SVG |
| 状态管理 | Pinia store（支持撤销/重做） |
| UI 组件 | Naive UI |
| 新增依赖 | 无 |

### 12.2 页面规划

| 页面 | 路由 | 说明 |
|------|------|------|
| 流程定义列表 | `/system/module/workflow` | 所有流程定义 |
| 流程设计器 | `/system/module/workflow/designer/:id` | 卡片编排设计器 |
| 我的待办 | `/system/module/workflow/tasks` | 审批工作台 |
| 流程实例列表 | `/system/module/workflow/instances` | 实例管理 |
| 流程实例详情 | `/system/module/workflow/instances/:id` | 执行路径+时间线 |
| 监控仪表盘 | `/system/module/workflow/monitor` | 统计+干预 |
| 代理设置 | `/system/module/workflow/delegation` | 代理人管理 |

### 12.3 设计器交互

- **卡片编排式**：上下排列的节点卡片，类钉钉/飞书风格
- **添加节点**：点击节点间 "+" 按钮弹出选择菜单
- **配置节点**：点击节点卡片在右侧面板编辑配置
- **条件/并行分支**：横向展开为多列卡片
- **拖拽排序**：支持节点上下拖拽调整顺序
- **版本管理**：顶部工具栏显示版本状态，支持保存草稿/发布

---

## 13. 后端文件组织

```
src/main/java/cn/projectan/strix/
├── controller/srv/workflow/
│   ├── WorkflowDefinitionController.java
│   ├── WorkflowInstanceController.java
│   ├── WorkflowTaskController.java
│   └── WorkflowAdminController.java
├── service/common/workflow/
│   ├── WfDefinitionService.java
│   ├── WfVersionService.java
│   ├── WfInstanceService.java
│   ├── WfTokenService.java
│   ├── WfTaskService.java
│   ├── WfTaskAssigneeService.java
│   ├── WfCommentService.java
│   ├── WfTimerService.java
│   ├── WfInstanceVarService.java
│   ├── WfDelegationService.java
│   └── WfStatsService.java
├── mapper/workflow/
│   ├── WfDefinitionMapper.java
│   ├── WfDefinitionVersionMapper.java
│   ├── WfInstanceMapper.java
│   ├── WfTokenMapper.java
│   ├── WfTaskMapper.java
│   ├── WfTaskAssigneeMapper.java
│   ├── WfCommentMapper.java
│   ├── WfTimerMapper.java
│   ├── WfInstanceVarMapper.java
│   └── WfDelegationMapper.java
├── model/
│   ├── db/workflow/
│   │   ├── WfDefinition.java
│   │   ├── WfDefinitionVersion.java
│   │   ├── WfInstance.java
│   │   ├── WfToken.java
│   │   ├── WfTask.java
│   │   ├── WfTaskAssignee.java
│   │   ├── WfComment.java
│   │   ├── WfTimer.java
│   │   ├── WfInstanceVar.java
│   │   └── WfDelegation.java
│   ├── request/workflow/
│   │   └── (Request DTOs)
│   ├── response/workflow/
│   │   └── (Response DTOs)
│   └── enums/workflow/
│       ├── WfNodeType.java
│       ├── WfApprovalMode.java
│       ├── WfInstanceStatus.java
│       ├── WfTaskAction.java
│       ├── WfTaskStatus.java
│       ├── WfTokenStatus.java
│       ├── WfTimerType.java
│       └── WfVersionStatus.java
├── core/module/workflow/
│   ├── engine/
│   │   ├── WorkflowEngine.java
│   │   ├── ExecutionContext.java
│   │   ├── NodeHandlerRegistry.java
│   │   └── ConditionEvaluator.java
│   ├── handler/
│   │   ├── NodeHandler.java
│   │   ├── StartNodeHandler.java
│   │   ├── EndNodeHandler.java
│   │   ├── ApprovalNodeHandler.java
│   │   ├── CcNodeHandler.java
│   │   ├── ConditionNodeHandler.java
│   │   ├── ConditionGroupNodeHandler.java
│   │   ├── ParallelNodeHandler.java
│   │   ├── DelayNodeHandler.java
│   │   ├── TriggerNodeHandler.java
│   │   ├── JumpNodeHandler.java
│   │   └── SubProcessNodeHandler.java
│   ├── event/
│   │   ├── WorkflowEventPublisher.java
│   │   ├── WorkflowEvent.java
│   │   ├── WorkflowEventType.java
│   │   ├── WorkflowTrigger.java
│   │   ├── WorkflowListener.java
│   │   ├── WorkflowCondition.java
│   │   ├── WorkflowTriggerContext.java
│   │   └── WorkflowTriggerRegistry.java
│   ├── timer/
│   │   ├── WorkflowTimerService.java
│   │   └── WorkflowTimerExecutor.java
│   ├── notification/
│   │   └── WorkflowNotificationService.java
│   └── model/
│       ├── WorkflowGraph.java
│       ├── WorkflowNode.java
│       ├── WorkflowEdge.java
│       ├── NodeConfig.java
│       └── NodeExecutionResult.java
└── resources/
    ├── mapper/workflow/
    │   └── (Mapper XML 文件)
    └── i18n/
        └── workflow_zh_CN.properties
```

### 前端文件组织

```
src/
├── api/workflow.ts
├── stores/workflow.ts
├── views/System/SystemModule/Workflow/
│   ├── WorkflowListPage.vue
│   ├── WorkflowDesignerPage.vue
│   ├── WorkflowTasksPage.vue
│   ├── WorkflowInstancesPage.vue
│   ├── WorkflowInstanceDetailPage.vue
│   ├── WorkflowMonitorPage.vue
│   ├── WorkflowDelegationPage.vue
│   └── components/
│       ├── designer/
│       │   ├── DesignerCanvas.vue
│       │   ├── NodeCard.vue
│       │   ├── NodeConnector.vue
│       │   ├── NodeConfigPanel.vue
│       │   ├── AddNodeMenu.vue
│       │   ├── ConditionBranch.vue
│       │   ├── ParallelBranch.vue
│       │   └── AssigneeSelector.vue
│       ├── task/
│       │   ├── TaskList.vue
│       │   ├── TaskActionDialog.vue
│       │   └── ApprovalForm.vue
│       ├── instance/
│       │   ├── InstanceTimeline.vue
│       │   ├── InstanceFlowView.vue
│       │   └── CommentSection.vue
│       └── monitor/
│           ├── StatsCards.vue
│           ├── InterventionDialog.vue
│           └── TimeAnalysisChart.vue
```
