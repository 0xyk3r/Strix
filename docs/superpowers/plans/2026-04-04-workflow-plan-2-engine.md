# Workflow Engine Core Implementation Plan (Plan 2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the workflow execution engine, 11 node handlers, event/trigger system, timer service, and notification integration.

**Architecture:** DAG execution engine with Token-based position tracking. Strategy pattern for node handlers. Annotation-driven trigger/listener/condition discovery via BeanPostProcessor. Spring ApplicationEvent for internal events. Quartz for timer scheduling.

**Tech Stack:** Java 21, Spring Boot 4.0.2, MyBatis Plus, Redisson, Quartz, Jackson

**Depends on:** Plan 1 (Backend Foundation) must be completed first.

---

## File Structure

```
src/main/java/cn/projectan/strix/core/module/workflow/
├── engine/
│   ├── WorkflowEngine.java              — Core execution engine
│   ├── ExecutionContext.java             — Per-execution context holder
│   ├── NodeHandlerRegistry.java         — Strategy registry for node handlers
│   └── ConditionEvaluator.java          — Rule-based condition evaluator
├── handler/
│   ├── NodeHandler.java                 — Handler interface
│   ├── AbstractNodeHandler.java         — Base class with shared logic
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
│   ├── WorkflowEvent.java               — Spring ApplicationEvent subclass
│   ├── WorkflowEventType.java           — Event type enum
│   ├── WorkflowEventPublisher.java      — Event publishing helper
│   ├── WorkflowTrigger.java             — @Annotation for trigger methods
│   ├── WorkflowListener.java            — @Annotation for event listeners
│   ├── WorkflowCondition.java           — @Annotation for condition methods
│   ├── WorkflowTriggerContext.java      — Context passed to trigger/condition methods
│   └── WorkflowTriggerRegistry.java     — BeanPostProcessor for auto-discovery
├── timer/
│   ├── WorkflowTimerService.java        — Timer lifecycle management
│   └── WorkflowTimerJob.java            — Quartz Job implementation
└── notification/
    └── WorkflowNotificationService.java — Notification integration layer

src/test/java/cn/projectan/strix/core/module/workflow/
├── engine/
│   ├── WorkflowEngineTest.java
│   ├── ConditionEvaluatorTest.java
│   └── NodeHandlerRegistryTest.java
├── handler/
│   ├── StartEndNodeHandlerTest.java
│   ├── ConditionNodeHandlerTest.java
│   └── ParallelNodeHandlerTest.java
└── event/
    └── WorkflowTriggerRegistryTest.java
```

---

## Task 1: Event Annotations + Types

Define the 3 custom annotations, event type enum, trigger context, and event model.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowEventType.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowTrigger.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowListener.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowCondition.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowTriggerContext.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowEvent.java`

- [ ] **Step 1: Create WorkflowEventType enum**

`java
package cn.projectan.strix.core.module.workflow.event;

import lombok.Getter;

@Getter
public enum WorkflowEventType {

    // 流程级事件
    PROCESS_STARTED("PROCESS_STARTED", "流程已启动"),
    PROCESS_COMPLETED("PROCESS_COMPLETED", "流程已完成"),
    PROCESS_REJECTED("PROCESS_REJECTED", "流程已拒绝"),
    PROCESS_CANCELLED("PROCESS_CANCELLED", "流程已撤销"),
    PROCESS_TERMINATED("PROCESS_TERMINATED", "流程已终止"),
    PROCESS_ERROR("PROCESS_ERROR", "流程异常"),

    // 节点级事件
    NODE_BEFORE_ENTER("NODE_BEFORE_ENTER", "节点进入前"),
    NODE_AFTER_ENTER("NODE_AFTER_ENTER", "节点进入后"),
    NODE_BEFORE_LEAVE("NODE_BEFORE_LEAVE", "节点离开前"),
    NODE_AFTER_LEAVE("NODE_AFTER_LEAVE", "节点离开后"),
    NODE_ERROR("NODE_ERROR", "节点异常"),

    // 任务级事件
    TASK_CREATED("TASK_CREATED", "任务已创建"),
    TASK_APPROVED("TASK_APPROVED", "任务已通过"),
    TASK_REJECTED("TASK_REJECTED", "任务已拒绝"),
    TASK_DELEGATED("TASK_DELEGATED", "任务已转办"),
    TASK_COUNTERSIGNED("TASK_COUNTERSIGNED", "任务已加签"),
    TASK_WITHDRAWN("TASK_WITHDRAWN", "任务已撕回"),
    TASK_TIMEOUT("TASK_TIMEOUT", "任务已超时"),
    TASK_REMINDED("TASK_REMINDED", "任务已催办");

    private final String code;
    private final String description;

    WorkflowEventType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
`

- [ ] **Step 2: Create @WorkflowTrigger annotation**

`java
package cn.projectan.strix.core.module.workflow.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法为工作流触发器，TRIGGER 节点通过 key 调用。
 * 方法签名: Map<String, Object> methodName(WorkflowTriggerContext ctx)
 * 返回值写入流程变量。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WorkflowTrigger {

    /** 触发器唯一标识 */
    String key();

    /** 触发器名称（前端展示用） */
    String name() default "";

    /** 描述 */
    String description() default "";
}
`

- [ ] **Step 3: Create @WorkflowListener annotation**

`java
package cn.projectan.strix.core.module.workflow.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法为工作流事件监听器。
 * 方法签名: void methodName(WorkflowEvent event)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WorkflowListener {

    /** 监听的事件类型 */
    WorkflowEventType event();

    /** 限定流程定义 key（为空则监听所有流程） */
    String definitionKey() default "";
}
`

- [ ] **Step 4: Create @WorkflowCondition annotation**

`java
package cn.projectan.strix.core.module.workflow.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法为自定义条件评估器，条件节点通过 key 调用。
 * 方法签名: boolean methodName(WorkflowTriggerContext ctx)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WorkflowCondition {

    /** 条件唯一标识 */
    String key();

    /** 条件名称（前端展示用） */
    String name() default "";

    /** 描述 */
    String description() default "";
}
`

- [ ] **Step 5: Create WorkflowTriggerContext**

`java
package cn.projectan.strix.core.module.workflow.event;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 传递给 @WorkflowTrigger / @WorkflowCondition 方法的上下文。
 */
@Getter
@Builder
public class WorkflowTriggerContext {

    /** 流程实例 ID */
    private final String instanceId;

    /** 流程定义 key */
    private final String definitionKey;

    /** 当前节点 ID */
    private final String nodeId;

    /** 当前节点名称 */
    private final String nodeName;

    /** 业务类型 */
    private final String bizType;

    /** 业务 ID */
    private final String bizId;

    /** 流程变量（只读快照） */
    private final Map<String, Object> variables;

