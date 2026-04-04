# Workflow System — Plan 1: Backend Foundation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the complete data foundation for the workflow system — SQL schema, entities, mappers, enums/dicts, graph model, basic CRUD services, and module configuration.

**Architecture:** DAG-based workflow engine. This plan establishes the data layer: 10 database tables (`wf_` prefix), entity classes extending `BaseModel<T>`, MyBatis Plus mappers, graph model POJOs for JSON serialization, and thin service wrappers. All workflow code is conditionally loaded via `@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")`.

**Tech Stack:** Java 21, Spring Boot 4.0.2, MyBatis Plus 3.5.16, Jackson, Lombok

**Spec:** `docs/superpowers/specs/2026-04-04-workflow-design.md`

---

## File Structure

### New Files

**Dict Constants** (`model/dict/workflow/`):
- `WfDefinitionStatus.java` — 流程定义状态 (1=启用, 2=停用)
- `WfVersionStatus.java` — 版本状态 (1=草稿, 2=已发布, 3=已废弃)
- `WfInstanceStatus.java` — 实例状态 (1=进行中, 2=已完成, 3=已拒绝, 4=已撤销, 5=已终止)
- `WfInstancePriority.java` — 实例优先级 (1=低, 2=中, 3=高, 4=紧急)
- `WfTokenStatus.java` — 令牌状态 (1=活跃, 2=等待, 3=已完成, 4=已终止)
- `WfTaskType.java` — 任务类型 (1=审批, 2=抄送, 3=自定义)
- `WfApprovalMode.java` — 审批模式 (1=任一, 2=全部, 3=顺序)
- `WfTaskStatus.java` — 任务状态 (1=待处理, 2=处理中, 3=已完成, 4=已取消)
- `WfTaskResult.java` — 任务结果 (0=待定, 1=通过, 2=拒绝, 3=回退, 4=转办)
- `WfAssigneeType.java` — 处理人类型 (1=指定人, 2=角色展开, 3=加签, 4=转办, 5=代理)
- `WfAssigneeStatus.java` — 处理人状态 (1=待处理, 2=已处理, 3=已跳过, 4=已取消)
- `WfAssigneeAction.java` — 处理人操作 (1=通过, 2=拒绝, 3=回退, 4=转办, 5=加签, 6=降签)
- `WfCommentType.java` — 评论类型 (1=审批意见, 2=讨论, 3=系统)
- `WfTimerType.java` — 定时器类型 (1=延迟, 2=超时, 3=催办, 4=SLA)
- `WfTimerStatus.java` — 定时器状态 (1=等待, 2=已触发, 3=已取消)
- `WfDelegationStatus.java` — 代理状态 (1=有效, 2=已过期, 3=已撤销)

**Enum Classes** (`model/enums/workflow/`):
- `WfNodeType.java` — 11 node types (START, END, APPROVAL, CC, etc.)
- `WfTimerAction.java` — Timer actions (AUTO_APPROVE, AUTO_REJECT, DELEGATE, REMIND)
- `WfVarType.java` — Variable types (STRING, NUMBER, BOOLEAN, JSON)

**Entity Classes** (`model/db/workflow/`):
- `WfDefinition.java`, `WfDefinitionVersion.java`, `WfInstance.java`
- `WfToken.java`, `WfTask.java`, `WfTaskAssignee.java`
- `WfComment.java`, `WfTimer.java`, `WfInstanceVar.java`, `WfDelegation.java`

**Mapper Interfaces** (`mapper/workflow/`):
- 10 mapper interfaces (one per entity)

**Mapper XML** (`resources/mapper/workflow/`):
- 10 empty XML mapper files

**Graph Model** (`core/module/workflow/model/`):
- `WorkflowGraph.java`, `WorkflowNode.java`, `WorkflowEdge.java`
- `NodePosition.java`, `NodeExecutionResult.java`

**Graph Config Models** (`core/module/workflow/model/config/`):
- `ApprovalNodeConfig.java`, `CcNodeConfig.java`, `ConditionNodeConfig.java`
- `ParallelNodeConfig.java`, `DelayNodeConfig.java`, `TriggerNodeConfig.java`
- `JumpNodeConfig.java`, `SubProcessNodeConfig.java`, `EndNodeConfig.java`
- `AssigneeConfig.java`, `TimeoutConfig.java`, `ReminderConfig.java`
- `NodeTriggerConfig.java`, `NotifyTemplateConfig.java`
- `ConditionBranch.java`, `ConditionItem.java`, `ParallelBranch.java`

**Services** (`service/common/workflow/`):
- 10 service classes (one per entity) + `WfStatsService.java`

**SQL**:
- `docs/sql/workflow_tables.sql`

**Tests**:
- `src/test/java/cn/projectan/strix/core/module/workflow/model/WorkflowGraphTest.java`

### Modified Files
- `src/main/resources/application.yml` — add `workflow: true`

---

## Conventions Reference

Before implementing, review these codebase conventions:

- **Entities**: Extend `BaseModel<T>`, use `@Getter @Setter @Accessors(chain = true) @NoArgsConstructor @AllArgsConstructor @TableName("table_name")`
- **BaseModel fields** (inherited, do NOT redeclare): `id` (ASSIGN_ID), `deletedStatus`, `createdTime`, `createdByType`, `createdBy`, `updatedTime`, `updatedByType`, `updatedBy`
- **Mappers**: Extend `BaseMapper<Entity>`, annotate with `@Mapper`
- **Mapper XML**: Empty body, namespace = mapper interface FQCN
- **Services**: Extend `ServiceImpl<Mapper, Entity>`, use `@Service @RequiredArgsConstructor`, add `@ConditionalOnProperty` for modules
- **Dict classes**: Implement `BaseDict`, use `@Dict` + `@DictData` annotations, `short` constants, `valid()` method
- **Enum classes**: Use `@Getter`, String `codeValue` + String `codeDesc`, `parseFromCodeValue()` static method
- **Package base**: `cn.projectan.strix`
- **Reserved words**: Wrap with backticks in `@TableField("`status`")` — applies to: status, key, name, group, type, action, order

---

## Task 1: SQL Migration Script

**Files:**
- Create: `docs/sql/workflow_tables.sql`

- [ ] **Step 1: Create the SQL migration file**

Create `docs/sql/workflow_tables.sql` with all 10 workflow tables:

`sql
-- ============================================================
-- Strix Workflow System - Database Schema
-- ============================================================

-- 1. wf_definition — 工作流定义
CREATE TABLE wf_definition (
    id              VARCHAR(32)     NOT NULL        COMMENT '主键',
    key           VARCHAR(128)    NOT NULL        COMMENT '流程唯一标识',
    
ame          VARCHAR(255)    NOT NULL        COMMENT '流程名称',
    description     VARCHAR(512)    DEFAULT NULL    COMMENT '流程描述',
    icon            VARCHAR(128)    DEFAULT NULL    COMMENT '图标标识',
    category        VARCHAR(64)     DEFAULT NULL    COMMENT '分类',
    status        SMALLINT        NOT NULL DEFAULT 1 COMMENT '状态 1=启用 2=停用',
    current_version INT             DEFAULT NULL    COMMENT '当前发布版本号',
    published_version_id VARCHAR(32) DEFAULT NULL   COMMENT '当前已发布版本 ID',
    deleted_status  SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识 0=正常 1=删除',
    created_time    DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by      VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time    DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by      VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_key (deleted_status, key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流定义';

-- 2. wf_definition_version — 版本快照
CREATE TABLE wf_definition_version (
    id              VARCHAR(32)     NOT NULL        COMMENT '主键',
    definition_id   VARCHAR(32)     NOT NULL        COMMENT '所属流程定义',
    version         INT             NOT NULL        COMMENT '版本号',
    status        SMALLINT        NOT NULL DEFAULT 1 COMMENT '状态 1=草稿 2=已发布 3=已废弃',
    graph_json      LONGTEXT        DEFAULT NULL    COMMENT '完整 DAG 图定义 (JSON)',
    change_log      VARCHAR(512)    DEFAULT NULL    COMMENT '变更说明',
    published_at    DATETIME        DEFAULT NULL    COMMENT '发布时间',
    published_by    VARCHAR(32)     DEFAULT NULL    COMMENT '发布人',
    deleted_status  SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识',
    created_time    DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by      VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time    DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by      VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_definition_id (definition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流定义版本';

-- 3. wf_instance — 流程实例
CREATE TABLE wf_instance (
    id                  VARCHAR(32)     NOT NULL        COMMENT '主键',
    definition_id       VARCHAR(32)     NOT NULL        COMMENT '流程定义',
    version_id          VARCHAR(32)     NOT NULL        COMMENT '绑定的版本快照',
    title               VARCHAR(255)    DEFAULT NULL    COMMENT '实例标题',
    biz_type            VARCHAR(128)    DEFAULT NULL    COMMENT '业务类型标识',
    biz_id              VARCHAR(64)     DEFAULT NULL    COMMENT '业务实体 ID',
    status            SMALLINT        NOT NULL DEFAULT 1 COMMENT '状态 1=进行中 2=已完成 3=已拒绝 4=已撤销 5=已终止',
    priority            SMALLINT        NOT NULL DEFAULT 2 COMMENT '优先级 1=低 2=中 3=高 4=紧急',
    initiator_id        VARCHAR(32)     NOT NULL        COMMENT '发起人 ID',
    parent_instance_id  VARCHAR(32)     DEFAULT NULL    COMMENT '父实例 ID（子流程）',
    parent_node_id      VARCHAR(64)     DEFAULT NULL    COMMENT '父实例中的子流程节点 ID',
    started_at          DATETIME        DEFAULT NULL    COMMENT '开始时间',
    ended_at            DATETIME        DEFAULT NULL    COMMENT '结束时间',
    end_reason          VARCHAR(512)    DEFAULT NULL    COMMENT '终止原因',
    deleted_status      SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识',
    created_time        DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type     SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by          VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time        DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type     SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by          VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_definition_id (definition_id),
    KEY idx_initiator (initiator_id),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流实例';

-- 4. wf_token — 执行令牌
CREATE TABLE wf_token (
    id              VARCHAR(32)     NOT NULL        COMMENT '主键',
    instance_id     VARCHAR(32)     NOT NULL        COMMENT '所属实例',
    parent_token_id VARCHAR(32)     DEFAULT NULL    COMMENT '父令牌（并行分支场景）',
    current_node_id VARCHAR(64)     DEFAULT NULL    COMMENT '当前停留节点 ID',
    status        SMALLINT        NOT NULL DEFAULT 1 COMMENT '状态 1=活跃 2=等待 3=已完成 4=已终止',
    arrived_at      DATETIME        DEFAULT NULL    COMMENT '到达当前节点时间',
    completed_at    DATETIME        DEFAULT NULL    COMMENT '离开当前节点时间',
    deleted_status  SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识',
    created_time    DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by      VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time    DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by      VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_instance_id (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流执行令牌';

-- 5. wf_task — 待办任务
CREATE TABLE wf_task (
    id              VARCHAR(32)     NOT NULL        COMMENT '主键',
    instance_id     VARCHAR(32)     NOT NULL        COMMENT '所属实例',
    token_id        VARCHAR(32)     NOT NULL        COMMENT '所属 Token',
    node_id         VARCHAR(64)     NOT NULL        COMMENT 'DAG 节点 ID',
    	ype          SMALLINT        NOT NULL        COMMENT '类型 1=审批 2=抄送 3=自定义',
    approval_mode   SMALLINT        DEFAULT NULL    COMMENT '审批模式 1=任一 2=全部 3=顺序',
    status        SMALLINT        NOT NULL DEFAULT 1 COMMENT '状态 1=待处理 2=处理中 3=已完成 4=已取消',
    result          SMALLINT        NOT NULL DEFAULT 0 COMMENT '结果 0=待定 1=通过 2=拒绝 3=回退 4=转办',
    seq_order       INT             DEFAULT NULL    COMMENT '顺序模式当前执行序号',
    deadline        DATETIME        DEFAULT NULL    COMMENT '截止时间',
    deleted_status  SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识',
    created_time    DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by      VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time    DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by      VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_instance_id (instance_id),
    KEY idx_token_id (token_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流待办任务';

-- 6. wf_task_assignee — 任务处理人
CREATE TABLE wf_task_assignee (
    id              VARCHAR(32)     NOT NULL        COMMENT '主键',
    task_id         VARCHAR(32)     NOT NULL        COMMENT '所属任务',
    assignee_id     VARCHAR(32)     NOT NULL        COMMENT '处理人 ID',
    assignee_type   SMALLINT        NOT NULL        COMMENT '类型 1=指定人 2=角色展开 3=加签 4=转办 5=代理',
    seq_order       INT             DEFAULT NULL    COMMENT '顺序模式序号',
    status        SMALLINT        NOT NULL DEFAULT 1 COMMENT '状态 1=待处理 2=已处理 3=已跳过 4=已取消',
    ction        SMALLINT        DEFAULT NULL    COMMENT '操作 1=通过 2=拒绝 3=回退 4=转办 5=加签 6=降签',
    comment         VARCHAR(1024)   DEFAULT NULL    COMMENT '审批意见',
    operated_at     DATETIME        DEFAULT NULL    COMMENT '操作时间',
    delegated_from  VARCHAR(32)     DEFAULT NULL    COMMENT '代理来源人 ID',
    deleted_status  SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识',
    created_time    DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by      VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time    DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by      VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_assignee_id (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流任务处理人';

-- 7. wf_comment — 评论/意见
CREATE TABLE wf_comment (
    id              VARCHAR(32)     NOT NULL        COMMENT '主键',
    instance_id     VARCHAR(32)     NOT NULL        COMMENT '所属实例',
    task_id         VARCHAR(32)     DEFAULT NULL    COMMENT '关联任务（可选）',
    author_id       VARCHAR(32)     NOT NULL        COMMENT '评论人',
    content         VARCHAR(2048)   NOT NULL        COMMENT '评论内容',
    	ype          SMALLINT        NOT NULL        COMMENT '类型 1=审批意见 2=讨论 3=系统',
    deleted_status  SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识',
    created_time    DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by      VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time    DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by      VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_instance_id (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流评论';

-- 8. wf_timer — 定时器/SLA
CREATE TABLE wf_timer (
    id              VARCHAR(32)     NOT NULL        COMMENT '主键',
    instance_id     VARCHAR(32)     NOT NULL        COMMENT '所属实例',
    token_id        VARCHAR(32)     DEFAULT NULL    COMMENT '所属 Token',
    node_id         VARCHAR(64)     DEFAULT NULL    COMMENT 'DAG 节点 ID',
    	ype          SMALLINT        NOT NULL        COMMENT '类型 1=延迟 2=超时 3=催办 4=SLA',
    fire_at         DATETIME        NOT NULL        COMMENT '触发时间',
    ction        VARCHAR(32)     DEFAULT NULL    COMMENT '超时动作',
    action_config   TEXT            DEFAULT NULL    COMMENT '动作配置 (JSON)',
    repeat_interval BIGINT          DEFAULT NULL    COMMENT '催办间隔（秒）',
    max_repeat      INT             DEFAULT NULL    COMMENT '最大催办次数',
    current_repeat  INT             DEFAULT 0       COMMENT '当前已催办次数',
    status        SMALLINT        NOT NULL DEFAULT 1 COMMENT '状态 1=等待 2=已触发 3=已取消',
    deleted_status  SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识',
    created_time    DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by      VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time    DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by      VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_instance_id (instance_id),
    KEY idx_fire_at (fire_at),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流定时器';

-- 9. wf_instance_var — 流程实例变量
CREATE TABLE wf_instance_var (
    id              VARCHAR(32)     NOT NULL        COMMENT '主键',
    instance_id     VARCHAR(32)     NOT NULL        COMMENT '所属实例',
    var_key         VARCHAR(128)    NOT NULL        COMMENT '变量名',
    var_value       TEXT            DEFAULT NULL    COMMENT '变量值 (JSON)',
    var_type        VARCHAR(16)     NOT NULL        COMMENT '数据类型 STRING/NUMBER/BOOLEAN/JSON',
    deleted_status  SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识',
    created_time    DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by      VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time    DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by      VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_instance_var (instance_id, var_key, deleted_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流实例变量';

-- 10. wf_delegation — 代理人设置
CREATE TABLE wf_delegation (
    id              VARCHAR(32)     NOT NULL        COMMENT '主键',
    delegator_id    VARCHAR(32)     NOT NULL        COMMENT '委托人 ID',
    delegate_id     VARCHAR(32)     NOT NULL        COMMENT '代理人 ID',
    start_time      DATETIME        NOT NULL        COMMENT '有效期开始',
    end_time        DATETIME        NOT NULL        COMMENT '有效期结束',
    definition_id   VARCHAR(32)     DEFAULT NULL    COMMENT '限定特定流程（可选）',
    status        SMALLINT        NOT NULL DEFAULT 1 COMMENT '状态 1=有效 2=已过期 3=已撤销',
    deleted_status  SMALLINT        NOT NULL DEFAULT 0 COMMENT '删除标识',
    created_time    DATETIME        DEFAULT NULL    COMMENT '创建时间',
    created_by_type SMALLINT        DEFAULT NULL    COMMENT '创建人类型',
    created_by      VARCHAR(32)     DEFAULT NULL    COMMENT '创建人',
    updated_time    DATETIME        DEFAULT NULL    COMMENT '更新时间',
    updated_by_type SMALLINT        DEFAULT NULL    COMMENT '更新人类型',
    updated_by      VARCHAR(32)     DEFAULT NULL    COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_delegator (delegator_id),
    KEY idx_delegate (delegate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流代理人设置';
`

- [ ] **Step 2: Commit**

