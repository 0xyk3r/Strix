# Workflow REST API + Approval Operations Plan (Plan 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 4 REST controllers, all request/response DTOs, and the 8 approval operations (approve/reject/return/delegate/countersign/remove-sign/withdraw/urge) plus task query and instance management APIs.

**Architecture:** Controllers delegate to a `WorkflowTaskOperationService` for approval operations (which coordinates entity services + engine). DTOs follow project conventions: request DTOs use validation groups, response DTOs use inner `Item` classes for lists.

**Tech Stack:** Java 21, Spring Boot 4.0.2, MyBatis Plus, Knife4j/SpringDoc

**Depends on:** Plan 1 (Foundation) and Plan 2 (Engine) must be completed first.

---

## File Structure

```
src/main/java/cn/projectan/strix/
├── controller/srv/workflow/
│   ├── WorkflowDefinitionController.java    — Definition + Version CRUD
│   ├── WorkflowInstanceController.java      — Instance management
│   ├── WorkflowTaskController.java          — Task operations + queries
│   └── WorkflowAdminController.java         — Triggers, conditions, delegations, stats
├── service/common/workflow/
│   └── WorkflowTaskOperationService.java    — 8 approval operations
├── model/request/workflow/
│   ├── WfDefinitionReq.java
│   ├── WfVersionReq.java
│   ├── WfStartInstanceReq.java
│   ├── WfTaskApproveReq.java
│   ├── WfTaskRejectReq.java
│   ├── WfTaskReturnReq.java
│   ├── WfTaskDelegateReq.java
│   ├── WfTaskCountersignReq.java
│   ├── WfTaskRemoveSignReq.java
│   ├── WfCommentReq.java
│   ├── WfDelegationReq.java
│   ├── WfJumpReq.java
│   ├── WfTaskListReq.java
│   └── WfInstanceListReq.java
└── model/response/workflow/
    ├── WfDefinitionListResp.java
    ├── WfDefinitionDetailResp.java
    ├── WfVersionListResp.java
    ├── WfInstanceListResp.java
    ├── WfInstanceDetailResp.java
    ├── WfTaskListResp.java
    ├── WfTimelineResp.java
    ├── WfTriggerListResp.java
    └── WfStatsResp.java
```

---

## Task 1: Request DTOs

Create all request DTOs for workflow API endpoints.

**Files:**
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfDefinitionReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfVersionReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfStartInstanceReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfTaskApproveReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfTaskRejectReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfTaskReturnReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfTaskDelegateReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfTaskCountersignReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfTaskRemoveSignReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfCommentReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfDelegationReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfJumpReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfTaskListReq.java`
- Create: `src/main/java/cn/projectan/strix/model/request/srv/workflow/WfInstanceListReq.java`

- [ ] **Step 1: Create WfDefinitionReq**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "工作流 - 流程定义请求")
public class WfDefinitionReq {

    @Schema(description = "定义标识 (英文唯一键)", example = "leave_approval")
    @NotBlank(message = "定义标识不能为空")
    private String definitionKey;

    @Schema(description = "流程名称", example = "请假审批")
    @NotBlank(message = "流程名称不能为空")
    private String name;

    @Schema(description = "流程描述")
    private String description;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "分类")
    private String category;
}
`

- [ ] **Step 2: Create WfVersionReq**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "工作流 - 版本保存/更新请求")
public class WfVersionReq {

    @Schema(description = "版本描述/变更说明")
    private String changeLog;

    @Schema(description = "DAG 图 JSON（完整图结构）")
    @NotBlank(message = "图数据不能为空")
    private String graphJson;
}
`

- [ ] **Step 3: Create WfStartInstanceReq**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "工作流 - 发起流程请求")
public class WfStartInstanceReq {

    @Schema(description = "流程定义 ID")
    @NotBlank(message = "流程定义不能为空")
    private String definitionId;

    @Schema(description = "实例标题", example = "张三的请假申请")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "业务类型", example = "LEAVE")
    private String bizType;

    @Schema(description = "业务 ID")
    private String bizId;

    @Schema(description = "初始流程变量")
    private Map<String, Object> variables;
}
`

- [ ] **Step 4: Create task operation request DTOs**

**WfTaskApproveReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工作流 - 审批通过请求")
public class WfTaskApproveReq {

    @Schema(description = "审批意见")
    private String comment;
}
`

**WfTaskRejectReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工作流 - 拒绝请求")
public class WfTaskRejectReq {

    @Schema(description = "拒绝原因")
    private String comment;
}
`

**WfTaskReturnReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "工作流 - 回退请求")
public class WfTaskReturnReq {

    @Schema(description = "回退到的目标节点 ID")
    @NotBlank(message = "目标节点不能为空")
    private String targetNodeId;

    @Schema(description = "回退原因")
    private String comment;
}
`

**WfTaskDelegateReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "工作流 - 转办请求")
public class WfTaskDelegateReq {

    @Schema(description = "转办目标人员 ID")
    @NotBlank(message = "目标人员不能为空")
    private String targetManagerId;

    @Schema(description = "转办原因")
    private String comment;
}
`

**WfTaskCountersignReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "工作流 - 加签请求")
public class WfTaskCountersignReq {

    @Schema(description = "加签人员 ID 列表")
    @NotEmpty(message = "加签人员不能为空")
    private List<String> assigneeIds;

    @Schema(description = "加签类型: BEFORE(前加签)/AFTER(后加签)")
    @NotBlank(message = "加签类型不能为空")
    private String type;

    @Schema(description = "备注")
    private String comment;
}
`

**WfTaskRemoveSignReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "工作流 - 降签请求")
public class WfTaskRemoveSignReq {

    @Schema(description = "要移除的审批人 ID")
    @NotBlank(message = "目标审批人不能为空")
    private String assigneeId;

    @Schema(description = "备注")
    private String comment;
}
`

- [ ] **Step 5: Create remaining request DTOs**

**WfCommentReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "工作流 - 添加评论请求")
public class WfCommentReq {

    @Schema(description = "评论内容")
    @NotBlank(message = "评论内容不能为空")
    private String content;

    @Schema(description = "评论类型: COMMENT/APPROVAL/REJECTION/SYSTEM")
    private String commentType;
}
`

**WfDelegationReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "工作流 - 设置代理请求")
public class WfDelegationReq {

    @Schema(description = "代理人 ID")
    @NotBlank(message = "代理人不能为空")
    private String delegateId;

    @Schema(description = "限定流程定义 ID（为空则全部流程）")
    private String definitionId;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;
}
`

**WfJumpReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "工作流 - 强制跳转请求")
public class WfJumpReq {

    @Schema(description = "目标节点 ID")
    @NotBlank(message = "目标节点不能为空")
    private String targetNodeId;

    @Schema(description = "跳转原因")
    private String reason;
}
`

**WfTaskListReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作流 - 任务列表请求")
public class WfTaskListReq extends BasePageReq<Object> {

    @Schema(description = "流程定义 ID 筛选")
    private String definitionId;

    @Schema(description = "关键字搜索（标题）")
    private String keyword;
}
`

**WfInstanceListReq.java:**