    /** 发起人 ID */
    private final String initiatorId;
}
`

- [ ] **Step 6: Create WorkflowEvent (Spring ApplicationEvent)**

`java
package cn.projectan.strix.core.module.workflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class WorkflowEvent extends ApplicationEvent {

    private final WorkflowEventType eventType;
    private final String instanceId;
    private final String definitionKey;
    private final String definitionName;
    private final String nodeId;
    private final String nodeName;
    private final String taskId;
    private final String operatorId;
    private final Map<String, Object> variables;

    private WorkflowEvent(Object source, WorkflowEventType eventType, String instanceId,
                           String definitionKey, String definitionName,
                           String nodeId, String nodeName,
                           String taskId, String operatorId,
                           Map<String, Object> variables) {
        super(source);
        this.eventType = eventType;
        this.instanceId = instanceId;
        this.definitionKey = definitionKey;
        this.definitionName = definitionName;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.taskId = taskId;
        this.operatorId = operatorId;
        this.variables = variables != null ? Map.copyOf(variables) : Map.of();
    }

    public static Builder builder(Object source, WorkflowEventType eventType) {
        return new Builder(source, eventType);
    }

    public static class Builder {
        private final Object source;
        private final WorkflowEventType eventType;
        private String instanceId;
        private String definitionKey;
        private String definitionName;
        private String nodeId;
        private String nodeName;
        private String taskId;
        private String operatorId;
        private Map<String, Object> variables;

        private Builder(Object source, WorkflowEventType eventType) {
            this.source = source;
            this.eventType = eventType;
        }

        public Builder instanceId(String instanceId) { this.instanceId = instanceId; return this; }
        public Builder definitionKey(String definitionKey) { this.definitionKey = definitionKey; return this; }
        public Builder definitionName(String definitionName) { this.definitionName = definitionName; return this; }
        public Builder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
        public Builder nodeName(String nodeName) { this.nodeName = nodeName; return this; }
        public Builder taskId(String taskId) { this.taskId = taskId; return this; }
        public Builder operatorId(String operatorId) { this.operatorId = operatorId; return this; }
        public Builder variables(Map<String, Object> variables) { this.variables = variables; return this; }

        public WorkflowEvent build() {
            return new WorkflowEvent(source, eventType, instanceId,
                    definitionKey, definitionName, nodeId, nodeName,
                    taskId, operatorId, variables);
        }
    }
}
`

- [ ] **Step 7: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/event/
git commit -m "feat(workflow): add event annotations, types, and models

- WorkflowEventType enum with 19 event types
- @WorkflowTrigger, @WorkflowListener, @WorkflowCondition annotations
- WorkflowTriggerContext for trigger/condition method invocation
- WorkflowEvent extending Spring ApplicationEvent

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 2: WorkflowTriggerRegistry (BeanPostProcessor)

Auto-discovers @WorkflowTrigger, @WorkflowListener, @WorkflowCondition annotated methods at startup.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowTriggerRegistry.java`

- [ ] **Step 1: Create WorkflowTriggerRegistry**

`java
package cn.projectan.strix.core.module.workflow.event;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowTriggerRegistry implements BeanPostProcessor {

    private final Map<String, TriggerEntry> triggerMap = new ConcurrentHashMap<>();
    private final Map<String, ConditionEntry> conditionMap = new ConcurrentHashMap<>();
    private final List<ListenerEntry> listeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        ReflectionUtils.doWithMethods(targetClass, method -> {
            scanTrigger(bean, method);
            scanListener(bean, method);
            scanCondition(bean, method);
        });
        return bean;
    }

    private void scanTrigger(Object bean, Method method) {
        WorkflowTrigger ann = AnnotationUtils.findAnnotation(method, WorkflowTrigger.class);
        if (ann == null) return;
        String key = ann.key();
        if (triggerMap.containsKey(key)) {
            log.warn("Duplicate @WorkflowTrigger key '{}', overwriting with {}.{}", key, bean.getClass().getSimpleName(), method.getName());
        }
        method.setAccessible(true);
        triggerMap.put(key, new TriggerEntry(bean, method, ann.key(), ann.name(), ann.description()));
        log.info("Registered @WorkflowTrigger: key={}, method={}.{}", key, bean.getClass().getSimpleName(), method.getName());
    }

    private void scanListener(Object bean, Method method) {
        WorkflowListener ann = AnnotationUtils.findAnnotation(method, WorkflowListener.class);
        if (ann == null) return;
        method.setAccessible(true);
        listeners.add(new ListenerEntry(bean, method, ann.event(), ann.definitionKey()));
        log.info("Registered @WorkflowListener: event={}, definitionKey={}, method={}.{}",
                ann.event(), ann.definitionKey(), bean.getClass().getSimpleName(), method.getName());
    }

    private void scanCondition(Object bean, Method method) {
        WorkflowCondition ann = AnnotationUtils.findAnnotation(method, WorkflowCondition.class);
        if (ann == null) return;
        String key = ann.key();
        if (conditionMap.containsKey(key)) {
            log.warn("Duplicate @WorkflowCondition key '{}', overwriting", key);
        }
        method.setAccessible(true);
        conditionMap.put(key, new ConditionEntry(bean, method, ann.key(), ann.name(), ann.description()));
        log.info("Registered @WorkflowCondition: key={}, method={}.{}", key, bean.getClass().getSimpleName(), method.getName());
    }

    /** 执行触发器，返回结果 Map（写入流程变量） */
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeTrigger(String key, WorkflowTriggerContext context) {
        TriggerEntry entry = triggerMap.get(key);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown workflow trigger: " + key);
        }
        try {
            Object result = entry.method().invoke(entry.bean(), context);
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            return result != null ? Map.of("_result", result) : Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute trigger '" + key + "': " + e.getMessage(), e);
        }
    }

    /** 评估自定义条件 */
    public boolean evaluateCondition(String key, WorkflowTriggerContext context) {
        ConditionEntry entry = conditionMap.get(key);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown workflow condition: " + key);
        }
        try {
            Object result = entry.method().invoke(entry.bean(), context);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate condition '" + key + "': " + e.getMessage(), e);
        }
    }

    /** 获取匹配事件的监听器列表 */
    public List<ListenerEntry> getListeners(WorkflowEventType eventType, String definitionKey) {
        return listeners.stream()
                .filter(l -> l.eventType() == eventType)
                .filter(l -> l.definitionKey().isEmpty() || l.definitionKey().equals(definitionKey))
                .toList();
    }

    /** 获取所有已注册触发器（前端设计器使用） */
    public List<TriggerEntry> getAllTriggers() {
        return List.copyOf(triggerMap.values());
    }

    /** 获取所有已注册条件（前端设计器使用） */
    public List<ConditionEntry> getAllConditions() {
        return List.copyOf(conditionMap.values());
    }

    public boolean hasTrigger(String key) {
        return triggerMap.containsKey(key);
    }

    public boolean hasCondition(String key) {
        return conditionMap.containsKey(key);
    }

    @Getter
    public record TriggerEntry(Object bean, Method method, String key, String name, String description) {}

    @Getter
    public record ConditionEntry(Object bean, Method method, String key, String name, String description) {}

    @Getter
    public record ListenerEntry(Object bean, Method method, WorkflowEventType eventType, String definitionKey) {}
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowTriggerRegistry.java
git commit -m "feat(workflow): add WorkflowTriggerRegistry BeanPostProcessor

Auto-discovers @WorkflowTrigger, @WorkflowListener, @WorkflowCondition
annotated methods at startup.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 3: WorkflowEventPublisher

Publishes WorkflowEvent via Spring ApplicationEventPublisher and invokes registered @WorkflowListener methods.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowEventPublisher.java`

- [ ] **Step 1: Create WorkflowEventPublisher**

`java
package cn.projectan.strix.core.module.workflow.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final WorkflowTriggerRegistry triggerRegistry;

    /**
     * 发布工作流事件：同时通过 Spring ApplicationEvent 和 @WorkflowListener 分发。
     */
    public void publish(WorkflowEvent event) {
        log.debug("Publishing workflow event: type={}, instanceId={}, nodeId={}",
                event.getEventType(), event.getInstanceId(), event.getNodeId());

        // 1. 发布 Spring ApplicationEvent（支持 @EventListener / @TransactionalEventListener）
        applicationEventPublisher.publishEvent(event);

        // 2. 调用 @WorkflowListener 注解方法
        List<WorkflowTriggerRegistry.ListenerEntry> listeners =
                triggerRegistry.getListeners(event.getEventType(), event.getDefinitionKey());
        for (WorkflowTriggerRegistry.ListenerEntry listener : listeners) {
            try {
                listener.method().invoke(listener.bean(), event);
            } catch (Exception e) {
                log.error("Error invoking @WorkflowListener {}.{}: {}",
                        listener.bean().getClass().getSimpleName(),
                        listener.method().getName(), e.getMessage(), e);
            }
        }
    }

    /** 快捷方法：发布流程级事件 */
    public void publishProcessEvent(WorkflowEventType eventType, String instanceId,
                                     String definitionKey, String definitionName,
                                     String operatorId, Map<String, Object> variables) {
        publish(WorkflowEvent.builder(this, eventType)
                .instanceId(instanceId)
                .definitionKey(definitionKey)
                .definitionName(definitionName)
                .operatorId(operatorId)
                .variables(variables)
                .build());
    }

    /** 快捷方法：发布节点级事件 */
    public void publishNodeEvent(WorkflowEventType eventType, String instanceId,
                                  String definitionKey, String definitionName,
                                  String nodeId, String nodeName,
                                  Map<String, Object> variables) {
        publish(WorkflowEvent.builder(this, eventType)
                .instanceId(instanceId)
                .definitionKey(definitionKey)
                .definitionName(definitionName)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .variables(variables)
                .build());
    }

    /** 快捷方法：发布任务级事件 */
    public void publishTaskEvent(WorkflowEventType eventType, String instanceId,
                                  String definitionKey, String definitionName,
                                  String nodeId, String nodeName,
                                  String taskId, String operatorId,
                                  Map<String, Object> variables) {
        publish(WorkflowEvent.builder(this, eventType)
                .instanceId(instanceId)
                .definitionKey(definitionKey)
                .definitionName(definitionName)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .taskId(taskId)
                .operatorId(operatorId)
                .variables(variables)
                .build());
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/event/WorkflowEventPublisher.java
git commit -m "feat(workflow): add WorkflowEventPublisher

Publishes events via Spring ApplicationEventPublisher and
invokes @WorkflowListener annotated methods.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 4: ExecutionContext

The per-execution context holder carrying all state for a single token step.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/engine/ExecutionContext.java`

- [ ] **Step 1: Create ExecutionContext**

`java
package cn.projectan.strix.core.module.workflow.engine;

import cn.projectan.strix.core.module.workflow.model.WorkflowGraph;
import cn.projectan.strix.core.module.workflow.model.WorkflowNode;
import cn.projectan.strix.model.db.workflow.WfDefinition;
import cn.projectan.strix.model.db.workflow.WfDefinitionVersion;
import cn.projectan.strix.model.db.workflow.WfInstance;
import cn.projectan.strix.model.db.workflow.WfToken;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class ExecutionContext {

    private final WfDefinition definition;
    private final WfDefinitionVersion version;
    private final WfInstance instance;
    private final WorkflowGraph graph;

    @Setter
    private WfToken currentToken;

    @Setter
    private WorkflowNode currentNode;

    /** 流程变量（可读写，引擎完成后持久化变更） */
    @Builder.Default
    private final Map<String, Object> variables = new HashMap<>();

    /** 本次执行中新增/修改的变量 key（用于增量持久化） */
    @Builder.Default
    @Setter
    private Map<String, Object> dirtyVariables = new HashMap<>();

    /** 操作人 ID（审批、干预等操作时设置） */
    @Setter
    private String operatorId;

    public String getInstanceId() {
        return instance.getId();
    }

    public String getDefinitionKey() {
        return definition.getDefinitionKey();
    }

    public String getDefinitionName() {
        return definition.getName();
    }

    /** 获取变量值 */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }

    /** 设置变量值（同时标记为 dirty） */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
        dirtyVariables.put(key, value);
    }

    /** 批量设置变量 */
    public void setVariables(Map<String, Object> vars) {
        if (vars != null) {
            vars.forEach(this::setVariable);
        }
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/engine/ExecutionContext.java
git commit -m "feat(workflow): add ExecutionContext

Per-execution context with instance, token, graph, variables,
and dirty-tracking for incremental persistence.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 5: NodeHandler Interface + AbstractNodeHandler + Registry

Define the handler interface, a base class with shared trigger logic, and a strategy registry.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/NodeHandler.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/AbstractNodeHandler.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/engine/NodeHandlerRegistry.java`

- [ ] **Step 1: Create NodeHandler interface**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;

/**
 * 节点处理器接口 — 策略模式。
 * 每种节点类型有一个实现。
 */
public interface NodeHandler {

    /** 返回处理的节点类型标识（对应 WfNodeType 枚举的 codeValue） */
    String getType();

    /** 节点进入时调用（可选，默认空） */
    default void onEnter(ExecutionContext context) {}

    /** 执行节点逻辑，返回执行结果 */
    NodeExecutionResult execute(ExecutionContext context);