`ash
git add docs/sql/workflow_tables.sql
git commit -m "feat(workflow): add SQL schema for 10 workflow tables

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 2: Workflow Dict Constants

Create 16 Dict constant classes for all Short-valued DB fields. All follow the same pattern as existing `JobStatus`.

**Files:**
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfDefinitionStatus.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfVersionStatus.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfInstanceStatus.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfInstancePriority.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfTokenStatus.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfTaskType.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfApprovalMode.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfTaskStatus.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfTaskResult.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfAssigneeType.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfAssigneeStatus.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfAssigneeAction.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfCommentType.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfTimerType.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfTimerStatus.java`
- Create: `src/main/java/cn/projectan/strix/model/dict/workflow/WfDelegationStatus.java`

- [ ] **Step 1: Create all 16 Dict classes**

Each Dict class follows this exact pattern (reference: `model/dict/system/JobStatus.java`):

`java
package cn.projectan.strix.model.dict.workflow;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import cn.projectan.strix.model.dict.system.DictDataStyle;
import io.swagger.v3.oas.annotations.media.Schema;

@Dict(key = "WfDefinitionStatus", value = "工作流-定义状态")
@Schema(description = "工作流-定义状态")
public class WfDefinitionStatus implements BaseDict {

    @DictData(label = "启用", sort = 1, style = DictDataStyle.SUCCESS)
    public static final short ENABLED = 1;

    @DictData(label = "停用", sort = 2, style = DictDataStyle.ERROR)
    public static final short DISABLED = 2;

    public static boolean valid(short value) {
        return value == ENABLED || value == DISABLED;
    }
}
`

Create each class with these specific values:

**WfVersionStatus.java:**
`java
@Dict(key = "WfVersionStatus", value = "工作流-版本状态")
@Schema(description = "工作流-版本状态")
public class WfVersionStatus implements BaseDict {
    @DictData(label = "草稿", sort = 1, style = DictDataStyle.DEFAULT)
    public static final short DRAFT = 1;
    @DictData(label = "已发布", sort = 2, style = DictDataStyle.SUCCESS)
    public static final short PUBLISHED = 2;
    @DictData(label = "已废弃", sort = 3, style = DictDataStyle.ERROR)
    public static final short DEPRECATED = 3;
    public static boolean valid(short value) { return value >= DRAFT && value <= DEPRECATED; }
}
`

**WfInstanceStatus.java:**
`java
@Dict(key = "WfInstanceStatus", value = "工作流-实例状态")
@Schema(description = "工作流-实例状态")
public class WfInstanceStatus implements BaseDict {
    @DictData(label = "进行中", sort = 1, style = DictDataStyle.PRIMARY)
    public static final short RUNNING = 1;
    @DictData(label = "已完成", sort = 2, style = DictDataStyle.SUCCESS)
    public static final short COMPLETED = 2;
    @DictData(label = "已拒绝", sort = 3, style = DictDataStyle.ERROR)
    public static final short REJECTED = 3;
    @DictData(label = "已撤销", sort = 4, style = DictDataStyle.WARNING)
    public static final short CANCELLED = 4;
    @DictData(label = "已终止", sort = 5, style = DictDataStyle.ERROR)
    public static final short TERMINATED = 5;
    public static boolean valid(short value) { return value >= RUNNING && value <= TERMINATED; }
}
`

**WfInstancePriority.java:**
`java
@Dict(key = "WfInstancePriority", value = "工作流-实例优先级")
@Schema(description = "工作流-实例优先级")
public class WfInstancePriority implements BaseDict {
    @DictData(label = "低", sort = 1, style = DictDataStyle.DEFAULT)
    public static final short LOW = 1;
    @DictData(label = "中", sort = 2, style = DictDataStyle.PRIMARY)
    public static final short MEDIUM = 2;
    @DictData(label = "高", sort = 3, style = DictDataStyle.WARNING)
    public static final short HIGH = 3;
    @DictData(label = "紧急", sort = 4, style = DictDataStyle.ERROR)
    public static final short URGENT = 4;
    public static boolean valid(short value) { return value >= LOW && value <= URGENT; }
}
`

**WfTokenStatus.java:**
`java
@Dict(key = "WfTokenStatus", value = "工作流-令牌状态")
@Schema(description = "工作流-令牌状态")
public class WfTokenStatus implements BaseDict {
    @DictData(label = "活跃", sort = 1, style = DictDataStyle.SUCCESS)
    public static final short ACTIVE = 1;
    @DictData(label = "等待", sort = 2, style = DictDataStyle.WARNING)
    public static final short WAITING = 2;
    @DictData(label = "已完成", sort = 3, style = DictDataStyle.DEFAULT)
    public static final short COMPLETED = 3;
    @DictData(label = "已终止", sort = 4, style = DictDataStyle.ERROR)
    public static final short TERMINATED = 4;
    public static boolean valid(short value) { return value >= ACTIVE && value <= TERMINATED; }
}
`

**WfTaskType.java:**
`java
@Dict(key = "WfTaskType", value = "工作流-任务类型")
@Schema(description = "工作流-任务类型")
public class WfTaskType implements BaseDict {
    @DictData(label = "审批", sort = 1, style = DictDataStyle.PRIMARY)
    public static final short APPROVAL = 1;
    @DictData(label = "抄送", sort = 2, style = DictDataStyle.DEFAULT)
    public static final short CC = 2;
    @DictData(label = "自定义", sort = 3, style = DictDataStyle.WARNING)
    public static final short CUSTOM = 3;
    public static boolean valid(short value) { return value >= APPROVAL && value <= CUSTOM; }
}
`

**WfApprovalMode.java:**
`java
@Dict(key = "WfApprovalMode", value = "工作流-审批模式")
@Schema(description = "工作流-审批模式")
public class WfApprovalMode implements BaseDict {
    @DictData(label = "任一通过", sort = 1, style = DictDataStyle.DEFAULT)
    public static final short ANY = 1;
    @DictData(label = "全部通过", sort = 2, style = DictDataStyle.PRIMARY)
    public static final short ALL = 2;
    @DictData(label = "顺序审批", sort = 3, style = DictDataStyle.WARNING)
    public static final short SEQ = 3;
    public static boolean valid(short value) { return value >= ANY && value <= SEQ; }
}
`

**WfTaskStatus.java:**
`java
@Dict(key = "WfTaskStatus", value = "工作流-任务状态")
@Schema(description = "工作流-任务状态")
public class WfTaskStatus implements BaseDict {
    @DictData(label = "待处理", sort = 1, style = DictDataStyle.WARNING)
    public static final short PENDING = 1;
    @DictData(label = "处理中", sort = 2, style = DictDataStyle.PRIMARY)
    public static final short IN_PROGRESS = 2;
    @DictData(label = "已完成", sort = 3, style = DictDataStyle.SUCCESS)
    public static final short COMPLETED = 3;
    @DictData(label = "已取消", sort = 4, style = DictDataStyle.ERROR)
    public static final short CANCELLED = 4;
    public static boolean valid(short value) { return value >= PENDING && value <= CANCELLED; }
}
`

**WfTaskResult.java:**
`java
@Dict(key = "WfTaskResult", value = "工作流-任务结果")
@Schema(description = "工作流-任务结果")
public class WfTaskResult implements BaseDict {
    @DictData(label = "待定", sort = 0, style = DictDataStyle.DEFAULT)
    public static final short PENDING = 0;
    @DictData(label = "通过", sort = 1, style = DictDataStyle.SUCCESS)
    public static final short APPROVED = 1;
    @DictData(label = "拒绝", sort = 2, style = DictDataStyle.ERROR)
    public static final short REJECTED = 2;
    @DictData(label = "回退", sort = 3, style = DictDataStyle.WARNING)
    public static final short RETURNED = 3;
    @DictData(label = "转办", sort = 4, style = DictDataStyle.PRIMARY)
    public static final short DELEGATED = 4;
    public static boolean valid(short value) { return value >= PENDING && value <= DELEGATED; }
}
`

**WfAssigneeType.java:**
`java
@Dict(key = "WfAssigneeType", value = "工作流-处理人类型")
@Schema(description = "工作流-处理人类型")
public class WfAssigneeType implements BaseDict {
    @DictData(label = "指定人", sort = 1, style = DictDataStyle.DEFAULT)
    public static final short DESIGNATED = 1;
    @DictData(label = "角色展开", sort = 2, style = DictDataStyle.PRIMARY)
    public static final short ROLE_EXPANDED = 2;
    @DictData(label = "加签", sort = 3, style = DictDataStyle.WARNING)
    public static final short COUNTERSIGNED = 3;
    @DictData(label = "转办", sort = 4, style = DictDataStyle.PRIMARY)
    public static final short DELEGATED = 4;
    @DictData(label = "代理", sort = 5, style = DictDataStyle.DEFAULT)
    public static final short PROXY = 5;
    public static boolean valid(short value) { return value >= DESIGNATED && value <= PROXY; }
}
`