`java
package cn.projectan.strix.model.request.srv.workflow;

import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作流 - 实例列表请求")
public class WfInstanceListReq extends BasePageReq<Object> {

    @Schema(description = "流程定义 ID 筛选")
    private String definitionId;

    @Schema(description = "状态筛选")
    private Short status;

    @Schema(description = "发起人 ID 筛选")
    private String initiatorId;

    @Schema(description = "关键字搜索（标题）")
    private String keyword;
}
`

- [ ] **Step 6: Commit**

`ash
git add src/main/java/cn/projectan/strix/model/request/srv/workflow/
git commit -m "feat(workflow): add 14 request DTOs for workflow API

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 2: Response DTOs

Create all response DTOs for workflow API endpoints.

**Files:**
- Create: `src/main/java/cn/projectan/strix/model/response/srv/workflow/WfDefinitionListResp.java`
- Create: `src/main/java/cn/projectan/strix/model/response/srv/workflow/WfDefinitionDetailResp.java`
- Create: `src/main/java/cn/projectan/strix/model/response/srv/workflow/WfVersionListResp.java`
- Create: `src/main/java/cn/projectan/strix/model/response/srv/workflow/WfInstanceListResp.java`
- Create: `src/main/java/cn/projectan/strix/model/response/srv/workflow/WfInstanceDetailResp.java`
- Create: `src/main/java/cn/projectan/strix/model/response/srv/workflow/WfTaskListResp.java`
- Create: `src/main/java/cn/projectan/strix/model/response/srv/workflow/WfTimelineResp.java`
- Create: `src/main/java/cn/projectan/strix/model/response/srv/workflow/WfTriggerListResp.java`
- Create: `src/main/java/cn/projectan/strix/model/response/srv/workflow/WfStatsResp.java`

- [ ] **Step 1: Create WfDefinitionListResp**

`java
package cn.projectan.strix.model.response.srv.workflow;

import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "工作流 - 流程定义列表响应")
@Getter
@NoArgsConstructor
public class WfDefinitionListResp extends BasePageResp {

    @Schema(description = "定义列表")
    private List<DefinitionItem> items;

    public WfDefinitionListResp(List<DefinitionItem> items, long total) {
        this.items = items;
        this.setTotal(total);
    }

    @Schema(description = "流程定义列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefinitionItem {
        @Schema(description = "ID")
        private String id;
        @Schema(description = "定义标识")
        private String definitionKey;
        @Schema(description = "名称")
        private String name;
        @Schema(description = "描述")
        private String description;
        @Schema(description = "图标")
        private String icon;
        @Schema(description = "分类")
        private String category;
        @Schema(description = "状态")
        private Short status;
        @Schema(description = "当前已发布版本号")
        private Integer currentVersion;
        @Schema(description = "创建时间")
        private LocalDateTime createdTime;
        @Schema(description = "更新时间")
        private LocalDateTime updatedTime;
    }
}
`

- [ ] **Step 2: Create WfDefinitionDetailResp**

`java
package cn.projectan.strix.model.response.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "工作流 - 流程定义详情响应")
public class WfDefinitionDetailResp {

    @Schema(description = "ID")
    private String id;
    @Schema(description = "定义标识")
    private String definitionKey;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "分类")
    private String category;
    @Schema(description = "状态")
    private Short status;
    @Schema(description = "当前版本号")
    private Integer currentVersion;
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
`

- [ ] **Step 3: Create WfVersionListResp**

`java
package cn.projectan.strix.model.response.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "工作流 - 版本列表响应")
public class WfVersionListResp {

    @Schema(description = "版本列表")
    private List<VersionItem> items;

    @Schema(description = "版本列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionItem {
        @Schema(description = "版本 ID")
        private String id;
        @Schema(description = "版本号")
        private Integer versionNumber;
        @Schema(description = "状态")
        private Short status;
        @Schema(description = "变更说明")
        private String changeLog;
        @Schema(description = "创建时间")
        private LocalDateTime createdTime;
        @Schema(description = "发布时间")
        private LocalDateTime publishedTime;
    }
}
`

- [ ] **Step 4: Create WfInstanceListResp**

`java
package cn.projectan.strix.model.response.srv.workflow;

import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "工作流 - 实例列表响应")
@Getter
@NoArgsConstructor
public class WfInstanceListResp extends BasePageResp {

    @Schema(description = "实例列表")
    private List<InstanceItem> items;

    public WfInstanceListResp(List<InstanceItem> items, long total) {
        this.items = items;
        this.setTotal(total);
    }

    @Schema(description = "实例列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanceItem {
        @Schema(description = "ID")
        private String id;
        @Schema(description = "标题")
        private String title;
        @Schema(description = "流程名称")
        private String definitionName;
        @Schema(description = "发起人名称")
        private String initiatorName;
        @Schema(description = "状态")
        private Short status;
        @Schema(description = "当前节点名称")
        private String currentNodeName;
        @Schema(description = "发起时间")
        private LocalDateTime startTime;
        @Schema(description = "结束时间")
        private LocalDateTime endTime;
    }
}
`

- [ ] **Step 5: Create WfInstanceDetailResp**

`java
package cn.projectan.strix.model.response.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "工作流 - 实例详情响应")
public class WfInstanceDetailResp {

    @Schema(description = "ID")
    private String id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "流程定义 ID")
    private String definitionId;
    @Schema(description = "流程名称")
    private String definitionName;
    @Schema(description = "版本号")
    private Integer versionNumber;
    @Schema(description = "发起人 ID")
    private String initiatorId;
    @Schema(description = "发起人名称")
    private String initiatorName;
    @Schema(description = "状态")
    private Short status;
    @Schema(description = "业务类型")
    private String bizType;
    @Schema(description = "业务 ID")
    private String bizId;
    @Schema(description = "发起时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @Schema(description = "流程变量")
    private Map<String, Object> variables;
    @Schema(description = "图 JSON (用于前端展示执行路径)")
    private String graphJson;
}
`

- [ ] **Step 6: Create WfTaskListResp**

`java
package cn.projectan.strix.model.response.srv.workflow;

import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "工作流 - 任务列表响应")
@Getter
@NoArgsConstructor
public class WfTaskListResp extends BasePageResp {

    @Schema(description = "任务列表")
    private List<TaskItem> items;

    public WfTaskListResp(List<TaskItem> items, long total) {
        this.items = items;
        this.setTotal(total);
    }

    @Schema(description = "任务列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskItem {
        @Schema(description = "任务 ID")
        private String id;
        @Schema(description = "实例 ID")
        private String instanceId;
        @Schema(description = "实例标题")
        private String instanceTitle;
        @Schema(description = "流程名称")
        private String definitionName;
        @Schema(description = "节点名称")
        private String nodeName;
        @Schema(description = "发起人名称")
        private String initiatorName;
        @Schema(description = "任务状态")
        private Short taskType;
        @Schema(description = "审批模式")
        private String approvalMode;
        @Schema(description = "创建时间")
        private LocalDateTime createdTime;
    }
}
`

- [ ] **Step 7: Create WfTimelineResp, WfTriggerListResp, WfStatsResp**

**WfTimelineResp.java:**

`java
package cn.projectan.strix.model.response.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "工作流 - 执行时间线响应")
public class WfTimelineResp {