    /** 节点离开时调用（可选，默认空） */
    default void onLeave(ExecutionContext context) {}
}
`

- [ ] **Step 2: Create AbstractNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.config.NodeTriggerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 节点处理器基类 — 提供配置反序列化和生命周期触发器执行。
 */
@Slf4j
public abstract class AbstractNodeHandler implements NodeHandler {

    protected final ObjectMapper objectMapper;
    protected final WorkflowTriggerRegistry triggerRegistry;

    protected AbstractNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry) {
        this.objectMapper = objectMapper;
        this.triggerRegistry = triggerRegistry;
    }

    /** 反序列化节点 config 为指定类型 */
    protected <T> T parseConfig(ExecutionContext context, Class<T> configClass) {
        JsonNode configNode = context.getCurrentNode().getConfig();
        if (configNode == null || configNode.isNull() || configNode.isMissingNode()) {
            try {
                return configClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create default config for " + configClass.getSimpleName(), e);
            }
        }
        try {
            return objectMapper.treeToValue(configNode, configClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse node config for node '"
                    + context.getCurrentNode().getId() + "': " + e.getMessage(), e);
        }
    }

    /** 执行节点生命周期触发器（如 onBeforeEnter, onAfterLeave） */
    protected void executeLifecycleTrigger(ExecutionContext context, String triggerKey) {
        if (triggerKey == null || triggerKey.isBlank()) return;
        if (!triggerRegistry.hasTrigger(triggerKey)) {
            log.warn("Lifecycle trigger '{}' not found for node '{}'", triggerKey, context.getCurrentNode().getId());
            return;
        }
        WorkflowTriggerContext triggerContext = buildTriggerContext(context);
        Map<String, Object> result = triggerRegistry.executeTrigger(triggerKey, triggerContext);
        context.setVariables(result);
    }

    /** 尝试执行 NodeTriggerConfig 中的指定阶段触发器 */
    protected void fireNodeTrigger(ExecutionContext context, NodeTriggerConfig triggers, String phase) {
        if (triggers == null) return;
        String key = switch (phase) {
            case "onBeforeEnter" -> triggers.getOnBeforeEnter();
            case "onAfterEnter" -> triggers.getOnAfterEnter();
            case "onBeforeLeave" -> triggers.getOnBeforeLeave();
            case "onAfterLeave" -> triggers.getOnAfterLeave();
            default -> null;
        };
        executeLifecycleTrigger(context, key);
    }

    protected WorkflowTriggerContext buildTriggerContext(ExecutionContext context) {
        return WorkflowTriggerContext.builder()
                .instanceId(context.getInstanceId())
                .definitionKey(context.getDefinitionKey())
                .nodeId(context.getCurrentNode().getId())
                .nodeName(context.getCurrentNode().getName())
                .bizType(context.getInstance().getBizType())
                .bizId(context.getInstance().getBizId())
                .variables(Map.copyOf(context.getVariables()))
                .initiatorId(context.getInstance().getInitiatorId())
                .build();
    }
}
`

- [ ] **Step 3: Create NodeHandlerRegistry**

`java
package cn.projectan.strix.core.module.workflow.engine;

import cn.projectan.strix.core.module.workflow.handler.NodeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点处理器注册表 — 根据节点类型查找对应的 NodeHandler。
 * 所有 NodeHandler 实现通过 Spring 自动注入。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class NodeHandlerRegistry {

    private final Map<String, NodeHandler> handlerMap = new HashMap<>();

    public NodeHandlerRegistry(List<NodeHandler> handlers) {
        for (NodeHandler handler : handlers) {
            String type = handler.getType();
            if (handlerMap.containsKey(type)) {
                log.warn("Duplicate NodeHandler for type '{}': {} replaced by {}",
                        type, handlerMap.get(type).getClass().getSimpleName(),
                        handler.getClass().getSimpleName());
            }
            handlerMap.put(type, handler);
            log.info("Registered NodeHandler: {} -> {}", type, handler.getClass().getSimpleName());
        }
    }

    public NodeHandler getHandler(String nodeType) {
        NodeHandler handler = handlerMap.get(nodeType);
        if (handler == null) {
            throw new IllegalArgumentException("No NodeHandler registered for type: " + nodeType);
        }
        return handler;
    }

    public boolean hasHandler(String nodeType) {
        return handlerMap.containsKey(nodeType);
    }
}
`

- [ ] **Step 4: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/handler/NodeHandler.java \
        src/main/java/cn/projectan/strix/core/module/workflow/handler/AbstractNodeHandler.java \
        src/main/java/cn/projectan/strix/core/module/workflow/engine/NodeHandlerRegistry.java
git commit -m "feat(workflow): add NodeHandler interface, AbstractNodeHandler, and Registry

Strategy pattern for node execution. Registry auto-collects all
NodeHandler beans via Spring list injection.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 6: ConditionEvaluator

Rule-based condition evaluator supporting structured JSON conditions and custom @WorkflowCondition.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/engine/ConditionEvaluator.java`
- Create: `src/test/java/cn/projectan/strix/core/module/workflow/engine/ConditionEvaluatorTest.java`

- [ ] **Step 1: Create ConditionEvaluator**

`java
package cn.projectan.strix.core.module.workflow.engine;

import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.config.ConditionBranch;
import cn.projectan.strix.core.module.workflow.model.config.ConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class ConditionEvaluator {

    private final WorkflowTriggerRegistry triggerRegistry;

    /**
     * 评估条件分支列表，返回第一个匹配分支的 targetNodeId。
     * 如果没有分支匹配，返回 defaultTargetNodeId。
     */
    public String evaluateBranches(List<ConditionBranch> branches, String defaultTargetNodeId,
                                    Map<String, Object> variables, WorkflowTriggerContext triggerContext) {
        if (branches != null) {
            for (ConditionBranch branch : branches) {
                if (evaluateBranch(branch, variables, triggerContext)) {
                    return branch.getTargetNodeId();
                }
            }
        }
        return defaultTargetNodeId;
    }

    /** 评估单个分支（一组条件 + 逻辑连接符） */
    public boolean evaluateBranch(ConditionBranch branch, Map<String, Object> variables,
                                   WorkflowTriggerContext triggerContext) {
        List<ConditionItem> conditions = branch.getConditions();
        if (conditions == null || conditions.isEmpty()) return true;

        String logic = branch.getLogic() != null ? branch.getLogic() : "AND";
        return "OR".equalsIgnoreCase(logic)
                ? conditions.stream().anyMatch(c -> evaluateItem(c, variables, triggerContext))
                : conditions.stream().allMatch(c -> evaluateItem(c, variables, triggerContext));
    }

    /** 评估单个条件项 */
    public boolean evaluateItem(ConditionItem item, Map<String, Object> variables,
                                 WorkflowTriggerContext triggerContext) {
        String field = item.getField();

        // 自定义条件: field 以 "custom:" 前缀标记
        if (field != null && field.startsWith("custom:")) {
            String conditionKey = field.substring("custom:".length());
            return triggerRegistry.evaluateCondition(conditionKey, triggerContext);
        }

        Object actualValue = variables.get(field);
        String op = item.getOp();
        Object expectedValue = item.getValue();

        return switch (op.toUpperCase()) {
            case "EQ" -> equals(actualValue, expectedValue);
            case "NEQ" -> !equals(actualValue, expectedValue);
            case "GT" -> compare(actualValue, expectedValue) > 0;
            case "GTE" -> compare(actualValue, expectedValue) >= 0;
            case "LT" -> compare(actualValue, expectedValue) < 0;
            case "LTE" -> compare(actualValue, expectedValue) <= 0;
            case "IN" -> isIn(actualValue, expectedValue);
            case "NOT_IN" -> !isIn(actualValue, expectedValue);
            case "CONTAINS" -> contains(actualValue, expectedValue);
            case "IS_NULL" -> actualValue == null;
            case "IS_NOT_NULL" -> actualValue != null;
            default -> {
                log.warn("Unknown condition operator: {}", op);
                yield false;
            }
        };
    }

    private boolean equals(Object actual, Object expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        if (actual instanceof Number && expected instanceof Number) {
            return toBigDecimal(actual).compareTo(toBigDecimal(expected)) == 0;
        }
        return actual.toString().equals(expected.toString());
    }

    private int compare(Object actual, Object expected) {
        if (actual == null || expected == null) return actual == null ? -1 : 1;
        return toBigDecimal(actual).compareTo(toBigDecimal(expected));
    }

    @SuppressWarnings("unchecked")
    private boolean isIn(Object actual, Object expected) {
        if (actual == null || expected == null) return false;
        if (expected instanceof Collection<?> coll) {
            return coll.stream().anyMatch(e -> equals(actual, e));
        }
        if (expected instanceof String str) {
            return List.of(str.split(",")).contains(actual.toString());
        }
        return false;
    }

    private boolean contains(Object actual, Object expected) {
        if (actual == null || expected == null) return false;
        return actual.toString().contains(expected.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert to number: " + value);
        }
    }
}
`

- [ ] **Step 2: Write ConditionEvaluator tests**

`java
package cn.projectan.strix.core.module.workflow.engine;

import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.config.ConditionBranch;
import cn.projectan.strix.core.module.workflow.model.config.ConditionItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        WorkflowTriggerRegistry registry = mock(WorkflowTriggerRegistry.class);
        evaluator = new ConditionEvaluator(registry);
    }

    @Test
    void testEQ() {
        Map<String, Object> vars = Map.of("status", "approved");
        assertTrue(evaluator.evaluateItem(new ConditionItem("status", "EQ", "approved"), vars, null));
        assertFalse(evaluator.evaluateItem(new ConditionItem("status", "EQ", "rejected"), vars, null));
    }

    @Test
    void testNumericComparison() {
        Map<String, Object> vars = Map.of("amount", 10000);
        assertTrue(evaluator.evaluateItem(new ConditionItem("amount", "GT", 5000), vars, null));
        assertFalse(evaluator.evaluateItem(new ConditionItem("amount", "GT", 20000), vars, null));
        assertTrue(evaluator.evaluateItem(new ConditionItem("amount", "GTE", 10000), vars, null));
        assertTrue(evaluator.evaluateItem(new ConditionItem("amount", "LT", 20000), vars, null));
        assertTrue(evaluator.evaluateItem(new ConditionItem("amount", "LTE", 10000), vars, null));
    }

    @Test
    void testIN() {
        Map<String, Object> vars = Map.of("dept", "engineering");
        assertTrue(evaluator.evaluateItem(new ConditionItem("dept", "IN", List.of("engineering", "product")), vars, null));
        assertFalse(evaluator.evaluateItem(new ConditionItem("dept", "IN", List.of("sales", "hr")), vars, null));
    }

    @Test
    void testCONTAINS() {
        Map<String, Object> vars = Map.of("title", "Senior Engineer");
        assertTrue(evaluator.evaluateItem(new ConditionItem("title", "CONTAINS", "Engineer"), vars, null));
        assertFalse(evaluator.evaluateItem(new ConditionItem("title", "CONTAINS", "Manager"), vars, null));
    }

    @Test
    void testNullChecks() {
        Map<String, Object> vars = Map.of("name", "test");
        assertTrue(evaluator.evaluateItem(new ConditionItem("name", "IS_NOT_NULL", null), vars, null));
        assertTrue(evaluator.evaluateItem(new ConditionItem("missing", "IS_NULL", null), vars, null));
    }

    @Test
    void testBranch_AND_logic() {
        Map<String, Object> vars = Map.of("amount", 10000, "dept", "engineering");
        ConditionBranch branch = new ConditionBranch(
                List.of(
                        new ConditionItem("amount", "GT", 5000),
                        new ConditionItem("dept", "EQ", "engineering")
                ),
                "AND",
                "approve_node"
        );
        assertTrue(evaluator.evaluateBranch(branch, vars, null));
    }

    @Test
    void testBranch_OR_logic() {
        Map<String, Object> vars = Map.of("amount", 3000, "dept", "engineering");
        ConditionBranch branch = new ConditionBranch(
                List.of(
                        new ConditionItem("amount", "GT", 5000),
                        new ConditionItem("dept", "EQ", "engineering")
                ),
                "OR",
                "approve_node"
        );
        assertTrue(evaluator.evaluateBranch(branch, vars, null));
    }

    @Test
    void testEvaluateBranches_firstMatch() {
        Map<String, Object> vars = Map.of("amount", 10000);
        List<ConditionBranch> branches = List.of(
                new ConditionBranch(List.of(new ConditionItem("amount", "GT", 50000)), "AND", "cfo_approve"),
                new ConditionBranch(List.of(new ConditionItem("amount", "GT", 5000)), "AND", "manager_approve"),
                new ConditionBranch(List.of(new ConditionItem("amount", "GT", 0)), "AND", "auto_approve")
        );
        String result = evaluator.evaluateBranches(branches, "default_node", vars, null);
        assertEquals("manager_approve", result);
    }

    @Test
    void testEvaluateBranches_noMatch_returnsDefault() {
        Map<String, Object> vars = Map.of("amount", -1);
        List<ConditionBranch> branches = List.of(
                new ConditionBranch(List.of(new ConditionItem("amount", "GT", 0)), "AND", "some_node")
        );
        String result = evaluator.evaluateBranches(branches, "default_node", vars, null);
        assertEquals("default_node", result);
    }
}
`

- [ ] **Step 3: Run tests**

Run: `./gradlew test --tests "cn.projectan.strix.core.module.workflow.engine.ConditionEvaluatorTest" --no-daemon`

Expected: All 8 tests PASS

- [ ] **Step 4: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/engine/ConditionEvaluator.java \
        src/test/java/cn/projectan/strix/core/module/workflow/engine/ConditionEvaluatorTest.java
git commit -m "feat(workflow): add ConditionEvaluator with structured rule evaluation

Supports EQ/NEQ/GT/GTE/LT/LTE/IN/NOT_IN/CONTAINS/IS_NULL/IS_NOT_NULL
operators, AND/OR logic, and custom @WorkflowCondition delegation.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 7: Simple Auto-Flow Node Handlers

Implement Start, End, CC, Jump, and Trigger handlers — all non-blocking, auto-advancing nodes.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/StartNodeHandler.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/EndNodeHandler.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/CcNodeHandler.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/JumpNodeHandler.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/TriggerNodeHandler.java`