**WfAssigneeStatus.java:**
`java
@Dict(key = "WfAssigneeStatus", value = "工作流-处理人状态")
@Schema(description = "工作流-处理人状态")
public class WfAssigneeStatus implements BaseDict {
    @DictData(label = "待处理", sort = 1, style = DictDataStyle.WARNING)
    public static final short PENDING = 1;
    @DictData(label = "已处理", sort = 2, style = DictDataStyle.SUCCESS)
    public static final short PROCESSED = 2;
    @DictData(label = "已跳过", sort = 3, style = DictDataStyle.DEFAULT)
    public static final short SKIPPED = 3;
    @DictData(label = "已取消", sort = 4, style = DictDataStyle.ERROR)
    public static final short CANCELLED = 4;
    public static boolean valid(short value) { return value >= PENDING && value <= CANCELLED; }
}
`

**WfAssigneeAction.java:**
`java
@Dict(key = "WfAssigneeAction", value = "工作流-处理人操作")
@Schema(description = "工作流-处理人操作")
public class WfAssigneeAction implements BaseDict {
    @DictData(label = "通过", sort = 1, style = DictDataStyle.SUCCESS)
    public static final short APPROVE = 1;
    @DictData(label = "拒绝", sort = 2, style = DictDataStyle.ERROR)
    public static final short REJECT = 2;
    @DictData(label = "回退", sort = 3, style = DictDataStyle.WARNING)
    public static final short RETURN = 3;
    @DictData(label = "转办", sort = 4, style = DictDataStyle.PRIMARY)
    public static final short DELEGATE = 4;
    @DictData(label = "加签", sort = 5, style = DictDataStyle.PRIMARY)
    public static final short COUNTERSIGN = 5;
    @DictData(label = "降签", sort = 6, style = DictDataStyle.WARNING)
    public static final short REMOVE_SIGN = 6;
    public static boolean valid(short value) { return value >= APPROVE && value <= REMOVE_SIGN; }
}
`

**WfCommentType.java:**
`java
@Dict(key = "WfCommentType", value = "工作流-评论类型")
@Schema(description = "工作流-评论类型")
public class WfCommentType implements BaseDict {
    @DictData(label = "审批意见", sort = 1, style = DictDataStyle.PRIMARY)
    public static final short APPROVAL_OPINION = 1;
    @DictData(label = "讨论", sort = 2, style = DictDataStyle.DEFAULT)
    public static final short DISCUSSION = 2;
    @DictData(label = "系统", sort = 3, style = DictDataStyle.WARNING)
    public static final short SYSTEM = 3;
    public static boolean valid(short value) { return value >= APPROVAL_OPINION && value <= SYSTEM; }
}
`

**WfTimerType.java:**
`java
@Dict(key = "WfTimerType", value = "工作流-定时器类型")
@Schema(description = "工作流-定时器类型")
public class WfTimerType implements BaseDict {
    @DictData(label = "延迟", sort = 1, style = DictDataStyle.DEFAULT)
    public static final short DELAY = 1;
    @DictData(label = "超时", sort = 2, style = DictDataStyle.WARNING)
    public static final short TIMEOUT = 2;
    @DictData(label = "催办", sort = 3, style = DictDataStyle.PRIMARY)
    public static final short REMINDER = 3;
    @DictData(label = "SLA", sort = 4, style = DictDataStyle.ERROR)
    public static final short SLA = 4;
    public static boolean valid(short value) { return value >= DELAY && value <= SLA; }
}
`

**WfTimerStatus.java:**
`java
@Dict(key = "WfTimerStatus", value = "工作流-定时器状态")
@Schema(description = "工作流-定时器状态")
public class WfTimerStatus implements BaseDict {
    @DictData(label = "等待", sort = 1, style = DictDataStyle.WARNING)
    public static final short WAITING = 1;
    @DictData(label = "已触发", sort = 2, style = DictDataStyle.SUCCESS)
    public static final short FIRED = 2;
    @DictData(label = "已取消", sort = 3, style = DictDataStyle.ERROR)
    public static final short CANCELLED = 3;
    public static boolean valid(short value) { return value >= WAITING && value <= CANCELLED; }
}
`

**WfDelegationStatus.java:**
`java
@Dict(key = "WfDelegationStatus", value = "工作流-代理状态")
@Schema(description = "工作流-代理状态")
public class WfDelegationStatus implements BaseDict {
    @DictData(label = "有效", sort = 1, style = DictDataStyle.SUCCESS)
    public static final short ACTIVE = 1;
    @DictData(label = "已过期", sort = 2, style = DictDataStyle.DEFAULT)
    public static final short EXPIRED = 2;
    @DictData(label = "已撤销", sort = 3, style = DictDataStyle.ERROR)
    public static final short REVOKED = 3;
    public static boolean valid(short value) { return value >= ACTIVE && value <= REVOKED; }
}
`

All classes use the same imports as `WfDefinitionStatus` shown above. Create the package directory `model/dict/workflow/` first.

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/model/dict/workflow/
git commit -m "feat(workflow): add 16 Dict constant classes for workflow status fields

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 3: Workflow Enum Classes

Create 3 enum classes for String-valued types used in graph JSON configs.

**Files:**
- Create: `src/main/java/cn/projectan/strix/model/enums/workflow/WfNodeType.java`
- Create: `src/main/java/cn/projectan/strix/model/enums/workflow/WfTimerAction.java`
- Create: `src/main/java/cn/projectan/strix/model/enums/workflow/WfVarType.java`

- [ ] **Step 1: Create WfNodeType enum**

`java
package cn.projectan.strix.model.enums.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "工作流节点类型")
public enum WfNodeType {

    START("START", "开始"),
    END("END", "结束"),
    APPROVAL("APPROVAL", "审批"),
    CC("CC", "抄送"),
    CONDITION("CONDITION", "条件分支"),
    CONDITION_GROUP("CONDITION_GROUP", "条件组"),
    PARALLEL("PARALLEL", "并行分支"),
    DELAY("DELAY", "延迟"),
    TRIGGER("TRIGGER", "触发器"),
    JUMP("JUMP", "跳转"),
    SUB_PROCESS("SUB_PROCESS", "子流程");

    private final String codeValue;
    private final String codeDesc;

    WfNodeType(String codeValue, String codeDesc) {
        this.codeValue = codeValue;
        this.codeDesc = codeDesc;
    }

    public static WfNodeType parseFromCodeValue(String codeValue) {
        for (WfNodeType e : WfNodeType.values()) {
            if (e.codeValue.equals(codeValue)) {
                return e;
            }
        }
        return null;
    }

    public static String getCodeDescByCodeValue(String codeValue) {
        WfNodeType enumItem = parseFromCodeValue(codeValue);
        return enumItem == null ? "" : enumItem.getCodeDesc();
    }

    public static boolean validateCodeValue(String codeValue) {
        return parseFromCodeValue(codeValue) != null;
    }

    /**
     * 是否为阻塞节点（需要外部信号才能继续）
     */
    public boolean isBlocking() {
        return this == APPROVAL || this == DELAY || this == SUB_PROCESS;
    }

    /**
     * 是否为自动节点（执行后立即流转）
     */
    public boolean isAutomatic() {
        return !isBlocking();
    }
}
`

- [ ] **Step 2: Create WfTimerAction enum**

`java
package cn.projectan.strix.model.enums.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "工作流定时器动作")
public enum WfTimerAction {

    AUTO_APPROVE("AUTO_APPROVE", "自动通过"),
    AUTO_REJECT("AUTO_REJECT", "自动拒绝"),
    DELEGATE("DELEGATE", "自动转办"),
    REMIND("REMIND", "仅提醒");

    private final String codeValue;
    private final String codeDesc;

    WfTimerAction(String codeValue, String codeDesc) {
        this.codeValue = codeValue;
        this.codeDesc = codeDesc;
    }

    public static WfTimerAction parseFromCodeValue(String codeValue) {
        for (WfTimerAction e : WfTimerAction.values()) {
            if (e.codeValue.equals(codeValue)) {
                return e;
            }
        }
        return null;
    }

    public static boolean validateCodeValue(String codeValue) {
        return parseFromCodeValue(codeValue) != null;
    }
}
`

- [ ] **Step 3: Create WfVarType enum**

`java
package cn.projectan.strix.model.enums.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "工作流变量类型")
public enum WfVarType {

    STRING("STRING", "字符串"),
    NUMBER("NUMBER", "数字"),
    BOOLEAN("BOOLEAN", "布尔"),
    JSON("JSON", "JSON 对象");

    private final String codeValue;
    private final String codeDesc;

    WfVarType(String codeValue, String codeDesc) {
        this.codeValue = codeValue;
        this.codeDesc = codeDesc;
    }

    public static WfVarType parseFromCodeValue(String codeValue) {
        for (WfVarType e : WfVarType.values()) {
            if (e.codeValue.equals(codeValue)) {
                return e;
            }
        }
        return null;
    }

    public static boolean validateCodeValue(String codeValue) {
        return parseFromCodeValue(codeValue) != null;
    }
}
`

- [ ] **Step 4: Commit**