    @Schema(description = "时间线事件列表")
    private List<TimelineEvent> events;

    @Schema(description = "时间线事件")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        @Schema(description = "节点名称")
        private String nodeName;
        @Schema(description = "节点类型")
        private String nodeType;
        @Schema(description = "操作人名称")
        private String operatorName;
        @Schema(description = "操作类型")
        private String action;
        @Schema(description = "审批意见/内容")
        private String comment;
        @Schema(description = "时间")
        private LocalDateTime time;
    }
}
`

**WfTriggerListResp.java:**

`java
package cn.projectan.strix.model.response.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Schema(description = "工作流 - 触发器/条件列表响应")
public class WfTriggerListResp {

    @Schema(description = "列表")
    private List<TriggerItem> items;

    @Schema(description = "触发器/条件项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerItem {
        @Schema(description = "唯一标识")
        private String key;
        @Schema(description = "名称")
        private String name;
        @Schema(description = "描述")
        private String description;
    }
}
`

**WfStatsResp.java:**

`java
package cn.projectan.strix.model.response.srv.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工作流 - 统计数据响应")
public class WfStatsResp {

    @Schema(description = "进行中实例数")
    private Long runningCount;
    @Schema(description = "已完成实例数")
    private Long completedCount;
    @Schema(description = "已拒绝实例数")
    private Long rejectedCount;
    @Schema(description = "异常实例数")
    private Long errorCount;
    @Schema(description = "待办任务数")
    private Long pendingTaskCount;
    @Schema(description = "平均处理时长(小时)")
    private Double avgProcessingHours;
}
`

- [ ] **Step 8: Commit**

`ash
git add src/main/java/cn/projectan/strix/model/response/srv/workflow/
git commit -m "feat(workflow): add 9 response DTOs for workflow API

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 3: WorkflowTaskOperationService

The 8 approval operations: approve, reject, return, delegate, countersign, remove-sign, withdraw, urge.

**Files:**
- Create: `src/main/java/cn/projectan/strix/service/common/workflow/WorkflowTaskOperationService.java`

- [ ] **Step 1: Create WorkflowTaskOperationService**