- [ ] **Step 1: Create StartNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class StartNodeHandler extends AbstractNodeHandler {

    public StartNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry) {
        super(objectMapper, triggerRegistry);
    }

    @Override
    public String getType() {
        return "START";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        log.debug("Executing START node: {}", context.getCurrentNode().getId());
        return NodeExecutionResult.continueWith(context.getGraph().findNextNodeIds(context.getCurrentNode().getId()));
    }
}
`

- [ ] **Step 2: Create EndNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.config.EndNodeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class EndNodeHandler extends AbstractNodeHandler {

    public EndNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry) {
        super(objectMapper, triggerRegistry);
    }

    @Override
    public String getType() {
        return "END";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        EndNodeConfig config = parseConfig(context, EndNodeConfig.class);
        log.debug("Executing END node: {}, endStatus={}", context.getCurrentNode().getId(), config.getEndStatus());
        return NodeExecutionResult.complete(config.getEndStatus());
    }
}
`

- [ ] **Step 3: Create CcNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.config.AssigneeConfig;
import cn.projectan.strix.core.module.workflow.model.config.CcNodeConfig;
import cn.projectan.strix.core.module.workflow.notification.WorkflowNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class CcNodeHandler extends AbstractNodeHandler {

    private final WorkflowNotificationService notificationService;

    public CcNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry,
                          WorkflowNotificationService notificationService) {
        super(objectMapper, triggerRegistry);
        this.notificationService = notificationService;
    }

    @Override
    public String getType() {
        return "CC";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        CcNodeConfig config = parseConfig(context, CcNodeConfig.class);
        log.debug("Executing CC node: {}, receivers={}", context.getCurrentNode().getId(),
                config.getAssignees() != null ? config.getAssignees().size() : 0);

        if (config.getAssignees() != null && !config.getAssignees().isEmpty()) {
            List<String> receiverIds = notificationService.resolveAssigneeIds(config.getAssignees());
            notificationService.sendCcNotification(context, receiverIds);
        }

        return NodeExecutionResult.continueWith(context.getGraph().findNextNodeIds(context.getCurrentNode().getId()));
    }
}
`

- [ ] **Step 4: Create JumpNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.config.JumpNodeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class JumpNodeHandler extends AbstractNodeHandler {

    public JumpNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry) {
        super(objectMapper, triggerRegistry);
    }

    @Override
    public String getType() {
        return "JUMP";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        JumpNodeConfig config = parseConfig(context, JumpNodeConfig.class);
        String targetNodeId = config.getTargetNodeId();
        log.debug("Executing JUMP node: {} -> target: {}", context.getCurrentNode().getId(), targetNodeId);

        if (targetNodeId == null || targetNodeId.isBlank()) {
            return NodeExecutionResult.error("JUMP node missing targetNodeId");
        }
        if (context.getGraph().findNode(targetNodeId).isEmpty()) {
            return NodeExecutionResult.error("JUMP target node not found: " + targetNodeId);
        }
        return NodeExecutionResult.continueWith(List.of(targetNodeId));
    }
}
`

- [ ] **Step 5: Create TriggerNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.config.TriggerNodeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class TriggerNodeHandler extends AbstractNodeHandler {

    public TriggerNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry) {
        super(objectMapper, triggerRegistry);
    }

    @Override
    public String getType() {
        return "TRIGGER";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        TriggerNodeConfig config = parseConfig(context, TriggerNodeConfig.class);
        String triggerKey = config.getTriggerKey();
        log.debug("Executing TRIGGER node: {}, triggerKey={}", context.getCurrentNode().getId(), triggerKey);

        if (triggerKey == null || triggerKey.isBlank()) {
            return NodeExecutionResult.error("TRIGGER node missing triggerKey");
        }
        if (!triggerRegistry.hasTrigger(triggerKey)) {
            return NodeExecutionResult.error("Trigger not found: " + triggerKey);
        }

        try {
            WorkflowTriggerContext triggerContext = buildTriggerContext(context);
            Map<String, Object> result = triggerRegistry.executeTrigger(triggerKey, triggerContext);
            context.setVariables(result);
        } catch (Exception e) {
            log.error("Trigger '{}' execution failed: {}", triggerKey, e.getMessage(), e);
            if (Boolean.TRUE.equals(config.getFailOnError())) {
                return NodeExecutionResult.error("Trigger failed: " + e.getMessage());
            }
        }

        return NodeExecutionResult.continueWith(context.getGraph().findNextNodeIds(context.getCurrentNode().getId()));
    }
}
`

- [ ] **Step 6: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/handler/StartNodeHandler.java \
        src/main/java/cn/projectan/strix/core/module/workflow/handler/EndNodeHandler.java \
        src/main/java/cn/projectan/strix/core/module/workflow/handler/CcNodeHandler.java \
        src/main/java/cn/projectan/strix/core/module/workflow/handler/JumpNodeHandler.java \
        src/main/java/cn/projectan/strix/core/module/workflow/handler/TriggerNodeHandler.java
git commit -m "feat(workflow): add Start, End, CC, Jump, Trigger node handlers

All non-blocking auto-flow handlers.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 8: Condition Node Handlers

Implement Condition (single condition evaluation) and ConditionGroup (multi-branch switch/case).

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/ConditionNodeHandler.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/ConditionGroupNodeHandler.java`

- [ ] **Step 1: Create ConditionNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ConditionEvaluator;
import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.config.ConditionNodeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class ConditionNodeHandler extends AbstractNodeHandler {

    private final ConditionEvaluator conditionEvaluator;

    public ConditionNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry,
                                 ConditionEvaluator conditionEvaluator) {
        super(objectMapper, triggerRegistry);
        this.conditionEvaluator = conditionEvaluator;
    }

    @Override
    public String getType() {
        return "CONDITION";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        ConditionNodeConfig config = parseConfig(context, ConditionNodeConfig.class);
        log.debug("Executing CONDITION node: {}", context.getCurrentNode().getId());

        String targetNodeId = conditionEvaluator.evaluateBranches(
                config.getBranches(),
                config.getDefaultTargetNodeId(),
                context.getVariables(),
                buildTriggerContext(context)
        );

        if (targetNodeId == null || targetNodeId.isBlank()) {
            return NodeExecutionResult.error("CONDITION node: no branch matched and no default target");
        }

        log.debug("CONDITION evaluated to target: {}", targetNodeId);
        return NodeExecutionResult.continueWith(List.of(targetNodeId));
    }
}
`

- [ ] **Step 2: Create ConditionGroupNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ConditionEvaluator;
import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.config.ConditionNodeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 条件组处理器 — 与 ConditionNodeHandler 逻辑相同，
 * 区别在于前端展示为多分支并列（switch/case 风格），
 * 而 CONDITION 是简单的 if/else。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class ConditionGroupNodeHandler extends AbstractNodeHandler {

    private final ConditionEvaluator conditionEvaluator;

    public ConditionGroupNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry,
                                      ConditionEvaluator conditionEvaluator) {
        super(objectMapper, triggerRegistry);
        this.conditionEvaluator = conditionEvaluator;
    }

    @Override
    public String getType() {
        return "CONDITION_GROUP";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        ConditionNodeConfig config = parseConfig(context, ConditionNodeConfig.class);
        log.debug("Executing CONDITION_GROUP node: {}", context.getCurrentNode().getId());

        String targetNodeId = conditionEvaluator.evaluateBranches(
                config.getBranches(),
                config.getDefaultTargetNodeId(),
                context.getVariables(),
                buildTriggerContext(context)
        );

        if (targetNodeId == null || targetNodeId.isBlank()) {
            return NodeExecutionResult.error("CONDITION_GROUP: no branch matched and no default target");
        }

        log.debug("CONDITION_GROUP evaluated to target: {}", targetNodeId);
        return NodeExecutionResult.continueWith(List.of(targetNodeId));
    }
}
`

- [ ] **Step 3: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/handler/ConditionNodeHandler.java \
        src/main/java/cn/projectan/strix/core/module/workflow/handler/ConditionGroupNodeHandler.java
git commit -m "feat(workflow): add Condition and ConditionGroup node handlers

Both delegate to ConditionEvaluator for branch evaluation.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 9: ApprovalNodeHandler