`ash
git add src/main/java/cn/projectan/strix/model/enums/workflow/
git commit -m "feat(workflow): add WfNodeType, WfTimerAction, WfVarType enums

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 4: Entity Classes

Create 10 entity classes in `model/db/workflow/`. All extend `BaseModel<T>` and follow the Notification/Job entity pattern.

**Files:**
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfDefinition.java`
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfDefinitionVersion.java`
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfInstance.java`
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfToken.java`
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfTask.java`
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfTaskAssignee.java`
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfComment.java`
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfTimer.java`
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfInstanceVar.java`
- Create: `src/main/java/cn/projectan/strix/model/db/workflow/WfDelegation.java`

- [ ] **Step 1: Create WfDefinition entity**

`java
package cn.projectan.strix.model.db.workflow;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 工作流定义
 *
 * @see cn.projectan.strix.model.dict.workflow.WfDefinitionStatus
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_definition")
public class WfDefinition extends BaseModel<WfDefinition> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程唯一标识
     */
    @TableField("key")
    private String key;

    /**
     * 流程名称
     */
    @TableField("
ame")
    private String name;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 图标标识
     */
    private String icon;

    /**
     * 分类
     */
    private String category;

    /**
     * 状态
     *
     * @see cn.projectan.strix.model.dict.workflow.WfDefinitionStatus
     */
    @TableField("status")
    private Short status;

    /**
     * 当前发布版本号
     */
    private Integer currentVersion;

    /**
     * 当前已发布版本 ID
     */
    private String publishedVersionId;
}
`

- [ ] **Step 2: Create WfDefinitionVersion entity**

`java
package cn.projectan.strix.model.db.workflow;

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
 * 工作流定义版本
 *
 * @see cn.projectan.strix.model.dict.workflow.WfVersionStatus
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_definition_version")
public class WfDefinitionVersion extends BaseModel<WfDefinitionVersion> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属流程定义
     */
    private String definitionId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 状态
     *
     * @see cn.projectan.strix.model.dict.workflow.WfVersionStatus
     */
    @TableField("status")
    private Short status;

    /**
     * 完整 DAG 图定义 (JSON)
     */
    private String graphJson;

    /**
     * 变更说明
     */
    private String changeLog;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 发布人
     */
    private String publishedBy;
}
`

- [ ] **Step 3: Create WfInstance entity**

`java
package cn.projectan.strix.model.db.workflow;

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
 * 工作流实例
 *
 * @see cn.projectan.strix.model.dict.workflow.WfInstanceStatus
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_instance")
public class WfInstance extends BaseModel<WfInstance> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义 ID
     */
    private String definitionId;

    /**
     * 绑定的版本快照 ID
     */
    private String versionId;

    /**
     * 实例标题
     */
    private String title;

    /**
     * 业务类型标识
     */
    private String bizType;

    /**
     * 业务实体 ID
     */
    private String bizId;

    /**
     * 状态
     *
     * @see cn.projectan.strix.model.dict.workflow.WfInstanceStatus
     */
    @TableField("status")
    private Short status;

    /**
     * 优先级
     *
     * @see cn.projectan.strix.model.dict.workflow.WfInstancePriority
     */
    private Short priority;

    /**
     * 发起人 ID (SystemManager)
     */
    private String initiatorId;

    /**
     * 父实例 ID（子流程场景）
     */
    private String parentInstanceId;

    /**
     * 父实例中的子流程节点 ID
     */
    private String parentNodeId;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime endedAt;

    /**
     * 终止原因
     */
    private String endReason;
}
`

- [ ] **Step 4: Create WfToken entity**

`java
package cn.projectan.strix.model.db.workflow;

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
 * 工作流执行令牌
 *
 * @see cn.projectan.strix.model.dict.workflow.WfTokenStatus
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_token")
public class WfToken extends BaseModel<WfToken> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属实例
     */
    private String instanceId;

    /**
     * 父令牌（并行分支场景）
     */
    private String parentTokenId;

    /**
     * 当前停留节点 ID
     */
    private String currentNodeId;

    /**
     * 状态
     *
     * @see cn.projectan.strix.model.dict.workflow.WfTokenStatus
     */
    @TableField("status")
    private Short status;

    /**
     * 到达当前节点时间
     */
    private LocalDateTime arrivedAt;

    /**
     * 离开当前节点时间
     */
    private LocalDateTime completedAt;
}
`

- [ ] **Step 5: Create WfTask entity**

`java
package cn.projectan.strix.model.db.workflow;

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
 * 工作流待办任务
 *
 * @see cn.projectan.strix.model.dict.workflow.WfTaskStatus
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_task")
public class WfTask extends BaseModel<WfTask> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属实例
     */
    private String instanceId;

    /**
     * 所属 Token
     */
    private String tokenId;

    /**
     * DAG 节点 ID
     */
    private String nodeId;

    /**
     * 任务类型
     *
     * @see cn.projectan.strix.model.dict.workflow.WfTaskType
     */
    @TableField("	ype")
    private Short type;

    /**
     * 审批模式（仅审批任务）
     *
     * @see cn.projectan.strix.model.dict.workflow.WfApprovalMode
     */
    private Short approvalMode;

    /**
     * 任务状态
     *
     * @see cn.projectan.strix.model.dict.workflow.WfTaskStatus
     */
    @TableField("status")
    private Short status;

    /**
     * 任务结果
     *
     * @see cn.projectan.strix.model.dict.workflow.WfTaskResult
     */
    private Short result;

    /**
     * 顺序模式下的当前执行序号
     */
    private Integer seqOrder;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;
}
`

- [ ] **Step 6: Create WfTaskAssignee entity**

`java
package cn.projectan.strix.model.db.workflow;

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
 * 工作流任务处理人
 *
 * @see cn.projectan.strix.model.dict.workflow.WfAssigneeStatus
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_task_assignee")
public class WfTaskAssignee extends BaseModel<WfTaskAssignee> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属任务
     */
    private String taskId;

    /**
     * 处理人 ID (SystemManager)
     */
    private String assigneeId;

    /**
     * 处理人类型
     *
     * @see cn.projectan.strix.model.dict.workflow.WfAssigneeType
     */
    private Short assigneeType;

    /**
     * 顺序模式下的序号
     */
    private Integer seqOrder;

    /**
     * 状态
     *
     * @see cn.projectan.strix.model.dict.workflow.WfAssigneeStatus
     */
    @TableField("status")
    private Short status;

    /**
     * 操作
     *
     * @see cn.projectan.strix.model.dict.workflow.WfAssigneeAction
     */
    @TableField("ction")
    private Short action;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 操作时间
     */
    private LocalDateTime operatedAt;

    /**
     * 代理来源人 ID
     */
    private String delegatedFrom;
}
`

- [ ] **Step 7: Create WfComment entity**

`java
package cn.projectan.strix.model.db.workflow;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 工作流评论/意见
 *
 * @see cn.projectan.strix.model.dict.workflow.WfCommentType
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_comment")
public class WfComment extends BaseModel<WfComment> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属实例
     */
    private String instanceId;

    /**
     * 关联任务（可选）
     */
    private String taskId;

    /**
     * 评论人
     */
    private String authorId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论类型
     *
     * @see cn.projectan.strix.model.dict.workflow.WfCommentType
     */
    @TableField("	ype")
    private Short type;
}
`

- [ ] **Step 8: Create WfTimer entity**

`java
package cn.projectan.strix.model.db.workflow;

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
 * 工作流定时器/SLA
 *
 * @see cn.projectan.strix.model.dict.workflow.WfTimerType
 * @see cn.projectan.strix.model.dict.workflow.WfTimerStatus
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_timer")
public class WfTimer extends BaseModel<WfTimer> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属实例
     */
    private String instanceId;

    /**
     * 所属 Token
     */
    private String tokenId;

    /**
     * DAG 节点 ID
     */
    private String nodeId;

    /**
     * 定时器类型
     *
     * @see cn.projectan.strix.model.dict.workflow.WfTimerType
     */
    @TableField("	ype")
    private Short type;

    /**
     * 触发时间
     */
    private LocalDateTime fireAt;

    /**
     * 超时动作
     *
     * @see cn.projectan.strix.model.enums.workflow.WfTimerAction
     */
    @TableField("ction")
    private String action;

    /**
     * 动作配置 (JSON)
     */
    private String actionConfig;

    /**
     * 催办间隔（秒）
     */
    private Long repeatInterval;

    /**
     * 最大催办次数
     */
    private Integer maxRepeat;

    /**
     * 当前已催办次数
     */
    private Integer currentRepeat;

    /**
     * 状态
     *
     * @see cn.projectan.strix.model.dict.workflow.WfTimerStatus
     */
    @TableField("status")
    private Short status;
}
`

- [ ] **Step 9: Create WfInstanceVar entity**

`java
package cn.projectan.strix.model.db.workflow;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 工作流实例变量
 *
 * @see cn.projectan.strix.model.enums.workflow.WfVarType
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_instance_var")
public class WfInstanceVar extends BaseModel<WfInstanceVar> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属实例
     */
    private String instanceId;

    /**
     * 变量名
     */
    private String varKey;

    /**
     * 变量值 (JSON)
     */
    private String varValue;

    /**
     * 数据类型
     *
     * @see cn.projectan.strix.model.enums.workflow.WfVarType
     */
    private String varType;
}
`