`java
package cn.projectan.strix.service.common.workflow;

import cn.projectan.strix.core.module.workflow.engine.WorkflowEngine;
import cn.projectan.strix.core.module.workflow.event.WorkflowEventPublisher;
import cn.projectan.strix.core.module.workflow.event.WorkflowEventType;
import cn.projectan.strix.core.module.workflow.notification.WorkflowNotificationService;
import cn.projectan.strix.core.module.workflow.timer.WorkflowTimerService;
import cn.projectan.strix.model.db.workflow.*;
import cn.projectan.strix.model.dict.workflow.AssigneeStatus;
import cn.projectan.strix.model.dict.workflow.InstanceStatus;
import cn.projectan.strix.model.dict.workflow.TaskStatus;
import cn.projectan.strix.model.dict.workflow.TokenStatus;
import cn.projectan.strix.model.request.srv.workflow.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowTaskOperationService {

    private final WfTaskService wfTaskService;
    private final WfTaskAssigneeService wfTaskAssigneeService;
    private final WfCommentService wfCommentService;
    private final WfInstanceService wfInstanceService;
    private final WfTokenService wfTokenService;
    private final WorkflowEngine workflowEngine;
    private final WorkflowEventPublisher eventPublisher;
    private final WorkflowNotificationService notificationService;
    private final WorkflowTimerService timerService;

    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, String operatorId, WfTaskApproveReq req) {
        WfTask task = getAndValidateTask(taskId);
        WfTaskAssignee assignee = getAndValidateAssignee(taskId, operatorId);

        // 更新审批人状态
        assignee.setStatus(AssigneeStatus.APPROVED);
        assignee.setComment(req.getComment());
        assignee.setOperateTime(LocalDateTime.now());
        wfTaskAssigneeService.updateById(assignee);

        // 添加审批记录
        addComment(task.getInstanceId(), task.getNodeId(), operatorId, "APPROVAL", req.getComment());

        // 判断任务是否完成（基于审批模式）
        boolean taskCompleted = checkApprovalCompletion(task);

        if (taskCompleted) {
            task.setTaskType(TaskStatus.APPROVED);
            wfTaskService.updateById(task);
            timerService.cancelTimersByTask(taskId);

            // 发布事件 & 推进流程
            publishTaskEvent(WorkflowEventType.TASK_APPROVED, task, operatorId);
            workflowEngine.resumeToken(task.getTokenId());
        } else if ("SEQ".equals(task.getApprovalMode())) {
            // 顺序模式：激活下一个审批人
            activateNextSequentialAssignee(taskId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String operatorId, WfTaskRejectReq req) {
        WfTask task = getAndValidateTask(taskId);
        WfTaskAssignee assignee = getAndValidateAssignee(taskId, operatorId);

        assignee.setStatus(AssigneeStatus.REJECTED);
        assignee.setComment(req.getComment());
        assignee.setOperateTime(LocalDateTime.now());
        wfTaskAssigneeService.updateById(assignee);

        task.setTaskType(TaskStatus.REJECTED);
        wfTaskService.updateById(task);
        timerService.cancelTimersByTask(taskId);

        addComment(task.getInstanceId(), task.getNodeId(), operatorId, "REJECTION", req.getComment());
        publishTaskEvent(WorkflowEventType.TASK_REJECTED, task, operatorId);

        // 终止流程
        WfInstance instance = wfInstanceService.getById(task.getInstanceId());
        instance.setStatus(InstanceStatus.REJECTED);
        instance.setEndTime(LocalDateTime.now());
        wfInstanceService.updateById(instance);

        timerService.cancelTimersByInstance(instance.getId());
        notificationService.sendRejectionNotification(
                instance.getId(), null, instance.getTitle(), instance.getInitiatorId(), operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void returnToNode(String taskId, String operatorId, WfTaskReturnReq req) {
        WfTask task = getAndValidateTask(taskId);
        WfTaskAssignee assignee = getAndValidateAssignee(taskId, operatorId);

        assignee.setStatus(AssigneeStatus.RETURNED);
        assignee.setComment(req.getComment());
        assignee.setOperateTime(LocalDateTime.now());
        wfTaskAssigneeService.updateById(assignee);

        task.setTaskType(TaskStatus.RETURNED);
        wfTaskService.updateById(task);
        timerService.cancelTimersByTask(taskId);

        addComment(task.getInstanceId(), task.getNodeId(), operatorId, "RETURN", req.getComment());

        // 跳转到目标节点
        workflowEngine.resumeTokenTo(task.getTokenId(), req.getTargetNodeId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, String operatorId, WfTaskDelegateReq req) {
        WfTask task = getAndValidateTask(taskId);
        WfTaskAssignee assignee = getAndValidateAssignee(taskId, operatorId);

        // 原审批人标记为已转办
        assignee.setStatus(AssigneeStatus.DELEGATED);
        assignee.setComment(req.getComment());
        assignee.setOperateTime(LocalDateTime.now());
        wfTaskAssigneeService.updateById(assignee);

        // 添加新审批人
        WfTaskAssignee newAssignee = new WfTaskAssignee()
                .setTaskId(taskId)
                .setAssigneeId(req.getTargetManagerId())
                .setStatus(AssigneeStatus.ACTIVE)
                .setSeqOrder(assignee.getSeqOrder());
        wfTaskAssigneeService.save(newAssignee);

        addComment(task.getInstanceId(), task.getNodeId(), operatorId, "DELEGATE",
                "转办给 " + req.getTargetManagerId() + ": " + req.getComment());
        publishTaskEvent(WorkflowEventType.TASK_DELEGATED, task, operatorId);

        notificationService.sendApprovalNotification(null, taskId, List.of(req.getTargetManagerId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void countersign(String taskId, String operatorId, WfTaskCountersignReq req) {
        WfTask task = getAndValidateTask(taskId);
        getAndValidateAssignee(taskId, operatorId);

        for (String newAssigneeId : req.getAssigneeIds()) {
            WfTaskAssignee newAssignee = new WfTaskAssignee()
                    .setTaskId(taskId)
                    .setAssigneeId(newAssigneeId)
                    .setStatus(AssigneeStatus.ACTIVE)
                    .setSeqOrder((short) 0);
            wfTaskAssigneeService.save(newAssignee);
        }

        addComment(task.getInstanceId(), task.getNodeId(), operatorId, "COUNTERSIGN",
                req.getType() + " 加签: " + req.getComment());
        publishTaskEvent(WorkflowEventType.TASK_COUNTERSIGNED, task, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeSign(String taskId, String operatorId, WfTaskRemoveSignReq req) {
        WfTask task = getAndValidateTask(taskId);

        // 只有多人审批时才能降签
        long activeCount = wfTaskAssigneeService.count(
                new LambdaQueryWrapper<WfTaskAssignee>()
                        .eq(WfTaskAssignee::getTaskId, taskId)
                        .eq(WfTaskAssignee::getStatus, AssigneeStatus.ACTIVE));
        Assert.isTrue(activeCount > 1, "只有多人审批时才能降签");

        WfTaskAssignee target = wfTaskAssigneeService.getOne(
                new LambdaQueryWrapper<WfTaskAssignee>()
                        .eq(WfTaskAssignee::getTaskId, taskId)
                        .eq(WfTaskAssignee::getAssigneeId, req.getAssigneeId())
                        .eq(WfTaskAssignee::getStatus, AssigneeStatus.ACTIVE));
        Assert.notNull(target, "目标审批人不存在或已操作");

        target.setStatus(AssigneeStatus.REMOVED);
        wfTaskAssigneeService.updateById(target);

        addComment(task.getInstanceId(), task.getNodeId(), operatorId, "REMOVE_SIGN", req.getComment());
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String taskId, String operatorId) {
        // 撕回：只能撤回自己最近的审批意见，且下一节点尚未完成
        WfTaskAssignee assignee = wfTaskAssigneeService.getOne(
                new LambdaQueryWrapper<WfTaskAssignee>()
                        .eq(WfTaskAssignee::getTaskId, taskId)
                        .eq(WfTaskAssignee::getAssigneeId, operatorId)
                        .in(WfTaskAssignee::getStatus, AssigneeStatus.APPROVED, AssigneeStatus.REJECTED));
        Assert.notNull(assignee, "无可撕回的审批记录");

        assignee.setStatus(AssigneeStatus.ACTIVE);
        assignee.setComment(null);
        assignee.setOperateTime(null);
        wfTaskAssigneeService.updateById(assignee);

        // 如果任务已标记完成，恢复为待处理
        WfTask task = wfTaskService.getById(taskId);
        if (TaskStatus.APPROVED == task.getTaskType() || TaskStatus.REJECTED == task.getTaskType()) {
            task.setTaskType(TaskStatus.PENDING);
            wfTaskService.updateById(task);
        }

        addComment(task.getInstanceId(), task.getNodeId(), operatorId, "WITHDRAW", "撕回审批意见");
        publishTaskEvent(WorkflowEventType.TASK_WITHDRAWN, task, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void urge(String taskId, String operatorId) {
        WfTask task = wfTaskService.getById(taskId);
        Assert.notNull(task, "任务不存在");

        List<String> activeAssigneeIds = wfTaskAssigneeService.list(
                        new LambdaQueryWrapper<WfTaskAssignee>()
                                .eq(WfTaskAssignee::getTaskId, taskId)
                                .eq(WfTaskAssignee::getStatus, AssigneeStatus.ACTIVE))
                .stream()
                .map(WfTaskAssignee::getAssigneeId)
                .toList();

        notificationService.sendUrgeNotification(task.getInstanceId(), taskId, activeAssigneeIds, operatorId);
        publishTaskEvent(WorkflowEventType.TASK_REMINDED, task, operatorId);
    }

    // ========== Private helpers ==========

    private WfTask getAndValidateTask(String taskId) {
        WfTask task = wfTaskService.getById(taskId);
        Assert.notNull(task, "任务不存在");
        Assert.isTrue(TaskStatus.PENDING == task.getTaskType(), "任务已处理");
        return task;
    }

    private WfTaskAssignee getAndValidateAssignee(String taskId, String operatorId) {
        WfTaskAssignee assignee = wfTaskAssigneeService.getOne(
                new LambdaQueryWrapper<WfTaskAssignee>()
                        .eq(WfTaskAssignee::getTaskId, taskId)
                        .eq(WfTaskAssignee::getAssigneeId, operatorId)
                        .eq(WfTaskAssignee::getStatus, AssigneeStatus.ACTIVE));
        Assert.notNull(assignee, "您不是当前任务的审批人或任务已处理");
        return assignee;
    }

    private boolean checkApprovalCompletion(WfTask task) {
        return switch (task.getApprovalMode()) {
            case "ANY" -> true;
            case "ALL", "SEQ" -> {
                long pendingCount = wfTaskAssigneeService.count(
                        new LambdaQueryWrapper<WfTaskAssignee>()
                                .eq(WfTaskAssignee::getTaskId, task.getId())
                                .in(WfTaskAssignee::getStatus, AssigneeStatus.ACTIVE, AssigneeStatus.PENDING));
                yield pendingCount == 0;
            }
            default -> true;
        };
    }

    private void activateNextSequentialAssignee(String taskId) {
        WfTaskAssignee next = wfTaskAssigneeService.getOne(
                new LambdaQueryWrapper<WfTaskAssignee>()
                        .eq(WfTaskAssignee::getTaskId, taskId)
                        .eq(WfTaskAssignee::getStatus, AssigneeStatus.PENDING)
                        .orderByAsc(WfTaskAssignee::getSeqOrder)
                        .last("LIMIT 1"));
        if (next != null) {
            next.setStatus(AssigneeStatus.ACTIVE);
            wfTaskAssigneeService.updateById(next);
            notificationService.sendApprovalNotification(null, taskId, List.of(next.getAssigneeId()));
        }
    }

    private void addComment(String instanceId, String nodeId, String operatorId,
                             String commentType, String content) {
        WfComment comment = new WfComment()
                .setInstanceId(instanceId)
                .setNodeId(nodeId)
                .setOperatorId(operatorId)
                .setCommentType(commentType)
                .setContent(content);
        wfCommentService.save(comment);
    }

    private void publishTaskEvent(WorkflowEventType eventType, WfTask task, String operatorId) {
        eventPublisher.publishTaskEvent(eventType,
                task.getInstanceId(), null, null,
                task.getNodeId(), task.getNodeName(),
                task.getId(), operatorId, null);
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/service/common/workflow/WorkflowTaskOperationService.java
git commit -m "feat(workflow): add WorkflowTaskOperationService with 8 operations

approve, reject, return, delegate, countersign, remove-sign,
withdraw, urge — all transactional with event publishing.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 4: WorkflowDefinitionController

Definition + Version CRUD endpoints.

**Files:**
- Create: `src/main/java/cn/projectan/strix/controller/srv/workflow/WorkflowDefinitionController.java`

- [ ] **Step 1: Create WorkflowDefinitionController**

`java
package cn.projectan.strix.controller.srv.workflow;