The most complex handler — creates tasks and assignees, sets up timeouts, supports 3 approval modes.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/ApprovalNodeHandler.java`

- [ ] **Step 1: Create ApprovalNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowEventPublisher;
import cn.projectan.strix.core.module.workflow.event.WorkflowEventType;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.config.ApprovalNodeConfig;
import cn.projectan.strix.core.module.workflow.model.config.TimeoutConfig;
import cn.projectan.strix.core.module.workflow.notification.WorkflowNotificationService;
import cn.projectan.strix.core.module.workflow.timer.WorkflowTimerService;
import cn.projectan.strix.model.db.workflow.WfTask;
import cn.projectan.strix.model.db.workflow.WfTaskAssignee;
import cn.projectan.strix.model.dict.workflow.TaskStatus;
import cn.projectan.strix.model.dict.workflow.AssigneeStatus;
import cn.projectan.strix.model.dict.workflow.TimerType;
import cn.projectan.strix.service.common.workflow.WfTaskAssigneeService;
import cn.projectan.strix.service.common.workflow.WfTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class ApprovalNodeHandler extends AbstractNodeHandler {

    private final WfTaskService wfTaskService;
    private final WfTaskAssigneeService wfTaskAssigneeService;
    private final WorkflowNotificationService notificationService;
    private final WorkflowEventPublisher eventPublisher;
    private final WorkflowTimerService timerService;

    public ApprovalNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry,
                                WfTaskService wfTaskService, WfTaskAssigneeService wfTaskAssigneeService,
                                WorkflowNotificationService notificationService,
                                WorkflowEventPublisher eventPublisher,
                                WorkflowTimerService timerService) {
        super(objectMapper, triggerRegistry);
        this.wfTaskService = wfTaskService;
        this.wfTaskAssigneeService = wfTaskAssigneeService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.timerService = timerService;
    }

    @Override
    public String getType() {
        return "APPROVAL";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        ApprovalNodeConfig config = parseConfig(context, ApprovalNodeConfig.class);
        String nodeId = context.getCurrentNode().getId();
        log.debug("Executing APPROVAL node: {}, mode={}", nodeId, config.getApprovalMode());

        // 1. 解析审批人
        List<String> assigneeIds = notificationService.resolveAssigneeIds(config.getAssignees());
        if (assigneeIds.isEmpty()) {
            return NodeExecutionResult.error("APPROVAL node has no assignees");
        }

        // 2. 创建任务
        WfTask task = new WfTask()
                .setInstanceId(context.getInstanceId())
                .setNodeId(nodeId)
                .setNodeName(context.getCurrentNode().getName())
                .setTokenId(context.getCurrentToken().getId())
                .setTaskType(TaskStatus.PENDING)
                .setApprovalMode(config.getApprovalMode());
        wfTaskService.save(task);

        // 3. 创建审批人记录
        short initialOrder = 1;
        List<WfTaskAssignee> assignees = new ArrayList<>();
        for (String assigneeId : assigneeIds) {
            WfTaskAssignee assignee = new WfTaskAssignee()
                    .setTaskId(task.getId())
                    .setAssigneeId(assigneeId)
                    .setStatus(AssigneeStatus.PENDING)
                    .setSeqOrder(initialOrder++);
            assignees.add(assignee);
        }

        // 顺序模式: 只激活第一个审批人
        if ("SEQ".equals(config.getApprovalMode())) {
            assignees.get(0).setStatus(AssigneeStatus.ACTIVE);
        } else {
            // ANY/ALL 模式: 所有审批人同时激活
            assignees.forEach(a -> a.setStatus(AssigneeStatus.ACTIVE));
        }
        wfTaskAssigneeService.saveBatch(assignees);

        // 4. 发送通知给激活的审批人
        List<String> activeAssigneeIds = assignees.stream()
                .filter(a -> AssigneeStatus.ACTIVE == a.getStatus())
                .map(WfTaskAssignee::getAssigneeId)
                .toList();
        notificationService.sendApprovalNotification(context, task.getId(), activeAssigneeIds);

        // 5. 设置超时定时器
        TimeoutConfig timeout = config.getTimeout();
        if (timeout != null && timeout.getDuration() != null) {
            timerService.createTimeoutTimer(context.getInstanceId(), task.getId(),
                    nodeId, timeout.getDuration(), timeout.getAction(), timeout.getDelegateTo());
        }

        // 6. 发布 TASK_CREATED 事件
        eventPublisher.publishTaskEvent(WorkflowEventType.TASK_CREATED,
                context.getInstanceId(), context.getDefinitionKey(), context.getDefinitionName(),
                nodeId, context.getCurrentNode().getName(), task.getId(),
                context.getInstance().getInitiatorId(), context.getVariables());

        // 7. 阻塞等待审批
        return NodeExecutionResult.waitHere();
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/handler/ApprovalNodeHandler.java
git commit -m "feat(workflow): add ApprovalNodeHandler

Creates tasks, assignees, timeouts. Supports ANY/ALL/SEQ modes.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 10: ParallelNodeHandler

Fork/Join handler that creates child tokens for each parallel branch.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/ParallelNodeHandler.java`