- [ ] **Step 10: Create WfDelegation entity**

`java
package cn.projectan.strix.model.db.workflow;

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
 * 工作流代理人设置
 *
 * @see cn.projectan.strix.model.dict.workflow.WfDelegationStatus
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_delegation")
public class WfDelegation extends BaseModel<WfDelegation> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 委托人 ID (SystemManager)
     */
    private String delegatorId;

    /**
     * 代理人 ID (SystemManager)
     */
    private String delegateId;

    /**
     * 有效期开始
     */
    private LocalDateTime startTime;

    /**
     * 有效期结束
     */
    private LocalDateTime endTime;

    /**
     * 限定特定流程（可选）
     */
    private String definitionId;

    /**
     * 状态
     *
     * @see cn.projectan.strix.model.dict.workflow.WfDelegationStatus
     */
    @TableField("status")
    private Short status;
}
`

- [ ] **Step 11: Commit**

`ash
git add src/main/java/cn/projectan/strix/model/db/workflow/
git commit -m "feat(workflow): add 10 workflow entity classes

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 5: Mapper Interfaces + XML Files

Create 10 mapper interfaces and 10 empty mapper XML files.

**Files:**
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfDefinitionMapper.java`
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfDefinitionVersionMapper.java`
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfInstanceMapper.java`
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfTokenMapper.java`
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfTaskMapper.java`
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfTaskAssigneeMapper.java`
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfCommentMapper.java`
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfTimerMapper.java`
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfInstanceVarMapper.java`
- Create: `src/main/java/cn/projectan/strix/mapper/workflow/WfDelegationMapper.java`
- Create: `src/main/resources/mapper/workflow/WfDefinitionMapper.xml`
- Create: `src/main/resources/mapper/workflow/WfDefinitionVersionMapper.xml`
- Create: `src/main/resources/mapper/workflow/WfInstanceMapper.xml`
- Create: `src/main/resources/mapper/workflow/WfTokenMapper.xml`
- Create: `src/main/resources/mapper/workflow/WfTaskMapper.xml`
- Create: `src/main/resources/mapper/workflow/WfTaskAssigneeMapper.xml`
- Create: `src/main/resources/mapper/workflow/WfCommentMapper.xml`
- Create: `src/main/resources/mapper/workflow/WfTimerMapper.xml`
- Create: `src/main/resources/mapper/workflow/WfInstanceVarMapper.xml`
- Create: `src/main/resources/mapper/workflow/WfDelegationMapper.xml`

- [ ] **Step 1: Create all 10 mapper interfaces**

Each mapper follows this pattern (reference: `mapper/system/NotificationMapper.java`):

**WfDefinitionMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfDefinitionMapper extends BaseMapper<WfDefinition> {
}
`

**WfDefinitionVersionMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfDefinitionVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfDefinitionVersionMapper extends BaseMapper<WfDefinitionVersion> {
}
`

**WfInstanceMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfInstance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfInstanceMapper extends BaseMapper<WfInstance> {
}
`

**WfTokenMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfTokenMapper extends BaseMapper<WfToken> {
}
`

**WfTaskMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfTaskMapper extends BaseMapper<WfTask> {
}
`

**WfTaskAssigneeMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfTaskAssignee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfTaskAssigneeMapper extends BaseMapper<WfTaskAssignee> {
}
`

**WfCommentMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfCommentMapper extends BaseMapper<WfComment> {
}
`

**WfTimerMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfTimer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfTimerMapper extends BaseMapper<WfTimer> {
}
`

**WfInstanceVarMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfInstanceVar;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfInstanceVarMapper extends BaseMapper<WfInstanceVar> {
}
`

**WfDelegationMapper.java:**
`java
package cn.projectan.strix.mapper.workflow;

import cn.projectan.strix.model.db.workflow.WfDelegation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfDelegationMapper extends BaseMapper<WfDelegation> {
}
`

- [ ] **Step 2: Create all 10 mapper XML files**

Each XML file follows this pattern (reference: `resources/mapper/system/NotificationMapper.xml`).
Create the directory `src/main/resources/mapper/workflow/` first.

**WfDefinitionMapper.xml:**
`xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!--suppress ALL -->
<mapper namespace="cn.projectan.strix.mapper.workflow.WfDefinitionMapper">

</mapper>
`

Repeat the same pattern for all 10 XML files, changing only the namespace:
- `cn.projectan.strix.mapper.workflow.WfDefinitionVersionMapper`
- `cn.projectan.strix.mapper.workflow.WfInstanceMapper`
- `cn.projectan.strix.mapper.workflow.WfTokenMapper`
- `cn.projectan.strix.mapper.workflow.WfTaskMapper`
- `cn.projectan.strix.mapper.workflow.WfTaskAssigneeMapper`
- `cn.projectan.strix.mapper.workflow.WfCommentMapper`
- `cn.projectan.strix.mapper.workflow.WfTimerMapper`
- `cn.projectan.strix.mapper.workflow.WfInstanceVarMapper`
- `cn.projectan.strix.mapper.workflow.WfDelegationMapper`

- [ ] **Step 3: Commit**

`ash
git add src/main/java/cn/projectan/strix/mapper/workflow/ src/main/resources/mapper/workflow/
git commit -m "feat(workflow): add 10 mapper interfaces and XML files

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 6: Graph Model + Config Classes

Create the DAG graph model POJOs and node config classes for JSON serialization. These represent the `graph_json` structure stored in `wf_definition_version`.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/WorkflowGraph.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/WorkflowNode.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/WorkflowEdge.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/NodePosition.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/NodeExecutionResult.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/AssigneeConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/TimeoutConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/ReminderConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/NodeTriggerConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/NotifyTemplateConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/ApprovalNodeConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/CcNodeConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/ConditionItem.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/ConditionBranch.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/ConditionNodeConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/ParallelBranch.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/ParallelNodeConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/DelayNodeConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/TriggerNodeConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/JumpNodeConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/SubProcessNodeConfig.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/model/config/EndNodeConfig.java`

- [ ] **Step 1: Create core graph model classes**

**WorkflowGraph.java:**
`java
package cn.projectan.strix.core.module.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowGraph {

    private List<WorkflowNode> nodes = new ArrayList<>();
    private List<WorkflowEdge> edges = new ArrayList<>();

    /**
     * 根据节点 ID 查找节点
     */
    public Optional<WorkflowNode> findNode(String nodeId) {
        return nodes.stream().filter(n -> n.getId().equals(nodeId)).findFirst();
    }

    /**
     * 查找指定节点的所有下游边
     */
    public List<WorkflowEdge> findOutgoingEdges(String nodeId) {
        return edges.stream().filter(e -> e.getSource().equals(nodeId)).toList();
    }

    /**
     * 查找指定节点的所有上游边
     */
    public List<WorkflowEdge> findIncomingEdges(String nodeId) {
        return edges.stream().filter(e -> e.getTarget().equals(nodeId)).toList();
    }

    /**
     * 查找开始节点
     */
    public Optional<WorkflowNode> findStartNode() {
        return nodes.stream().filter(n -> "START".equals(n.getType())).findFirst();
    }

    /**
     * 获取指定节点的所有下游节点 ID
     */
    public List<String> findNextNodeIds(String nodeId) {
        return findOutgoingEdges(nodeId).stream().map(WorkflowEdge::getTarget).toList();
    }
}
`

**WorkflowNode.java:**
`java
package cn.projectan.strix.core.module.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowNode {

    /**
     * 节点唯一 ID (图内唯一)
     */
    private String id;

    /**
     * 节点类型
     *
     * @see cn.projectan.strix.model.enums.workflow.WfNodeType
     */
    private String type;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 设计器中的位置
     */
    private NodePosition position;

    /**
     * 节点特定配置 (原始 JSON，由各 NodeHandler 解析为具体类型)
     */
    private JsonNode config;
}
`

**WorkflowEdge.java:**
`java
package cn.projectan.strix.core.module.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowEdge {

    private String id;
    private String source;
    private String target;
}
`

**NodePosition.java:**
`java
package cn.projectan.strix.core.module.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodePosition {

    private int x;
    private int y;
}
`

**NodeExecutionResult.java:**
`java
package cn.projectan.strix.core.module.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionResult {

    public enum Status {
        /** 自动流转到下一节点 */
        CONTINUE,
        /** 阻塞等待（审批/延迟/子流程） */
        WAIT,
        /** 并行分支 Fork */
        FORK,
        /** 执行出错 */
        ERROR
    }

    private Status status;

    /** CONTINUE: 下一节点 ID 列表 */
    private List<String> nextNodeIds = new ArrayList<>();

    /** FORK: 各分支的节点 ID 列表 */
    private List<List<String>> forkBranches = new ArrayList<>();

    /** 写入流程变量 */
    private Map<String, Object> variables = new HashMap<>();

    /** ERROR: 错误信息 */
    private String errorMessage;

    public static NodeExecutionResult continueWith(List<String> nextNodeIds) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setStatus(Status.CONTINUE);
        result.setNextNodeIds(nextNodeIds);
        return result;
    }

    public static NodeExecutionResult waitHere() {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setStatus(Status.WAIT);
        return result;
    }

    public static NodeExecutionResult fork(List<List<String>> branches) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setStatus(Status.FORK);
        result.setForkBranches(branches);
        return result;
    }

    public static NodeExecutionResult error(String message) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setStatus(Status.ERROR);
        result.setErrorMessage(message);
        return result;
    }
}
`

- [ ] **Step 2: Create shared config model classes**

**AssigneeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssigneeConfig {

    /** 类型: MANAGER / ROLE */
    private String type;

    /** SystemManager ID 或 SystemRole ID */
    private String id;

    /** 显示名称（设计器用） */
    private String name;
}
`

**TimeoutConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeoutConfig {

    /** ISO 8601 Duration (如 PT24H) */
    private String duration;

    /** 超时动作: AUTO_APPROVE / AUTO_REJECT / DELEGATE / REMIND */
    private String action;

    /** DELEGATE 时的目标人 ID */
    private String delegateId;
}
`

**ReminderConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReminderConfig {

    /** ISO 8601 Duration 催办间隔 */
    private String interval;

    /** 最大催办次数 */
    private int maxCount;
}
`

**NodeTriggerConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeTriggerConfig {

    /** 进入节点前触发的 trigger key */
    private String onBeforeEnter;

    /** 进入节点后触发的 trigger key */
    private String onAfterEnter;

    /** 离开节点前触发的 trigger key */
    private String onBeforeLeave;

    /** 离开节点后触发的 trigger key */
    private String onAfterLeave;
}
`

**NotifyTemplateConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotifyTemplateConfig {

    /** 通知标题模板（支持变量占位符 ${varName}） */
    private String title;

    /** 通知内容模板 */
    private String content;
}
`

- [ ] **Step 3: Create node-specific config classes**

**ApprovalNodeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalNodeConfig {

    /** 审批模式: ANY / ALL / SEQ */
    private String approvalMode;

    /** 审批人列表 */
    private List<AssigneeConfig> assignees = new ArrayList<>();

    /** 超时配置 */
    private TimeoutConfig timeout;

    /** 催办配置 */
    private ReminderConfig reminder;

    /** 生命周期触发器 */
    private NodeTriggerConfig triggers;

    /** 通知模板 */
    private NotifyTemplateConfig notifyTemplate;

    /** 通知渠道: SITE / SMS */
    private List<String> notifyChannels;
}
`

**CcNodeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CcNodeConfig {

    /** 抄送人列表 */
    private List<AssigneeConfig> receivers = new ArrayList<>();

    /** 通知模板 */
    private NotifyTemplateConfig notifyTemplate;

    /** 通知渠道 */
    private List<String> notifyChannels;

    /** 生命周期触发器 */
    private NodeTriggerConfig triggers;
}
`

**ConditionItem.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionItem {

    /** 变量字段名 */
    private String field;

    /** 运算符: EQ / NEQ / GT / GTE / LT / LTE / IN / NOT_IN / CONTAINS / IS_NULL / IS_NOT_NULL */
    private String op;

    /** 比较值 */
    private Object value;
}
`

**ConditionBranch.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionBranch {

    /** 条件列表 */
    private List<ConditionItem> conditions = new ArrayList<>();

    /** 逻辑组合: AND / OR */
    private String logic;

    /** 匹配时跳转的目标节点 ID */
    private String targetNodeId;
}
`

**ConditionNodeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionNodeConfig {

    /** 条件分支规则列表 */
    private List<ConditionBranch> branches = new ArrayList<>();

    /** 默认目标节点（所有条件都不匹配时） */
    private String defaultTargetNodeId;

    /** 生命周期触发器 */
    private NodeTriggerConfig triggers;
}
`

**ParallelBranch.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParallelBranch {

    /** 分支 ID */
    private String id;

    /** 分支名称 */
    private String name;
}
`

**ParallelNodeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParallelNodeConfig {

    /** 并行分支列表 */
    private List<ParallelBranch> branches = new ArrayList<>();

    /** 汇合模式: ALL / ANY */
    private String joinMode;

    /** 生命周期触发器 */
    private NodeTriggerConfig triggers;
}
`

**DelayNodeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelayNodeConfig {

    /** 延迟类型: DURATION / FIXED_TIME */
    private String delayType;

    /** ISO 8601 Duration (delayType=DURATION 时) */
    private String duration;

    /** ISO 8601 DateTime (delayType=FIXED_TIME 时) */
    private String fixedTime;

    /** 生命周期触发器 */
    private NodeTriggerConfig triggers;
}
`

**TriggerNodeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriggerNodeConfig {

    /** 引用 @WorkflowTrigger 注册的 key */
    private String triggerKey;

    /** 是否异步执行 */
    private boolean async;

    /** 生命周期触发器 */
    private NodeTriggerConfig triggers;
}
`

**JumpNodeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JumpNodeConfig {

    /** 跳转目标节点 ID */
    private String targetNodeId;

    /** 生命周期触发器 */
    private NodeTriggerConfig triggers;
}
`

**SubProcessNodeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubProcessNodeConfig {

    /** 子流程定义 key */
    private String definitionKey;

    /** 输入变量映射 (父变量名 -> 子变量名) */
    private Map<String, String> inputMapping = new HashMap<>();

    /** 输出变量映射 (子变量名 -> 父变量名) */
    private Map<String, String> outputMapping = new HashMap<>();

    /** 生命周期触发器 */
    private NodeTriggerConfig triggers;
}
`

**EndNodeConfig.java:**
`java
package cn.projectan.strix.core.module.workflow.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndNodeConfig {

    /** 结束状态: COMPLETED / REJECTED */
    private String endStatus;

    /** 生命周期触发器 */
    private NodeTriggerConfig triggers;
}
`

- [ ] **Step 4: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/model/
git commit -m "feat(workflow): add DAG graph model and node config classes

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 7: Basic CRUD Services

Create 11 service classes (10 entity services + 1 stats service). Each follows the `ServiceImpl` pattern with `@ConditionalOnProperty` for module conditional loading.

**Files:**
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfDefinitionService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfVersionService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfInstanceService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfTokenService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfTaskService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfTaskAssigneeService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfCommentService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfTimerService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfInstanceVarService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfDelegationService.java`
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WfStatsService.java`

- [ ] **Step 1: Create WfDefinitionService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfDefinitionMapper;
import cn.projectan.strix.model.db.workflow.WfDefinition;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfDefinitionService extends ServiceImpl<WfDefinitionMapper, WfDefinition> {

}
`

- [ ] **Step 2: Create WfVersionService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfDefinitionVersionMapper;
import cn.projectan.strix.model.db.workflow.WfDefinitionVersion;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfVersionService extends ServiceImpl<WfDefinitionVersionMapper, WfDefinitionVersion> {

}
`

- [ ] **Step 3: Create WfInstanceService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfInstanceMapper;
import cn.projectan.strix.model.db.workflow.WfInstance;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfInstanceService extends ServiceImpl<WfInstanceMapper, WfInstance> {

}
`

- [ ] **Step 4: Create WfTokenService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfTokenMapper;
import cn.projectan.strix.model.db.workflow.WfToken;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfTokenService extends ServiceImpl<WfTokenMapper, WfToken> {

}
`

- [ ] **Step 5: Create WfTaskService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfTaskMapper;
import cn.projectan.strix.model.db.workflow.WfTask;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfTaskService extends ServiceImpl<WfTaskMapper, WfTask> {

}
`

- [ ] **Step 6: Create WfTaskAssigneeService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfTaskAssigneeMapper;
import cn.projectan.strix.model.db.workflow.WfTaskAssignee;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfTaskAssigneeService extends ServiceImpl<WfTaskAssigneeMapper, WfTaskAssignee> {

}
`

- [ ] **Step 7: Create WfCommentService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfCommentMapper;
import cn.projectan.strix.model.db.workflow.WfComment;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfCommentService extends ServiceImpl<WfCommentMapper, WfComment> {

}
`

- [ ] **Step 8: Create WfTimerService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfTimerMapper;
import cn.projectan.strix.model.db.workflow.WfTimer;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfTimerService extends ServiceImpl<WfTimerMapper, WfTimer> {

}
`

- [ ] **Step 9: Create WfInstanceVarService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfInstanceVarMapper;
import cn.projectan.strix.model.db.workflow.WfInstanceVar;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfInstanceVarService extends ServiceImpl<WfInstanceVarMapper, WfInstanceVar> {

}
`