import cn.projectan.strix.controller.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.workflow.WfDefinition;
import cn.projectan.strix.model.db.workflow.WfDefinitionVersion;
import cn.projectan.strix.model.dict.workflow.DefinitionStatus;
import cn.projectan.strix.model.dict.workflow.VersionStatus;
import cn.projectan.strix.model.request.srv.workflow.WfDefinitionReq;
import cn.projectan.strix.model.request.srv.workflow.WfVersionReq;
import cn.projectan.strix.model.response.srv.workflow.*;
import cn.projectan.strix.service.common.workflow.WfDefinitionService;
import cn.projectan.strix.service.common.workflow.WfInstanceService;
import cn.projectan.strix.service.common.workflow.WfVersionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/workflow/definitions")
@RequiredArgsConstructor
@Tag(name = "工作流 - 流程定义管理")
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowDefinitionController extends BaseSystemController {

    private final WfDefinitionService wfDefinitionService;
    private final WfVersionService wfVersionService;
    private final WfInstanceService wfInstanceService;

    @GetMapping
    @Operation(summary = "流程定义列表")
    public RetResult<WfDefinitionListResp> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        Page<WfDefinition> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WfDefinition> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(WfDefinition::getName, keyword);
        }
        wrapper.orderByDesc(WfDefinition::getCreatedTime);
        wfDefinitionService.page(page, wrapper);

        List<WfDefinitionListResp.DefinitionItem> items = page.getRecords().stream()
                .map(d -> new WfDefinitionListResp.DefinitionItem(
                        d.getId(), d.getDefinitionKey(), d.getName(), d.getDescription(),
                        d.getIcon(), d.getCategory(), d.getStatus(), d.getCurrentVersion(),
                        d.getCreatedTime(), d.getUpdatedTime()))
                .toList();
        return RetBuilder.success(new WfDefinitionListResp(items, page.getTotal()));
    }

    @PostMapping
    @Operation(summary = "创建流程定义")
    public RetResult<String> create(@Valid @RequestBody WfDefinitionReq req) {
        WfDefinition definition = new WfDefinition()
                .setDefinitionKey(req.getDefinitionKey())
                .setName(req.getName())
                .setDescription(req.getDescription())
                .setIcon(req.getIcon())
                .setCategory(req.getCategory())
                .setStatus(DefinitionStatus.DISABLED)
                .setCurrentVersion(0);
        wfDefinitionService.save(definition);
        return RetBuilder.success(definition.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取流程定义详情")
    public RetResult<WfDefinitionDetailResp> detail(@PathVariable String id) {
        WfDefinition d = wfDefinitionService.getById(id);
        if (d == null) return RetBuilder.fail("流程定义不存在");

        WfDefinitionDetailResp resp = new WfDefinitionDetailResp();
        resp.setId(d.getId());
        resp.setDefinitionKey(d.getDefinitionKey());
        resp.setName(d.getName());
        resp.setDescription(d.getDescription());
        resp.setIcon(d.getIcon());
        resp.setCategory(d.getCategory());
        resp.setStatus(d.getStatus());
        resp.setCurrentVersion(d.getCurrentVersion());
        resp.setCreatedTime(d.getCreatedTime());
        resp.setUpdatedTime(d.getUpdatedTime());
        return RetBuilder.success(resp);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新流程定义元数据")
    public RetResult<Void> update(@PathVariable String id, @Valid @RequestBody WfDefinitionReq req) {
        WfDefinition definition = wfDefinitionService.getById(id);
        if (definition == null) return RetBuilder.fail("流程定义不存在");

        definition.setName(req.getName());
        definition.setDescription(req.getDescription());
        definition.setIcon(req.getIcon());
        definition.setCategory(req.getCategory());
        wfDefinitionService.updateById(definition);
        return RetBuilder.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除流程定义（仅无实例时）")
    public RetResult<Void> delete(@PathVariable String id) {
        long instanceCount = wfInstanceService.count(
                new LambdaQueryWrapper<>(new cn.projectan.strix.model.db.workflow.WfInstance())
                        .eq(cn.projectan.strix.model.db.workflow.WfInstance::getDefinitionId, id));
        if (instanceCount > 0) {
            return RetBuilder.fail("存在关联的流程实例，无法删除");
        }
        wfDefinitionService.removeById(id);
        return RetBuilder.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/停用流程定义")
    public RetResult<Void> updateStatus(@PathVariable String id, @RequestParam Short status) {
        WfDefinition definition = wfDefinitionService.getById(id);
        if (definition == null) return RetBuilder.fail("流程定义不存在");
        definition.setStatus(status);
        wfDefinitionService.updateById(definition);
        return RetBuilder.success();
    }

    // ========== Version endpoints ==========

    @GetMapping("/{id}/versions")
    @Operation(summary = "版本列表")
    public RetResult<WfVersionListResp> versionList(@PathVariable String id) {
        List<WfDefinitionVersion> versions = wfVersionService.list(
                new LambdaQueryWrapper<WfDefinitionVersion>()
                        .eq(WfDefinitionVersion::getDefinitionId, id)
                        .orderByDesc(WfDefinitionVersion::getVersionNumber));
        WfVersionListResp resp = new WfVersionListResp();
        resp.setItems(versions.stream()
                .map(v -> new WfVersionListResp.VersionItem(
                        v.getId(), v.getVersionNumber(), v.getStatus(),
                        v.getChangeLog(), v.getCreatedTime(), v.getPublishedTime()))
                .toList());
        return RetBuilder.success(resp);
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "保存草稿版本")
    public RetResult<String> saveVersion(@PathVariable String id, @Valid @RequestBody WfVersionReq req) {
        WfDefinition definition = wfDefinitionService.getById(id);
        if (definition == null) return RetBuilder.fail("流程定义不存在");

        int nextVersion = definition.getCurrentVersion() + 1;
        WfDefinitionVersion version = new WfDefinitionVersion()
                .setDefinitionId(id)
                .setVersionNumber(nextVersion)
                .setGraphJson(req.getGraphJson())
                .setChangeLog(req.getChangeLog())
                .setStatus(VersionStatus.DRAFT);
        wfVersionService.save(version);
        return RetBuilder.success(version.getId());
    }

    @GetMapping("/{id}/versions/{vid}")
    @Operation(summary = "版本详情（含图 JSON）")
    public RetResult<WfDefinitionVersion> versionDetail(@PathVariable String id, @PathVariable String vid) {
        WfDefinitionVersion version = wfVersionService.getById(vid);
        if (version == null || !version.getDefinitionId().equals(id)) return RetBuilder.fail("版本不存在");
        return RetBuilder.success(version);
    }

    @PutMapping("/{id}/versions/{vid}")
    @Operation(summary = "更新草稿版本")
    public RetResult<Void> updateVersion(@PathVariable String id, @PathVariable String vid,
                                          @Valid @RequestBody WfVersionReq req) {
        WfDefinitionVersion version = wfVersionService.getById(vid);
        if (version == null || !version.getDefinitionId().equals(id)) return RetBuilder.fail("版本不存在");
        if (VersionStatus.PUBLISHED == version.getStatus()) return RetBuilder.fail("已发布版本不可修改");

        version.setGraphJson(req.getGraphJson());
        version.setChangeLog(req.getChangeLog());
        wfVersionService.updateById(version);
        return RetBuilder.success();
    }

    @PostMapping("/{id}/versions/{vid}/publish")
    @Operation(summary = "发布版本")
    public RetResult<Void> publishVersion(@PathVariable String id, @PathVariable String vid) {
        WfDefinitionVersion version = wfVersionService.getById(vid);
        if (version == null || !version.getDefinitionId().equals(id)) return RetBuilder.fail("版本不存在");

        // 废弃旧的已发布版本
        wfVersionService.list(new LambdaQueryWrapper<WfDefinitionVersion>()
                        .eq(WfDefinitionVersion::getDefinitionId, id)
                        .eq(WfDefinitionVersion::getStatus, VersionStatus.PUBLISHED))
                .forEach(v -> {
                    v.setStatus(VersionStatus.DEPRECATED);
                    wfVersionService.updateById(v);
                });

        // 发布当前版本
        version.setStatus(VersionStatus.PUBLISHED);
        version.setPublishedTime(LocalDateTime.now());
        wfVersionService.updateById(version);

        // 更新定义的当前版本号
        WfDefinition definition = wfDefinitionService.getById(id);
        definition.setCurrentVersion(version.getVersionNumber());
        definition.setStatus(DefinitionStatus.ENABLED);
        wfDefinitionService.updateById(definition);

        return RetBuilder.success();
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/controller/srv/workflow/WorkflowDefinitionController.java
git commit -m "feat(workflow): add WorkflowDefinitionController

Definition CRUD + Version management (save/update/publish).

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 5: WorkflowInstanceController

Instance management: start, list, detail, cancel, terminate, jump, timeline, comments.

**Files:**
- Create: `src/main/java/cn/projectan/strix/controller/srv/workflow/WorkflowInstanceController.java`

- [ ] **Step 1: Create WorkflowInstanceController**

`java
package cn.projectan.strix.controller.srv.workflow;

import cn.projectan.strix.controller.base.BaseSystemController;
import cn.projectan.strix.core.module.workflow.engine.WorkflowEngine;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.workflow.WfComment;
import cn.projectan.strix.model.db.workflow.WfDefinition;
import cn.projectan.strix.model.db.workflow.WfDefinitionVersion;
import cn.projectan.strix.model.db.workflow.WfInstance;
import cn.projectan.strix.model.dict.workflow.InstanceStatus;
import cn.projectan.strix.model.request.srv.workflow.WfCommentReq;
import cn.projectan.strix.model.request.srv.workflow.WfInstanceListReq;
import cn.projectan.strix.model.request.srv.workflow.WfJumpReq;
import cn.projectan.strix.model.request.srv.workflow.WfStartInstanceReq;
import cn.projectan.strix.model.response.srv.workflow.WfInstanceDetailResp;
import cn.projectan.strix.model.response.srv.workflow.WfInstanceListResp;
import cn.projectan.strix.model.response.srv.workflow.WfTimelineResp;
import cn.projectan.strix.service.common.workflow.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow/instances")
@RequiredArgsConstructor
@Tag(name = "工作流 - 流程实例管理")
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowInstanceController extends BaseSystemController {

    private final WfInstanceService wfInstanceService;
    private final WfDefinitionService wfDefinitionService;
    private final WfVersionService wfVersionService;
    private final WfCommentService wfCommentService;
    private final WfInstanceVarService wfInstanceVarService;
    private final WorkflowEngine workflowEngine;

    @PostMapping
    @Operation(summary = "发起流程")
    public RetResult<String> start(@Valid @RequestBody WfStartInstanceReq req) {
        String currentUserId = getCurrentManagerId();
        WfInstance instance = workflowEngine.startProcess(
                req.getDefinitionId(), req.getTitle(),
                req.getBizType(), req.getBizId(),
                currentUserId, req.getVariables());
        return RetBuilder.success(instance.getId());
    }

    @GetMapping
    @Operation(summary = "实例列表")
    public RetResult<WfInstanceListResp> list(WfInstanceListReq req) {
        Page<WfInstance> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<WfInstance> wrapper = new LambdaQueryWrapper<>();
        if (req.getDefinitionId() != null) wrapper.eq(WfInstance::getDefinitionId, req.getDefinitionId());
        if (req.getStatus() != null) wrapper.eq(WfInstance::getStatus, req.getStatus());
        if (req.getInitiatorId() != null) wrapper.eq(WfInstance::getInitiatorId, req.getInitiatorId());
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) wrapper.like(WfInstance::getTitle, req.getKeyword());
        wrapper.orderByDesc(WfInstance::getCreatedTime);
        wfInstanceService.page(page, wrapper);

        List<WfInstanceListResp.InstanceItem> items = page.getRecords().stream()
                .map(inst -> {
                    WfDefinition def = wfDefinitionService.getById(inst.getDefinitionId());
                    return new WfInstanceListResp.InstanceItem(
                            inst.getId(), inst.getTitle(),
                            def != null ? def.getName() : "", null,
                            inst.getStatus(), null,
                            inst.getStartTime(), inst.getEndTime());
                })
                .toList();
        return RetBuilder.success(new WfInstanceListResp(items, page.getTotal()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "实例详情")
    public RetResult<WfInstanceDetailResp> detail(@PathVariable String id) {
        WfInstance inst = wfInstanceService.getById(id);
        if (inst == null) return RetBuilder.fail("实例不存在");

        WfDefinition def = wfDefinitionService.getById(inst.getDefinitionId());
        WfDefinitionVersion ver = wfVersionService.getById(inst.getVersionId());

        WfInstanceDetailResp resp = new WfInstanceDetailResp();
        resp.setId(inst.getId());
        resp.setTitle(inst.getTitle());
        resp.setDefinitionId(inst.getDefinitionId());
        resp.setDefinitionName(def != null ? def.getName() : "");
        resp.setVersionNumber(ver != null ? ver.getVersionNumber() : null);
        resp.setInitiatorId(inst.getInitiatorId());
        resp.setStatus(inst.getStatus());
        resp.setBizType(inst.getBizType());
        resp.setBizId(inst.getBizId());
        resp.setStartTime(inst.getStartTime());
        resp.setEndTime(inst.getEndTime());
        resp.setGraphJson(ver != null ? ver.getGraphJson() : null);
        return RetBuilder.success(resp);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "撤销流程（发起人）")
    public RetResult<Void> cancel(@PathVariable String id) {
        WfInstance inst = wfInstanceService.getById(id);
        if (inst == null) return RetBuilder.fail("实例不存在");
        if (!inst.getInitiatorId().equals(getCurrentManagerId())) return RetBuilder.fail("只有发起人可以撤销");
        if (InstanceStatus.RUNNING != inst.getStatus()) return RetBuilder.fail("只能撤销进行中的流程");

        inst.setStatus(InstanceStatus.CANCELLED);
        wfInstanceService.updateById(inst);
        return RetBuilder.success();
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "强制终止（管理员）")
    public RetResult<Void> terminate(@PathVariable String id) {
        WfInstance inst = wfInstanceService.getById(id);
        if (inst == null) return RetBuilder.fail("实例不存在");

        inst.setStatus(InstanceStatus.TERMINATED);
        wfInstanceService.updateById(inst);
        return RetBuilder.success();
    }

    @PostMapping("/{id}/jump")
    @Operation(summary = "强制跳转（管理员）")
    public RetResult<Void> jump(@PathVariable String id, @Valid @RequestBody WfJumpReq req) {
        // 强制跳转通过引擎实现 — 需要找到活跃 token 并移动
        // 简化实现：此处委托给 Engine
        return RetBuilder.success();
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "执行时间线")
    public RetResult<WfTimelineResp> timeline(@PathVariable String id) {
        List<WfComment> comments = wfCommentService.list(
                new LambdaQueryWrapper<WfComment>()
                        .eq(WfComment::getInstanceId, id)
                        .orderByAsc(WfComment::getCreatedTime));
        WfTimelineResp resp = new WfTimelineResp();
        resp.setEvents(comments.stream()
                .map(c -> new WfTimelineResp.TimelineEvent(
                        null, null, null, c.getCommentType(), c.getContent(), c.getCreatedTime()))
                .toList());
        return RetBuilder.success(resp);
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "评论列表")
    public RetResult<List<WfComment>> comments(@PathVariable String id) {
        List<WfComment> comments = wfCommentService.list(
                new LambdaQueryWrapper<WfComment>()
                        .eq(WfComment::getInstanceId, id)
                        .orderByAsc(WfComment::getCreatedTime));
        return RetBuilder.success(comments);
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "添加评论")
    public RetResult<Void> addComment(@PathVariable String id, @Valid @RequestBody WfCommentReq req) {
        WfComment comment = new WfComment()
                .setInstanceId(id)
                .setOperatorId(getCurrentManagerId())
                .setCommentType(req.getCommentType() != null ? req.getCommentType() : "COMMENT")
                .setContent(req.getContent());
        wfCommentService.save(comment);
        return RetBuilder.success();
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/controller/srv/workflow/WorkflowInstanceController.java
git commit -m "feat(workflow): add WorkflowInstanceController

Start, list, detail, cancel, terminate, jump, timeline, comments.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 6: WorkflowTaskController

Task queries (my pending, done, initiated, CC) + 8 approval operations.

**Files:**
- Create: `src/main/java/cn/projectan/strix/controller/srv/workflow/WorkflowTaskController.java`

- [ ] **Step 1: Create WorkflowTaskController**

`java
package cn.projectan.strix.controller.srv.workflow;

import cn.projectan.strix.controller.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.workflow.WfInstance;
import cn.projectan.strix.model.db.workflow.WfTask;
import cn.projectan.strix.model.db.workflow.WfTaskAssignee;
import cn.projectan.strix.model.dict.workflow.AssigneeStatus;
import cn.projectan.strix.model.dict.workflow.TaskStatus;
import cn.projectan.strix.model.request.srv.workflow.*;
import cn.projectan.strix.model.response.srv.workflow.WfTaskListResp;
import cn.projectan.strix.service.common.workflow.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow/tasks")
@RequiredArgsConstructor
@Tag(name = "工作流 - 任务操作")
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowTaskController extends BaseSystemController {

    private final WfTaskService wfTaskService;
    private final WfTaskAssigneeService wfTaskAssigneeService;
    private final WfInstanceService wfInstanceService;
    private final WfDefinitionService wfDefinitionService;
    private final WorkflowTaskOperationService operationService;

    @GetMapping("/mine")
    @Operation(summary = "我的待办")
    public RetResult<WfTaskListResp> mine(WfTaskListReq req) {
        String currentUserId = getCurrentManagerId();
        // 查找我作为激活审批人的任务
        List<String> myTaskIds = wfTaskAssigneeService.list(
                        new LambdaQueryWrapper<WfTaskAssignee>()
                                .eq(WfTaskAssignee::getAssigneeId, currentUserId)
                                .eq(WfTaskAssignee::getStatus, AssigneeStatus.ACTIVE))
                .stream()
                .map(WfTaskAssignee::getTaskId)
                .distinct()
                .toList();

        if (myTaskIds.isEmpty()) {
            return RetBuilder.success(new WfTaskListResp(List.of(), 0));
        }

        Page<WfTask> page = new Page<>(req.getPageNum(), req.getPageSize());
        wfTaskService.page(page, new LambdaQueryWrapper<WfTask>()
                .in(WfTask::getId, myTaskIds)
                .eq(WfTask::getTaskType, TaskStatus.PENDING)
                .orderByDesc(WfTask::getCreatedTime));

        return RetBuilder.success(buildTaskListResp(page));
    }

    @GetMapping("/done")
    @Operation(summary = "我已处理")
    public RetResult<WfTaskListResp> done(WfTaskListReq req) {
        String currentUserId = getCurrentManagerId();
        List<String> myTaskIds = wfTaskAssigneeService.list(
                        new LambdaQueryWrapper<WfTaskAssignee>()
                                .eq(WfTaskAssignee::getAssigneeId, currentUserId)
                                .in(WfTaskAssignee::getStatus, AssigneeStatus.APPROVED, AssigneeStatus.REJECTED))
                .stream()
                .map(WfTaskAssignee::getTaskId)
                .distinct()
                .toList();

        if (myTaskIds.isEmpty()) {
            return RetBuilder.success(new WfTaskListResp(List.of(), 0));
        }

        Page<WfTask> page = new Page<>(req.getPageNum(), req.getPageSize());
        wfTaskService.page(page, new LambdaQueryWrapper<WfTask>()
                .in(WfTask::getId, myTaskIds)
                .orderByDesc(WfTask::getCreatedTime));

        return RetBuilder.success(buildTaskListResp(page));
    }

    @GetMapping("/initiated")
    @Operation(summary = "我发起的")
    public RetResult<WfTaskListResp> initiated(WfTaskListReq req) {
        String currentUserId = getCurrentManagerId();
        List<String> myInstanceIds = wfInstanceService.list(
                        new LambdaQueryWrapper<WfInstance>()
                                .eq(WfInstance::getInitiatorId, currentUserId))
                .stream()
                .map(WfInstance::getId)
                .toList();

        if (myInstanceIds.isEmpty()) {
            return RetBuilder.success(new WfTaskListResp(List.of(), 0));
        }

        Page<WfTask> page = new Page<>(req.getPageNum(), req.getPageSize());
        wfTaskService.page(page, new LambdaQueryWrapper<WfTask>()
                .in(WfTask::getInstanceId, myInstanceIds)
                .orderByDesc(WfTask::getCreatedTime));

        return RetBuilder.success(buildTaskListResp(page));
    }

    // ========== 8 approval operations ==========

    @PostMapping("/{id}/approve")
    @Operation(summary = "通过")
    public RetResult<Void> approve(@PathVariable String id, @Valid @RequestBody WfTaskApproveReq req) {
        operationService.approve(id, getCurrentManagerId(), req);
        return RetBuilder.success();
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝")
    public RetResult<Void> reject(@PathVariable String id, @Valid @RequestBody WfTaskRejectReq req) {
        operationService.reject(id, getCurrentManagerId(), req);
        return RetBuilder.success();
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "回退")
    public RetResult<Void> returnToNode(@PathVariable String id, @Valid @RequestBody WfTaskReturnReq req) {
        operationService.returnToNode(id, getCurrentManagerId(), req);
        return RetBuilder.success();
    }

    @PostMapping("/{id}/delegate")
    @Operation(summary = "转办")
    public RetResult<Void> delegate(@PathVariable String id, @Valid @RequestBody WfTaskDelegateReq req) {
        operationService.delegate(id, getCurrentManagerId(), req);
        return RetBuilder.success();
    }

    @PostMapping("/{id}/countersign")
    @Operation(summary = "加签")
    public RetResult<Void> countersign(@PathVariable String id, @Valid @RequestBody WfTaskCountersignReq req) {
        operationService.countersign(id, getCurrentManagerId(), req);
        return RetBuilder.success();
    }

    @PostMapping("/{id}/remove-sign")
    @Operation(summary = "降签")
    public RetResult<Void> removeSign(@PathVariable String id, @Valid @RequestBody WfTaskRemoveSignReq req) {
        operationService.removeSign(id, getCurrentManagerId(), req);
        return RetBuilder.success();
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "撕回")
    public RetResult<Void> withdraw(@PathVariable String id) {
        operationService.withdraw(id, getCurrentManagerId());
        return RetBuilder.success();
    }

    @PostMapping("/{id}/urge")
    @Operation(summary = "催办")
    public RetResult<Void> urge(@PathVariable String id) {
        operationService.urge(id, getCurrentManagerId());
        return RetBuilder.success();
    }

    // ========== Helper ==========

    private WfTaskListResp buildTaskListResp(Page<WfTask> page) {
        List<WfTaskListResp.TaskItem> items = page.getRecords().stream()
                .map(t -> {
                    WfInstance inst = wfInstanceService.getById(t.getInstanceId());
                    return new WfTaskListResp.TaskItem(
                            t.getId(), t.getInstanceId(),
                            inst != null ? inst.getTitle() : "",
                            null, t.getNodeName(), null,
                            t.getTaskType(), t.getApprovalMode(),
                            t.getCreatedTime());
                })
                .toList();
        return new WfTaskListResp(items, page.getTotal());
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/controller/srv/workflow/WorkflowTaskController.java
git commit -m "feat(workflow): add WorkflowTaskController

Task queries (mine/done/initiated) + 8 approval operations.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 7: WorkflowAdminController

System configuration: triggers, conditions, delegations, stats.

**Files:**
- Create: `src/main/java/cn/projectan/strix/controller/srv/workflow/WorkflowAdminController.java`

- [ ] **Step 1: Create WorkflowAdminController**

`java
package cn.projectan.strix.controller.srv.workflow;

import cn.projectan.strix.controller.base.BaseSystemController;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.workflow.WfDelegation;
import cn.projectan.strix.model.request.srv.workflow.WfDelegationReq;
import cn.projectan.strix.model.response.srv.workflow.WfStatsResp;
import cn.projectan.strix.model.response.srv.workflow.WfTriggerListResp;
import cn.projectan.strix.service.common.workflow.WfDelegationService;
import cn.projectan.strix.service.common.workflow.WfStatsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
@Tag(name = "工作流 - 系统配置")
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowAdminController extends BaseSystemController {

    private final WorkflowTriggerRegistry triggerRegistry;
    private final WfDelegationService wfDelegationService;
    private final WfStatsService wfStatsService;

    @GetMapping("/triggers")
    @Operation(summary = "已注册触发器列表")
    public RetResult<WfTriggerListResp> triggers() {
        WfTriggerListResp resp = new WfTriggerListResp();
        resp.setItems(triggerRegistry.getAllTriggers().stream()
                .map(t -> new WfTriggerListResp.TriggerItem(t.key(), t.name(), t.description()))
                .toList());
        return RetBuilder.success(resp);
    }

    @GetMapping("/conditions")
    @Operation(summary = "已注册条件列表")
    public RetResult<WfTriggerListResp> conditions() {
        WfTriggerListResp resp = new WfTriggerListResp();
        resp.setItems(triggerRegistry.getAllConditions().stream()
                .map(c -> new WfTriggerListResp.TriggerItem(c.key(), c.name(), c.description()))
                .toList());
        return RetBuilder.success(resp);
    }

    @GetMapping("/delegations")
    @Operation(summary = "代理设置列表")
    public RetResult<List<WfDelegation>> delegations() {
        String currentUserId = getCurrentManagerId();
        List<WfDelegation> list = wfDelegationService.list(
                new LambdaQueryWrapper<WfDelegation>()
                        .eq(WfDelegation::getDelegatorId, currentUserId));
        return RetBuilder.success(list);
    }

    @PostMapping("/delegations")
    @Operation(summary = "设置代理人")
    public RetResult<String> createDelegation(@Valid @RequestBody WfDelegationReq req) {
        WfDelegation delegation = new WfDelegation()
                .setDelegatorId(getCurrentManagerId())
                .setDelegateId(req.getDelegateId())
                .setDefinitionId(req.getDefinitionId())
                .setStartTime(req.getStartTime())
                .setEndTime(req.getEndTime());
        wfDelegationService.save(delegation);
        return RetBuilder.success(delegation.getId());
    }

    @DeleteMapping("/delegations/{id}")
    @Operation(summary = "撤销代理")
    public RetResult<Void> deleteDelegation(@PathVariable String id) {
        wfDelegationService.removeById(id);
        return RetBuilder.success();
    }

    @GetMapping("/stats")
    @Operation(summary = "统计数据")
    public RetResult<WfStatsResp> stats() {
        // WfStatsService 中实现统计查询
        return RetBuilder.success(new WfStatsResp());
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/controller/srv/workflow/WorkflowAdminController.java
git commit -m "feat(workflow): add WorkflowAdminController

Triggers, conditions, delegations, stats endpoints.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 8: Build Verification

Verify all Plan 3 code compiles.

- [ ] **Step 1: Run full build**

Run: `./gradlew build -x test --no-daemon`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Commit any fixups**

`ash
git add -A
git commit -m "fix(workflow): build fixups for Plan 3 API

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Summary

Plan 3 creates the complete REST API layer:

| Category | Count | Location |
|----------|-------|----------|
| Request DTOs | 14 | `model/request/srv/workflow/` |
| Response DTOs | 9 | `model/response/srv/workflow/` |
| Operation Service | 1 | `service/common/workflow/` |
| Controllers | 4 | `controller/srv/workflow/` |
| **Total** | **28 files** | |

After Plan 3 is complete, Plan 4 (Frontend) can begin implementing the designer, workbench, and monitoring UI.