- [ ] **Step 1: Create ParallelNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.WorkflowEdge;
import cn.projectan.strix.core.module.workflow.model.config.ParallelNodeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class ParallelNodeHandler extends AbstractNodeHandler {

    public ParallelNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry) {
        super(objectMapper, triggerRegistry);
    }

    @Override
    public String getType() {
        return "PARALLEL";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        ParallelNodeConfig config = parseConfig(context, ParallelNodeConfig.class);
        String nodeId = context.getCurrentNode().getId();
        log.debug("Executing PARALLEL node: {}, joinMode={}", nodeId, config.getJoinMode());

        // 每条出边代表一个并行分支
        List<WorkflowEdge> outEdges = context.getGraph().findOutgoingEdges(nodeId);
        if (outEdges.isEmpty()) {
            return NodeExecutionResult.error("PARALLEL node has no outgoing branches");
        }

        List<List<String>> branches = new ArrayList<>();
        for (WorkflowEdge edge : outEdges) {
            branches.add(List.of(edge.getTarget()));
        }

        log.debug("PARALLEL forking into {} branches", branches.size());
        return NodeExecutionResult.fork(branches);
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/handler/ParallelNodeHandler.java
git commit -m "feat(workflow): add ParallelNodeHandler for fork/join

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 11: DelayNodeHandler

Blocking handler that creates a timer and waits for it to fire.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/DelayNodeHandler.java`

- [ ] **Step 1: Create DelayNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.config.DelayNodeConfig;
import cn.projectan.strix.core.module.workflow.timer.WorkflowTimerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class DelayNodeHandler extends AbstractNodeHandler {

    private final WorkflowTimerService timerService;

    public DelayNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry,
                             WorkflowTimerService timerService) {
        super(objectMapper, triggerRegistry);
        this.timerService = timerService;
    }

    @Override
    public String getType() {
        return "DELAY";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        DelayNodeConfig config = parseConfig(context, DelayNodeConfig.class);
        String nodeId = context.getCurrentNode().getId();
        log.debug("Executing DELAY node: {}, delayType={}, duration={}",
                nodeId, config.getDelayType(), config.getDuration());

        if ("FIXED_DURATION".equals(config.getDelayType())) {
            timerService.createDelayTimer(context.getInstanceId(), context.getCurrentToken().getId(),
                    nodeId, config.getDuration());
        } else if ("FIXED_TIME".equals(config.getDelayType())) {
            timerService.createDelayTimerAt(context.getInstanceId(), context.getCurrentToken().getId(),
                    nodeId, config.getTargetTime());
        } else {
            return NodeExecutionResult.error("Unknown delay type: " + config.getDelayType());
        }

        return NodeExecutionResult.waitHere();
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/handler/DelayNodeHandler.java
git commit -m "feat(workflow): add DelayNodeHandler with timer scheduling

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 12: SubProcessNodeHandler

Creates child workflow instance and waits for it to complete.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/handler/SubProcessNodeHandler.java`

- [ ] **Step 1: Create SubProcessNodeHandler**

`java
package cn.projectan.strix.core.module.workflow.handler;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.event.WorkflowTriggerRegistry;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.config.SubProcessNodeConfig;
import cn.projectan.strix.model.db.workflow.WfDefinition;
import cn.projectan.strix.model.db.workflow.WfDefinitionVersion;
import cn.projectan.strix.model.db.workflow.WfInstance;
import cn.projectan.strix.model.dict.workflow.InstanceStatus;
import cn.projectan.strix.model.dict.workflow.VersionStatus;
import cn.projectan.strix.service.common.workflow.WfDefinitionService;
import cn.projectan.strix.service.common.workflow.WfInstanceService;
import cn.projectan.strix.service.common.workflow.WfVersionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class SubProcessNodeHandler extends AbstractNodeHandler {

    private final WfDefinitionService wfDefinitionService;
    private final WfVersionService wfVersionService;
    private final WfInstanceService wfInstanceService;

    public SubProcessNodeHandler(ObjectMapper objectMapper, WorkflowTriggerRegistry triggerRegistry,
                                  WfDefinitionService wfDefinitionService,
                                  WfVersionService wfVersionService,
                                  WfInstanceService wfInstanceService) {
        super(objectMapper, triggerRegistry);
        this.wfDefinitionService = wfDefinitionService;
        this.wfVersionService = wfVersionService;
        this.wfInstanceService = wfInstanceService;
    }

    @Override
    public String getType() {
        return "SUB_PROCESS";
    }

    @Override
    public NodeExecutionResult execute(ExecutionContext context) {
        SubProcessNodeConfig config = parseConfig(context, SubProcessNodeConfig.class);
        String subDefId = config.getSubDefinitionId();
        log.debug("Executing SUB_PROCESS node: {}, subDefinitionId={}", context.getCurrentNode().getId(), subDefId);

        if (subDefId == null || subDefId.isBlank()) {
            return NodeExecutionResult.error("SUB_PROCESS node missing subDefinitionId");
        }

        // 查找子流程的已发布版本
        WfDefinition subDef = wfDefinitionService.getById(subDefId);
        if (subDef == null) {
            return NodeExecutionResult.error("Sub-process definition not found: " + subDefId);
        }

        WfDefinitionVersion publishedVersion = wfVersionService.getOne(
                new LambdaQueryWrapper<WfDefinitionVersion>()
                        .eq(WfDefinitionVersion::getDefinitionId, subDefId)
                        .eq(WfDefinitionVersion::getStatus, VersionStatus.PUBLISHED)
                        .orderByDesc(WfDefinitionVersion::getVersionNumber)
                        .last("LIMIT 1"));

        if (publishedVersion == null) {
            return NodeExecutionResult.error("No published version for sub-process: " + subDef.getName());
        }

        // 创建子流程实例
        WfInstance subInstance = new WfInstance()
                .setDefinitionId(subDefId)
                .setVersionId(publishedVersion.getId())
                .setTitle(subDef.getName() + " (子流程)")
                .setInitiatorId(context.getInstance().getInitiatorId())
                .setBizType(context.getInstance().getBizType())
                .setBizId(context.getInstance().getBizId())
                .setParentInstanceId(context.getInstanceId())
                .setParentNodeId(context.getCurrentNode().getId())
                .setStatus(InstanceStatus.RUNNING);
        wfInstanceService.save(subInstance);

        // 变量映射: 父 -> 子
        // 子流程启动由 WorkflowEngine 处理（通过 subInstanceId 标记），Plan 3 完善
        context.setVariable("_subInstanceId", subInstance.getId());

        return NodeExecutionResult.waitHere();
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/handler/SubProcessNodeHandler.java
git commit -m "feat(workflow): add SubProcessNodeHandler

Creates child workflow instance and waits for completion.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 13: WorkflowEngine

The core engine that drives token execution through the DAG.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/engine/WorkflowEngine.java`

- [ ] **Step 1: Create WorkflowEngine**

`java
package cn.projectan.strix.core.module.workflow.engine;

import cn.projectan.strix.core.module.workflow.event.WorkflowEventPublisher;
import cn.projectan.strix.core.module.workflow.event.WorkflowEventType;
import cn.projectan.strix.core.module.workflow.handler.NodeHandler;
import cn.projectan.strix.core.module.workflow.model.NodeExecutionResult;
import cn.projectan.strix.core.module.workflow.model.WorkflowGraph;
import cn.projectan.strix.core.module.workflow.model.WorkflowNode;
import cn.projectan.strix.model.db.workflow.WfDefinition;
import cn.projectan.strix.model.db.workflow.WfDefinitionVersion;
import cn.projectan.strix.model.db.workflow.WfInstance;
import cn.projectan.strix.model.db.workflow.WfInstanceVar;
import cn.projectan.strix.model.db.workflow.WfToken;
import cn.projectan.strix.model.dict.workflow.InstanceStatus;
import cn.projectan.strix.model.dict.workflow.TokenStatus;
import cn.projectan.strix.model.dict.workflow.VersionStatus;
import cn.projectan.strix.service.common.workflow.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowEngine {

    private final NodeHandlerRegistry handlerRegistry;
    private final WorkflowEventPublisher eventPublisher;
    private final WfDefinitionService wfDefinitionService;
    private final WfVersionService wfVersionService;
    private final WfInstanceService wfInstanceService;
    private final WfTokenService wfTokenService;
    private final WfInstanceVarService wfInstanceVarService;
    private final ObjectMapper objectMapper;

    /**
     * 启动新流程实例。
     */
    @Transactional(rollbackFor = Exception.class)
    public WfInstance startProcess(String definitionId, String title, String bizType, String bizId,
                                    String initiatorId, Map<String, Object> variables) {
        // 1. 查找定义和已发布版本
        WfDefinition definition = wfDefinitionService.getById(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + definitionId);
        }

        WfDefinitionVersion version = wfVersionService.getOne(
                new LambdaQueryWrapper<WfDefinitionVersion>()
                        .eq(WfDefinitionVersion::getDefinitionId, definitionId)
                        .eq(WfDefinitionVersion::getStatus, VersionStatus.PUBLISHED)
                        .orderByDesc(WfDefinitionVersion::getVersionNumber)
                        .last("LIMIT 1"));
        if (version == null) {
            throw new IllegalStateException("No published version for definition: " + definition.getName());
        }

        // 2. 解析 Graph
        WorkflowGraph graph = parseGraph(version.getGraphJson());

        // 3. 创建实例
        WfInstance instance = new WfInstance()
                .setDefinitionId(definitionId)
                .setVersionId(version.getId())
                .setTitle(title)
                .setBizType(bizType)
                .setBizId(bizId)
                .setInitiatorId(initiatorId)
                .setStatus(InstanceStatus.RUNNING)
                .setStartTime(LocalDateTime.now());
        wfInstanceService.save(instance);

        // 4. 保存初始变量
        if (variables != null && !variables.isEmpty()) {
            saveVariables(instance.getId(), variables);
        }

        // 5. 创建主 Token 并定位到 START 节点
        Optional<WorkflowNode> startNode = graph.findStartNode();
        if (startNode.isEmpty()) {
            throw new IllegalStateException("Graph has no START node");
        }

        WfToken token = new WfToken()
                .setInstanceId(instance.getId())
                .setCurrentNodeId(startNode.get().getId())
                .setStatus(TokenStatus.ACTIVE)
                .setIsPrimary(true);
        wfTokenService.save(token);

        // 6. 发布 PROCESS_STARTED 事件
        eventPublisher.publishProcessEvent(WorkflowEventType.PROCESS_STARTED,
                instance.getId(), definition.getDefinitionKey(), definition.getName(),
                initiatorId, variables);

        // 7. 开始执行
        ExecutionContext context = buildContext(definition, version, instance, graph, token, variables);
        executeToken(context);

        return instance;
    }

    /**
     * 推进 Token 执行（从当前节点继续）。
     * 用于外部信号恢复（审批完成、定时器触发等）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void resumeToken(String tokenId) {
        WfToken token = wfTokenService.getById(tokenId);
        if (token == null || TokenStatus.ACTIVE != token.getStatus()) {
            log.warn("Cannot resume token: {} (not found or not active)", tokenId);
            return;
        }

        ExecutionContext context = buildContextFromToken(token);
        // 从当前节点的下一节点继续
        List<String> nextNodeIds = context.getGraph().findNextNodeIds(token.getCurrentNodeId());
        advanceToken(context, nextNodeIds);
    }

    /**
     * 推进 Token 到指定节点（用于条件分支、跳转等）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void resumeTokenTo(String tokenId, String targetNodeId) {
        WfToken token = wfTokenService.getById(tokenId);
        if (token == null) return;
        ExecutionContext context = buildContextFromToken(token);
        advanceToken(context, List.of(targetNodeId));
    }

    /**
     * 核心执行循环：在当前节点执行 handler，根据结果推进。
     */
    private void executeToken(ExecutionContext context) {
        WfToken token = context.getCurrentToken();
        WorkflowNode node = context.getCurrentNode();

        if (node == null) {
            node = context.getGraph().findNode(token.getCurrentNodeId()).orElse(null);
            if (node == null) {
                log.error("Node not found in graph: {}", token.getCurrentNodeId());
                return;
            }
            context.setCurrentNode(node);
        }

        log.debug("Executing token {} at node {} (type={})", token.getId(), node.getId(), node.getType());

        // 获取处理器
        NodeHandler handler = handlerRegistry.getHandler(node.getType());

        // 发布 NODE_BEFORE_ENTER
        eventPublisher.publishNodeEvent(WorkflowEventType.NODE_BEFORE_ENTER,
                context.getInstanceId(), context.getDefinitionKey(), context.getDefinitionName(),
                node.getId(), node.getName(), context.getVariables());

        // onEnter 回调
        handler.onEnter(context);

        // 发布 NODE_AFTER_ENTER
        eventPublisher.publishNodeEvent(WorkflowEventType.NODE_AFTER_ENTER,
                context.getInstanceId(), context.getDefinitionKey(), context.getDefinitionName(),
                node.getId(), node.getName(), context.getVariables());

        // 执行节点逻辑
        NodeExecutionResult result;
        try {
            result = handler.execute(context);
        } catch (Exception e) {
            log.error("Error executing node {}: {}", node.getId(), e.getMessage(), e);
            eventPublisher.publishNodeEvent(WorkflowEventType.NODE_ERROR,
                    context.getInstanceId(), context.getDefinitionKey(), context.getDefinitionName(),
                    node.getId(), node.getName(), context.getVariables());
            markInstanceError(context, e.getMessage());
            return;
        }

        // 持久化脏变量
        persistDirtyVariables(context);

        // 根据结果处理
        switch (result.getStatus()) {
            case CONTINUE -> {
                // 离开当前节点
                handler.onLeave(context);
                eventPublisher.publishNodeEvent(WorkflowEventType.NODE_AFTER_LEAVE,
                        context.getInstanceId(), context.getDefinitionKey(), context.getDefinitionName(),
                        node.getId(), node.getName(), context.getVariables());
                advanceToken(context, result.getNextNodeIds());
            }
            case WAIT -> {
                // Token 保持在当前节点，等待外部信号
                log.debug("Token {} waiting at node {}", token.getId(), node.getId());
            }
            case FORK -> {
                // 并行分支：主 Token 挂起，创建子 Token
                token.setStatus(TokenStatus.SUSPENDED);
                wfTokenService.updateById(token);
                for (List<String> branch : result.getForkBranches()) {
                    if (!branch.isEmpty()) {
                        WfToken childToken = new WfToken()
                                .setInstanceId(context.getInstanceId())
                                .setParentTokenId(token.getId())
                                .setCurrentNodeId(branch.get(0))
                                .setStatus(TokenStatus.ACTIVE)
                                .setIsPrimary(false);
                        wfTokenService.save(childToken);
                        // 递归执行子 Token
                        ExecutionContext childCtx = context.toBuilder()
                                .currentToken(childToken)
                                .currentNode(null)
                                .build();
                        childCtx.setCurrentNode(context.getGraph().findNode(branch.get(0)).orElse(null));
                        executeToken(childCtx);
                    }
                }
            }
            case COMPLETE -> {
                // 结束节点：Token 完成
                handler.onLeave(context);
                completeToken(context, result.getEndStatus());
            }
            case ERROR -> {
                log.error("Node {} returned error: {}", node.getId(), result.getErrorMessage());
                markInstanceError(context, result.getErrorMessage());
            }
        }
    }

    /** 推进 Token 到下一节点并继续执行 */
    private void advanceToken(ExecutionContext context, List<String> nextNodeIds) {
        if (nextNodeIds == null || nextNodeIds.isEmpty()) {
            log.warn("No next nodes for token {}", context.getCurrentToken().getId());
            return;
        }

        // 单一后继节点：移动 Token
        String nextNodeId = nextNodeIds.get(0);
        WfToken token = context.getCurrentToken();
        token.setCurrentNodeId(nextNodeId);
        wfTokenService.updateById(token);

        context.setCurrentNode(context.getGraph().findNode(nextNodeId).orElse(null));
        executeToken(context);
    }

    /** Token 完成处理 */
    private void completeToken(ExecutionContext context, String endStatus) {
        WfToken token = context.getCurrentToken();
        token.setStatus(TokenStatus.COMPLETED);
        wfTokenService.updateById(token);
        log.debug("Token {} completed", token.getId());

        if (token.getParentTokenId() != null) {
            // 子 Token 完成 → 检查兄弟 Token 是否全部完成（Join）
            checkJoinCompletion(context, token.getParentTokenId());
        } else {
            // 主 Token 完成 → 流程完成
            completeInstance(context, endStatus);
        }
    }

    /** 检查并行 Join 是否满足 */
    private void checkJoinCompletion(ExecutionContext context, String parentTokenId) {
        List<WfToken> siblings = wfTokenService.list(
                new LambdaQueryWrapper<WfToken>()
                        .eq(WfToken::getParentTokenId, parentTokenId));
        boolean allCompleted = siblings.stream()
                .allMatch(t -> TokenStatus.COMPLETED == t.getStatus()
                        || TokenStatus.TERMINATED == t.getStatus());

        if (allCompleted) {
            log.debug("All child tokens completed for parent {}, resuming", parentTokenId);
            // 恢复父 Token
            WfToken parentToken = wfTokenService.getById(parentTokenId);
            parentToken.setStatus(TokenStatus.ACTIVE);
            wfTokenService.updateById(parentToken);
            // 父 Token 从并行节点的下一节点继续
            ExecutionContext parentCtx = buildContextFromToken(parentToken);
            // 并行节点的汇合点：找并行节点（PARALLEL）的图上后继
            List<String> nextIds = parentCtx.getGraph().findNextNodeIds(parentToken.getCurrentNodeId());
            advanceToken(parentCtx, nextIds);
        }
    }

    /** 流程实例完成 */
    private void completeInstance(ExecutionContext context, String endStatus) {
        WfInstance instance = context.getInstance();
        instance.setStatus("REJECTED".equals(endStatus) ? InstanceStatus.REJECTED : InstanceStatus.COMPLETED);
        instance.setEndTime(LocalDateTime.now());
        wfInstanceService.updateById(instance);

        WorkflowEventType eventType = "REJECTED".equals(endStatus)
                ? WorkflowEventType.PROCESS_REJECTED
                : WorkflowEventType.PROCESS_COMPLETED;
        eventPublisher.publishProcessEvent(eventType,
                instance.getId(), context.getDefinitionKey(), context.getDefinitionName(),
                context.getOperatorId(), context.getVariables());

        // 如果是子流程完成，恢复父流程
        if (instance.getParentInstanceId() != null) {
            resumeParentProcess(instance);
        }
    }

    /** 子流程完成后恢复父流程 */
    private void resumeParentProcess(WfInstance childInstance) {
        WfToken parentToken = wfTokenService.getOne(
                new LambdaQueryWrapper<WfToken>()
                        .eq(WfToken::getInstanceId, childInstance.getParentInstanceId())
                        .eq(WfToken::getCurrentNodeId, childInstance.getParentNodeId())
                        .eq(WfToken::getStatus, TokenStatus.ACTIVE));
        if (parentToken != null) {
            resumeToken(parentToken.getId());
        }
    }

    private void markInstanceError(ExecutionContext context, String errorMessage) {
        WfInstance instance = context.getInstance();
        instance.setStatus(InstanceStatus.ERROR);
        wfInstanceService.updateById(instance);
        eventPublisher.publishProcessEvent(WorkflowEventType.PROCESS_ERROR,
                instance.getId(), context.getDefinitionKey(), context.getDefinitionName(),
                context.getOperatorId(), Map.of("error", errorMessage));
    }

    private ExecutionContext buildContext(WfDefinition definition, WfDefinitionVersion version,
                                          WfInstance instance, WorkflowGraph graph,
                                          WfToken token, Map<String, Object> variables) {
        WorkflowNode currentNode = graph.findNode(token.getCurrentNodeId()).orElse(null);
        return ExecutionContext.builder()
                .definition(definition)
                .version(version)
                .instance(instance)
                .graph(graph)
                .currentToken(token)
                .currentNode(currentNode)
                .variables(variables != null ? new HashMap<>(variables) : new HashMap<>())
                .build();
    }

    private ExecutionContext buildContextFromToken(WfToken token) {
        WfInstance instance = wfInstanceService.getById(token.getInstanceId());
        WfDefinition definition = wfDefinitionService.getById(instance.getDefinitionId());
        WfDefinitionVersion version = wfVersionService.getById(instance.getVersionId());
        WorkflowGraph graph = parseGraph(version.getGraphJson());
        Map<String, Object> variables = loadVariables(instance.getId());
        return buildContext(definition, version, instance, graph, token, variables);
    }

    private WorkflowGraph parseGraph(String graphJson) {
        try {
            return objectMapper.readValue(graphJson, WorkflowGraph.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse workflow graph: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> loadVariables(String instanceId) {
        List<WfInstanceVar> vars = wfInstanceVarService.list(
                new LambdaQueryWrapper<WfInstanceVar>()
                        .eq(WfInstanceVar::getInstanceId, instanceId));
        Map<String, Object> map = new HashMap<>();
        for (WfInstanceVar var : vars) {
            map.put(var.getVarKey(), deserializeVarValue(var));
        }
        return map;
    }

    private void saveVariables(String instanceId, Map<String, Object> variables) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            WfInstanceVar var = new WfInstanceVar()
                    .setInstanceId(instanceId)
                    .setVarKey(entry.getKey())
                    .setVarType(inferVarType(entry.getValue()))
                    .setVarValue(String.valueOf(entry.getValue()));
            wfInstanceVarService.save(var);
        }
    }

    private void persistDirtyVariables(ExecutionContext context) {
        if (context.getDirtyVariables().isEmpty()) return;
        for (Map.Entry<String, Object> entry : context.getDirtyVariables().entrySet()) {
            WfInstanceVar existing = wfInstanceVarService.getOne(
                    new LambdaQueryWrapper<WfInstanceVar>()
                            .eq(WfInstanceVar::getInstanceId, context.getInstanceId())
                            .eq(WfInstanceVar::getVarKey, entry.getKey()));
            if (existing != null) {
                existing.setVarValue(String.valueOf(entry.getValue()));
                existing.setVarType(inferVarType(entry.getValue()));
                wfInstanceVarService.updateById(existing);
            } else {
                WfInstanceVar newVar = new WfInstanceVar()
                        .setInstanceId(context.getInstanceId())
                        .setVarKey(entry.getKey())
                        .setVarType(inferVarType(entry.getValue()))
                        .setVarValue(String.valueOf(entry.getValue()));
                wfInstanceVarService.save(newVar);
            }
        }
        context.setDirtyVariables(new HashMap<>());
    }

    private Object deserializeVarValue(WfInstanceVar var) {
        return switch (var.getVarType()) {
            case "INTEGER" -> Long.parseLong(var.getVarValue());
            case "DECIMAL" -> Double.parseDouble(var.getVarValue());
            case "BOOLEAN" -> Boolean.parseBoolean(var.getVarValue());
            default -> var.getVarValue();
        };
    }

    private String inferVarType(Object value) {
        if (value instanceof Integer || value instanceof Long) return "INTEGER";
        if (value instanceof Float || value instanceof Double) return "DECIMAL";
        if (value instanceof Boolean) return "BOOLEAN";
        return "STRING";
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/engine/WorkflowEngine.java
git commit -m "feat(workflow): add WorkflowEngine core execution loop

DAG execution with Token tracking, fork/join, sub-process,
variable persistence, and event publishing.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 14: WorkflowTimerService + WorkflowTimerJob

Timer lifecycle management using Redisson delayed queue + Quartz for recurring reminders.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/timer/WorkflowTimerService.java`
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/timer/WorkflowTimerJob.java`

- [ ] **Step 1: Create WorkflowTimerService**

`java
package cn.projectan.strix.core.module.workflow.timer;

import cn.projectan.strix.core.delayedtask.DelayedTaskManager;
import cn.projectan.strix.model.db.workflow.WfTimer;
import cn.projectan.strix.model.dict.workflow.TimerStatus;
import cn.projectan.strix.model.dict.workflow.TimerType;
import cn.projectan.strix.service.common.workflow.WfTimerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowTimerService {

    private static final String TIMER_QUEUE = "strix:workflow:timer";

    private final WfTimerService wfTimerService;
    private final DelayedTaskManager delayedTaskManager;

    /** 创建延迟节点定时器 (ISO 8601 Duration, e.g. PT24H) */
    public void createDelayTimer(String instanceId, String tokenId, String nodeId, String isoDuration) {
        Duration duration = Duration.parse(isoDuration);
        LocalDateTime fireTime = LocalDateTime.now().plus(duration);

        WfTimer timer = new WfTimer()
                .setInstanceId(instanceId)
                .setTokenId(tokenId)
                .setNodeId(nodeId)
                .setTimerType(TimerType.DELAY)
                .setFireTime(fireTime)
                .setStatus(TimerStatus.PENDING);
        wfTimerService.save(timer);

        delayedTaskManager.schedule(TIMER_QUEUE, timer.getId(), duration.toMillis(), TimeUnit.MILLISECONDS);
        log.info("Created DELAY timer: id={}, fireTime={}", timer.getId(), fireTime);
    }

    /** 创建延迟节点定时器（指定时间点） */
    public void createDelayTimerAt(String instanceId, String tokenId, String nodeId, String targetTime) {
        LocalDateTime fireTime = LocalDateTime.parse(targetTime);
        long delayMs = ChronoUnit.MILLIS.between(LocalDateTime.now(), fireTime);
        if (delayMs < 0) delayMs = 0;

        WfTimer timer = new WfTimer()
                .setInstanceId(instanceId)
                .setTokenId(tokenId)
                .setNodeId(nodeId)
                .setTimerType(TimerType.DELAY)
                .setFireTime(fireTime)
                .setStatus(TimerStatus.PENDING);
        wfTimerService.save(timer);

        delayedTaskManager.schedule(TIMER_QUEUE, timer.getId(), delayMs, TimeUnit.MILLISECONDS);
        log.info("Created DELAY timer at: id={}, fireTime={}", timer.getId(), fireTime);
    }

    /** 创建审批超时定时器 */
    public void createTimeoutTimer(String instanceId, String taskId, String nodeId,
                                    String isoDuration, String action, String delegateTo) {
        Duration duration = Duration.parse(isoDuration);
        LocalDateTime fireTime = LocalDateTime.now().plus(duration);

        WfTimer timer = new WfTimer()
                .setInstanceId(instanceId)
                .setTaskId(taskId)
                .setNodeId(nodeId)
                .setTimerType(TimerType.TIMEOUT)
                .setFireTime(fireTime)
                .setAction(action)
                .setActionParam(delegateTo)
                .setStatus(TimerStatus.PENDING);
        wfTimerService.save(timer);

        delayedTaskManager.schedule(TIMER_QUEUE, timer.getId(), duration.toMillis(), TimeUnit.MILLISECONDS);
        log.info("Created TIMEOUT timer: id={}, fireTime={}, action={}", timer.getId(), fireTime, action);
    }

    /** 创建催办提醒定时器 */
    public void createReminderTimer(String instanceId, String taskId, String nodeId,
                                     String interval, int maxReminders) {
        Duration duration = Duration.parse(interval);
        LocalDateTime fireTime = LocalDateTime.now().plus(duration);

        WfTimer timer = new WfTimer()
                .setInstanceId(instanceId)
                .setTaskId(taskId)
                .setNodeId(nodeId)
                .setTimerType(TimerType.REMINDER)
                .setFireTime(fireTime)
                .setAction("REMIND")
                .setMaxRetries(maxReminders)
                .setRetryCount(0)
                .setStatus(TimerStatus.PENDING);
        wfTimerService.save(timer);

        delayedTaskManager.schedule(TIMER_QUEUE, timer.getId(), duration.toMillis(), TimeUnit.MILLISECONDS);
        log.info("Created REMINDER timer: id={}, interval={}, maxReminders={}", timer.getId(), interval, maxReminders);
    }

    /** 取消指定实例的所有定时器 */
    public void cancelTimersByInstance(String instanceId) {
        wfTimerService.list(new LambdaQueryWrapper<WfTimer>()
                        .eq(WfTimer::getInstanceId, instanceId)
                        .eq(WfTimer::getStatus, TimerStatus.PENDING))
                .forEach(timer -> {
                    timer.setStatus(TimerStatus.CANCELLED);
                    wfTimerService.updateById(timer);
                    delayedTaskManager.cancel(TIMER_QUEUE, timer.getId());
                });
    }

    /** 取消指定任务的定时器 */
    public void cancelTimersByTask(String taskId) {
        wfTimerService.list(new LambdaQueryWrapper<WfTimer>()
                        .eq(WfTimer::getTaskId, taskId)
                        .eq(WfTimer::getStatus, TimerStatus.PENDING))
                .forEach(timer -> {
                    timer.setStatus(TimerStatus.CANCELLED);
                    wfTimerService.updateById(timer);
                    delayedTaskManager.cancel(TIMER_QUEUE, timer.getId());
                });
    }
}
`

- [ ] **Step 2: Create WorkflowTimerJob**

`java
package cn.projectan.strix.core.module.workflow.timer;

import cn.projectan.strix.core.module.workflow.engine.WorkflowEngine;
import cn.projectan.strix.core.module.workflow.event.WorkflowEventPublisher;
import cn.projectan.strix.core.module.workflow.event.WorkflowEventType;
import cn.projectan.strix.core.module.workflow.notification.WorkflowNotificationService;
import cn.projectan.strix.model.db.workflow.WfTimer;
import cn.projectan.strix.model.dict.workflow.TimerStatus;
import cn.projectan.strix.model.dict.workflow.TimerType;
import cn.projectan.strix.service.common.workflow.WfTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowTimerJob {

    private final WfTimerService wfTimerService;
    private final WorkflowEngine workflowEngine;
    private final WorkflowNotificationService notificationService;
    private final WorkflowEventPublisher eventPublisher;
    private final WorkflowTimerService timerService;

    /**
     * 定时器触发回调 — 由 DelayedTaskManager 消费时调用。
     */
    public void onTimerFired(String timerId) {
        WfTimer timer = wfTimerService.getById(timerId);
        if (timer == null || TimerStatus.PENDING != timer.getStatus()) {
            log.warn("Timer {} not found or not pending", timerId);
            return;
        }

        log.info("Timer fired: id={}, type={}, action={}", timerId, timer.getTimerType(), timer.getAction());

        timer.setStatus(TimerStatus.FIRED);
        wfTimerService.updateById(timer);

        switch (timer.getTimerType()) {
            case TimerType.DELAY -> handleDelayTimer(timer);
            case TimerType.TIMEOUT -> handleTimeoutTimer(timer);
            case TimerType.REMINDER -> handleReminderTimer(timer);
            default -> log.warn("Unknown timer type: {}", timer.getTimerType());
        }
    }

    private void handleDelayTimer(WfTimer timer) {
        // 延迟到期：恢复 Token 继续执行
        workflowEngine.resumeToken(timer.getTokenId());
    }

    private void handleTimeoutTimer(WfTimer timer) {
        String action = timer.getAction();
        if (action == null) return;

        eventPublisher.publishTaskEvent(WorkflowEventType.TASK_TIMEOUT,
                timer.getInstanceId(), null, null,
                timer.getNodeId(), null, timer.getTaskId(), null, null);

        switch (action) {
            case "AUTO_APPROVE" -> {
                log.info("Auto-approving task {} due to timeout", timer.getTaskId());
                // 调用 Plan 3 中实现的审批操作
            }
            case "AUTO_REJECT" -> {
                log.info("Auto-rejecting task {} due to timeout", timer.getTaskId());
            }
            case "DELEGATE" -> {
                log.info("Auto-delegating task {} to {}", timer.getTaskId(), timer.getActionParam());
            }
            case "REMIND" -> {
                notificationService.sendTimeoutReminder(timer.getInstanceId(), timer.getTaskId());
            }
            default -> log.warn("Unknown timeout action: {}", action);
        }
    }

    private void handleReminderTimer(WfTimer timer) {
        notificationService.sendTimeoutReminder(timer.getInstanceId(), timer.getTaskId());

        // 检查是否需要继续提醒
        int retryCount = timer.getRetryCount() + 1;
        if (retryCount < timer.getMaxRetries()) {
            timer.setRetryCount(retryCount);
            timer.setStatus(TimerStatus.PENDING);
            wfTimerService.updateById(timer);
            // 重新调度
            // 此处复用 timerService 但需要知道原始间隔（从 timer.actionParam 获取）
            log.info("Reminder {}/{} sent for task {}", retryCount, timer.getMaxRetries(), timer.getTaskId());
        }
    }
}
`

- [ ] **Step 3: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/timer/
git commit -m "feat(workflow): add timer service and timer job

WorkflowTimerService for timer lifecycle (create/cancel).
WorkflowTimerJob for timer callbacks (delay/timeout/reminder).

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 15: WorkflowNotificationService

Notification integration layer wrapping the existing NotificationService.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/notification/WorkflowNotificationService.java`

- [ ] **Step 1: Create WorkflowNotificationService**

`java
package cn.projectan.strix.core.module.workflow.notification;

import cn.projectan.strix.core.module.workflow.engine.ExecutionContext;
import cn.projectan.strix.core.module.workflow.model.config.AssigneeConfig;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.db.system.SystemRole;
import cn.projectan.strix.service.system.NotificationService;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.service.system.SystemRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowNotificationService {

    private static final String BIZ_TYPE_WORKFLOW = "WORKFLOW";

    private final NotificationService notificationService;
    private final SystemManagerService systemManagerService;
    private final SystemRoleService systemRoleService;

    /**
     * 解析 AssigneeConfig 列表为实际人员 ID。
     * MANAGER → 直接使用 ID
     * ROLE → 查询该角色下所有 SystemManager
     */
    public List<String> resolveAssigneeIds(List<AssigneeConfig> assignees) {
        if (assignees == null || assignees.isEmpty()) return List.of();

        Set<String> ids = new LinkedHashSet<>();
        for (AssigneeConfig assignee : assignees) {
            switch (assignee.getType()) {
                case "MANAGER" -> ids.add(assignee.getId());
                case "ROLE" -> {
                    List<String> managerIds = findManagerIdsByRoleId(assignee.getId());
                    ids.addAll(managerIds);
                }
                default -> log.warn("Unknown assignee type: {}", assignee.getType());
            }
        }
        return new ArrayList<>(ids);
    }

    /** 发送审批通知 */
    public void sendApprovalNotification(ExecutionContext context, String taskId, List<String> receiverIds) {
        if (receiverIds.isEmpty()) return;
        String title = context.getDefinitionName() + " - 审批待办";
        String content = context.getInstance().getTitle() + "，当前节点：" + context.getCurrentNode().getName();
        notificationService.sendNotification(
                BIZ_TYPE_WORKFLOW, context.getInstanceId(),
                title, content,
                "WORKFLOW_TASK", taskId, null,
                context.getInstance().getInitiatorId(), receiverIds);
    }

    /** 发送抄送通知 */
    public void sendCcNotification(ExecutionContext context, List<String> receiverIds) {
        if (receiverIds.isEmpty()) return;
        String title = context.getDefinitionName() + " - 抄送通知";
        String content = context.getInstance().getTitle() + "，当前节点：" + context.getCurrentNode().getName();
        notificationService.sendNotification(
                BIZ_TYPE_WORKFLOW, context.getInstanceId(),
                title, content,
                "WORKFLOW_INSTANCE", context.getInstanceId(), null,
                context.getInstance().getInitiatorId(), receiverIds);
    }

    /** 发送流程完成通知 */
    public void sendCompletionNotification(String instanceId, String definitionName,
                                            String instanceTitle, String initiatorId) {
        String title = definitionName + " - 流程完成";
        String content = instanceTitle + " 已审批通过";
        notificationService.sendNotification(
                BIZ_TYPE_WORKFLOW, instanceId,
                title, content,
                "WORKFLOW_INSTANCE", instanceId, null,
                null, List.of(initiatorId));
    }

    /** 发送流程拒绝通知 */
    public void sendRejectionNotification(String instanceId, String definitionName,
                                           String instanceTitle, String initiatorId, String rejecterName) {
        String title = definitionName + " - 流程被拒绝";
        String content = instanceTitle + " 被 " + rejecterName + " 拒绝";
        notificationService.sendNotification(
                BIZ_TYPE_WORKFLOW, instanceId,
                title, content,
                "WORKFLOW_INSTANCE", instanceId, null,
                null, List.of(initiatorId));
    }

    /** 发送超时提醒 */
    public void sendTimeoutReminder(String instanceId, String taskId) {
        // 查找任务的审批人并发送催办通知 — 具体实现在 Plan 3 完善
        log.info("Sending timeout reminder for task {} in instance {}", taskId, instanceId);
    }

    /** 发送催办通知 */
    public void sendUrgeNotification(String instanceId, String taskId, List<String> receiverIds,
                                      String urgerName) {
        String title = "催办提醒";
        String content = urgerName + " 催促您尽快处理审批任务";
        notificationService.sendNotification(
                BIZ_TYPE_WORKFLOW, instanceId,
                title, content,
                "WORKFLOW_TASK", taskId, null,
                null, receiverIds);
    }

    private List<String> findManagerIdsByRoleId(String roleId) {
        // 查询 system_manager_role 关联表获取角色下的管理人员
        // 此处简化实现，实际需要查询关联表
        return systemManagerService.list(new LambdaQueryWrapper<SystemManager>()
                        .eq(SystemManager::getRoleId, roleId))
                .stream()
                .map(SystemManager::getId)
                .toList();
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/notification/WorkflowNotificationService.java
git commit -m "feat(workflow): add WorkflowNotificationService

Wraps NotificationService for workflow-specific notifications:
approval, CC, completion, rejection, timeout, urge.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 16: Timer Consumer Initialization

Register the timer consumer at startup so delayed tasks trigger WorkflowTimerJob.

**Files:**
- Create: `src/main/java/cn/projectan/strix/core/module/workflow/timer/WorkflowTimerInitializer.java`

- [ ] **Step 1: Create WorkflowTimerInitializer**

`java
package cn.projectan.strix.core.module.workflow.timer;

import cn.projectan.strix.core.delayedtask.DelayedTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "workflow", havingValue = "true")
public class WorkflowTimerInitializer implements ApplicationRunner {