- [ ] **Step 10: Create WfDelegationService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.mapper.workflow.WfDelegationMapper;
import cn.projectan.strix.model.db.workflow.WfDelegation;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfDelegationService extends ServiceImpl<WfDelegationMapper, WfDelegation> {

}
`

- [ ] **Step 11: Create WfStatsService (placeholder)**

`java
package cn.projectan.strix.service.common.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 工作流统计服务 — 基础骨架，Plan 2 中实现统计逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WfStatsService {

    private final WfInstanceService wfInstanceService;
    private final WfTaskService wfTaskService;
}
`

- [ ] **Step 12: Commit**

`ash
git add src/main/java/cn/projectan/strix/service/common/workflow/
git commit -m "feat(workflow): add 11 basic workflow service classes

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 8: Module Configuration

Enable the workflow module in application configuration.

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Add workflow module config**

In `src/main/resources/application.yml`, locate the `strix.module` section:

`yaml
strix:
  module:
    sms: true
    oss: true
    job: true
    oauth: true
    push: true
    pay: true
`

Add `workflow: true` after `pay: true`:

`yaml
strix:
  module:
    sms: true
    oss: true
    job: true
    oauth: true
    push: true
    pay: true
    workflow: true
`

- [ ] **Step 2: Commit**

`ash
git add src/main/resources/application.yml
git commit -m "feat(workflow): enable workflow module in application config

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 9: Build Verification + Graph Model Tests

Verify that all code compiles and the graph model serializes correctly.

**Files:**
- Create: `src/test/java/cn/projectan/strix/core/module/workflow/model/WorkflowGraphTest.java`

- [ ] **Step 1: Write WorkflowGraph serialization test**

`java
package cn.projectan.strix.core.module.workflow.model;

import cn.projectan.strix.core.module.workflow.model.config.ApprovalNodeConfig;
import cn.projectan.strix.core.module.workflow.model.config.AssigneeConfig;
import cn.projectan.strix.core.module.workflow.model.config.ConditionBranch;
import cn.projectan.strix.core.module.workflow.model.config.ConditionItem;
import cn.projectan.strix.core.module.workflow.model.config.ConditionNodeConfig;
import cn.projectan.strix.core.module.workflow.model.config.TimeoutConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class WorkflowGraphTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGraphSerializationRoundTrip() throws Exception {
        // Build a simple graph: START -> APPROVAL -> END
        WorkflowGraph graph = new WorkflowGraph();

        WorkflowNode startNode = new WorkflowNode();
        startNode.setId("start_1");
        startNode.setType("START");
        startNode.setName("开始");
        startNode.setPosition(new NodePosition(0, 0));

        WorkflowNode approvalNode = new WorkflowNode();
        approvalNode.setId("approve_1");
        approvalNode.setType("APPROVAL");
        approvalNode.setName("主管审批");
        approvalNode.setPosition(new NodePosition(0, 1));

        // Set approval config as JsonNode
        ApprovalNodeConfig approvalConfig = new ApprovalNodeConfig();
        approvalConfig.setApprovalMode("ANY");
        approvalConfig.setAssignees(List.of(
                new AssigneeConfig("ROLE", "dept_manager", "部门主管"),
                new AssigneeConfig("MANAGER", "12345", "张三")
        ));
        approvalConfig.setTimeout(new TimeoutConfig("PT24H", "AUTO_APPROVE", null));
        approvalNode.setConfig(objectMapper.valueToTree(approvalConfig));

        WorkflowNode endNode = new WorkflowNode();
        endNode.setId("end_1");
        endNode.setType("END");
        endNode.setName("结束");
        endNode.setPosition(new NodePosition(0, 2));

        graph.setNodes(List.of(startNode, approvalNode, endNode));
        graph.setEdges(List.of(
                new WorkflowEdge("e1", "start_1", "approve_1"),
                new WorkflowEdge("e2", "approve_1", "end_1")
        ));

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(graph);
        log.info("Graph JSON: {}", json);
        assertNotNull(json);
        assertTrue(json.contains("start_1"));
        assertTrue(json.contains("APPROVAL"));

        // Deserialize back
        WorkflowGraph deserialized = objectMapper.readValue(json, WorkflowGraph.class);
        assertEquals(3, deserialized.getNodes().size());
        assertEquals(2, deserialized.getEdges().size());

        // Verify node lookup
        assertTrue(deserialized.findNode("approve_1").isPresent());
        assertEquals("APPROVAL", deserialized.findNode("approve_1").get().getType());
        assertTrue(deserialized.findNode("nonexistent").isEmpty());

        // Verify edge queries
        assertEquals(1, deserialized.findOutgoingEdges("start_1").size());
        assertEquals("approve_1", deserialized.findOutgoingEdges("start_1").get(0).getTarget());
        assertEquals(1, deserialized.findIncomingEdges("end_1").size());

        // Verify start node finder
        assertTrue(deserialized.findStartNode().isPresent());
        assertEquals("start_1", deserialized.findStartNode().get().getId());

        // Verify next node lookup
        List<String> nextNodes = deserialized.findNextNodeIds("approve_1");
        assertEquals(1, nextNodes.size());
        assertEquals("end_1", nextNodes.get(0));

        // Verify config deserialization
        JsonNode configNode = deserialized.findNode("approve_1").get().getConfig();
        ApprovalNodeConfig parsedConfig = objectMapper.treeToValue(configNode, ApprovalNodeConfig.class);
        assertEquals("ANY", parsedConfig.getApprovalMode());
        assertEquals(2, parsedConfig.getAssignees().size());
        assertEquals("ROLE", parsedConfig.getAssignees().get(0).getType());
        assertEquals("PT24H", parsedConfig.getTimeout().getDuration());
    }

    @Test
    void testConditionNodeConfigSerialization() throws Exception {
        ConditionNodeConfig config = new ConditionNodeConfig();
        config.setDefaultTargetNodeId("cc_1");
        config.setBranches(List.of(
                new ConditionBranch(
                        List.of(new ConditionItem("amount", "GT", 10000)),
                        "AND",
                        "approve_2"
                )
        ));

        String json = objectMapper.writeValueAsString(config);
        ConditionNodeConfig deserialized = objectMapper.readValue(json, ConditionNodeConfig.class);

        assertEquals("cc_1", deserialized.getDefaultTargetNodeId());
        assertEquals(1, deserialized.getBranches().size());
        assertEquals("amount", deserialized.getBranches().get(0).getConditions().get(0).getField());
        assertEquals("GT", deserialized.getBranches().get(0).getConditions().get(0).getOp());
    }

    @Test
    void testNodeExecutionResultFactoryMethods() {
        // Test CONTINUE
        NodeExecutionResult continueResult = NodeExecutionResult.continueWith(List.of("node_2"));
        assertEquals(NodeExecutionResult.Status.CONTINUE, continueResult.getStatus());
        assertEquals(1, continueResult.getNextNodeIds().size());

        // Test WAIT
        NodeExecutionResult waitResult = NodeExecutionResult.waitHere();
        assertEquals(NodeExecutionResult.Status.WAIT, waitResult.getStatus());

        // Test FORK
        NodeExecutionResult forkResult = NodeExecutionResult.fork(List.of(
                List.of("branch_a_1", "branch_a_2"),
                List.of("branch_b_1")
        ));
        assertEquals(NodeExecutionResult.Status.FORK, forkResult.getStatus());
        assertEquals(2, forkResult.getForkBranches().size());

        // Test ERROR
        NodeExecutionResult errorResult = NodeExecutionResult.error("something went wrong");
        assertEquals(NodeExecutionResult.Status.ERROR, errorResult.getStatus());
        assertEquals("something went wrong", errorResult.getErrorMessage());
    }
}
`

- [ ] **Step 2: Run the test**

Run: `./gradlew test --tests "cn.projectan.strix.core.module.workflow.model.WorkflowGraphTest" --no-daemon`

Expected: All 3 tests PASS

- [ ] **Step 3: Run full build to verify compilation**

Run: `./gradlew build -x test --no-daemon`

Expected: BUILD SUCCESSFUL — all new classes compile without errors

- [ ] **Step 4: Commit test**

`ash
git add src/test/java/cn/projectan/strix/core/module/workflow/model/WorkflowGraphTest.java
git commit -m "test(workflow): add graph model serialization tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Summary

Plan 1 creates the complete data foundation:

| Category | Count | Location |
|----------|-------|----------|
| SQL Script | 1 (10 tables) | `docs/sql/workflow_tables.sql` |
| Dict Classes | 16 | `model/dict/workflow/` |
| Enum Classes | 3 | `model/enums/workflow/` |
| Entity Classes | 10 | `model/db/workflow/` |
| Mapper Interfaces | 10 | `mapper/workflow/` |
| Mapper XML | 10 | `resources/mapper/workflow/` |
| Graph Model | 5 | `core/module/workflow/model/` |
| Config Models | 17 | `core/module/workflow/model/config/` |
| Services | 11 | `service/common/workflow/` |
| Tests | 1 | `test/.../WorkflowGraphTest.java` |
| **Total** | **84 files** | |

After Plan 1 is complete, Plan 2 (Engine Core) can begin implementing the execution engine, node handlers, event system, timers, and notification integration — all building on this foundation.