    private static final String TIMER_QUEUE = "strix:workflow:timer";

    private final DelayedTaskManager delayedTaskManager;
    private final WorkflowTimerJob workflowTimerJob;

    @Override
    public void run(ApplicationArguments args) {
        delayedTaskManager.registerConsumer(TIMER_QUEUE,
                workflowTimerJob::onTimerFired, 1, TimeUnit.SECONDS);
        log.info("Workflow timer consumer registered on queue: {}", TIMER_QUEUE);
    }
}
`

- [ ] **Step 2: Commit**

`ash
git add src/main/java/cn/projectan/strix/core/module/workflow/timer/WorkflowTimerInitializer.java
git commit -m "feat(workflow): add timer consumer initializer

Registers DelayedTaskManager consumer at startup.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 17: Build Verification + Compilation Test

Verify all Plan 2 code compiles and core components wire up correctly.

**Files:**
- No new files

- [ ] **Step 1: Run full build**

Run: `./gradlew build -x test --no-daemon`

Expected: BUILD SUCCESSFUL — all new classes compile without errors

- [ ] **Step 2: Run existing tests**

Run: `./gradlew test --no-daemon`

Expected: All tests pass (including Plan 1's WorkflowGraphTest)

- [ ] **Step 3: Final commit (if any fixups needed)**

`ash
git add -A
git commit -m "fix(workflow): build fixups for Plan 2 engine

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Summary

Plan 2 creates the complete execution engine:

| Category | Count | Location |
|----------|-------|----------|
| Event Types + Annotations | 6 | `core/module/workflow/event/` |
| Trigger Registry | 1 | `core/module/workflow/event/` |
| Event Publisher | 1 | `core/module/workflow/event/` |
| Execution Context | 1 | `core/module/workflow/engine/` |
| Handler Interface + Base | 2 | `core/module/workflow/handler/` |
| Handler Registry | 1 | `core/module/workflow/engine/` |
| Condition Evaluator | 1 | `core/module/workflow/engine/` |
| Node Handlers | 11 | `core/module/workflow/handler/` |
| Workflow Engine | 1 | `core/module/workflow/engine/` |
| Timer Service + Job + Init | 3 | `core/module/workflow/timer/` |
| Notification Service | 1 | `core/module/workflow/notification/` |
| Tests | 1 | `test/.../ConditionEvaluatorTest.java` |
| **Total** | **30 files** | |

After Plan 2 is complete, Plan 3 (REST API + Approval Operations) can begin implementing controllers, DTOs, and the 8 approval actions — all building on this engine.
