# Workflow Frontend Implementation Plan (Plan 4)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the complete workflow frontend: API layer, Pinia stores, card-based flow designer, approval workbench, instance management, and monitoring dashboard.

**Architecture:** Card-based flow designer (DingTalk/Feishu style) with pure DOM/CSS rendering, vue-draggable-plus for drag-and-drop, Pinia for state management with undo/redo support.

**Tech Stack:** Vue 3, TypeScript, Naive UI, Pinia, vue-draggable-plus

**Depends on:** Plan 1-3 (Backend) should be completed first, but frontend can start with mocked data.

**Frontend Project Path:** `Z:\Projects\VueProjects\StrixPage`

---

## File Structure

`
src/api/workflow.ts                                        # API layer with TypeScript types
src/stores/workflow.ts                                     # Designer state + undo/redo
src/utils/workflow-graph.ts                                # Graph ↔ Tree conversion utils
src/router/index.ts                                        # Modify: add workflow routes
src/views/System/SystemModule/Workflow/
  WorkflowDefinitionIndex.vue                              # Definition list (CRUD)
  WorkflowDesigner.vue                                     # Designer page (main composition)
  WorkflowTaskMine.vue                                     # My pending tasks
  WorkflowTaskDone.vue                                     # Completed tasks
  WorkflowTaskInitiated.vue                                # My initiated tasks
  WorkflowInstanceDetail.vue                               # Instance detail page
  WorkflowMonitor.vue                                      # Monitoring dashboard
  WorkflowDelegation.vue                                   # Delegation settings
  components/
    designer/
      DesignerCanvas.vue                                   # Recursive tree renderer
      NodeCard.vue                                         # Single node card UI
      NodeConnector.vue                                    # Vertical line + add button
      AddNodeMenu.vue                                      # Node type picker popover
      ConditionBranch.vue                                  # Multi-branch for conditions
      ParallelBranch.vue                                   # Multi-branch for parallel
    config/
      NodeConfigDrawer.vue                                 # Right drawer container
      ApprovalNodeConfig.vue                               # Approval node settings
      CcNodeConfig.vue                                     # CC node settings
      ConditionNodeConfig.vue                              # Condition rule builder
      DelayNodeConfig.vue                                  # Delay settings
      TriggerNodeConfig.vue                                # Trigger settings
      JumpNodeConfig.vue                                   # Jump target picker
    common/
      AssigneeSelector.vue                                 # Person/Role multi-picker
      TaskActionDialog.vue                                 # Approve/Reject/Return/etc modal
      InstanceTimeline.vue                                 # Instance event timeline
      InstanceFlowView.vue                                 # Read-only visual flow
`

**Total: ~30 files** (1 API, 1 store, 1 util, 1 modify router, 8 pages, ~18 components)

---

## Task 1: TypeScript Types + API Layer

All TypeScript interfaces and API methods for the workflow module.

**Files:**
- Create: `src/api/workflow.ts`

- [ ] **Step 1: Create workflow API file with types and methods**

`	ypescript
import type { RetResult } from './types'
import { http } from '@/plugins/axios'

// ==================== Type Definitions ====================

export type NodeType =
  | 'START' | 'END' | 'APPROVAL' | 'CC' | 'CONDITION' | 'CONDITION_GROUP'
  | 'PARALLEL' | 'DELAY' | 'TRIGGER' | 'JUMP' | 'SUB_PROCESS'

export type ApprovalMode = 'ANY' | 'ALL' | 'SEQUENTIAL'
export type AssigneeType = 'MANAGER' | 'ROLE' | 'INITIATOR' | 'INITIATOR_DEPT_LEADER'
export type InstanceStatus = 'RUNNING' | 'COMPLETED' | 'REJECTED' | 'CANCELLED' | 'SUSPENDED'
export type TaskStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'DELEGATED' | 'SKIPPED'

// ---- Graph model ----

export interface WorkflowNode {
  id: string
  type: NodeType
  name: string
  config: Record<string, any>
  x?: number
  y?: number
}

export interface WorkflowEdge {
  id: string
  sourceNodeId: string
  targetNodeId: string
  conditionExpression?: string
  sortOrder?: number
}

export interface WorkflowGraph {
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

// ---- Designer tree model (frontend only) ----

export interface DesignerBranch {
  id: string
  name: string
  conditionExpression?: string
  sortOrder: number
  children: DesignerTreeNode[]
}

export interface DesignerTreeNode {
  id: string
  type: NodeType
  name: string
  config: Record<string, any>
  branches?: DesignerBranch[]  // For CONDITION_GROUP / PARALLEL
  next?: DesignerTreeNode      // Sequential next node
}

// ---- Definition ----

export interface WfDefinition {
  id: string
  name: string
  key: string
  category?: string
  description?: string
  iconUrl?: string
  status: number
  latestVersionId?: string
  publishedVersionId?: string
  createdTime: string
  updatedTime: string
}

export interface WfDefinitionVersion {
  id: string
  definitionId: string
  versionNumber: number
  graphJson: string
  changeLog?: string
  status: number
  publishedTime?: string
  createdTime: string
}

// ---- Instance ----

export interface WfInstance {
  id: string
  definitionId: string
  versionId: string
  title: string
  initiatorId: string
  initiatorName?: string
  status: number
  bizType?: string
  bizId?: string
  startTime: string
  endTime?: string
  variables?: string
}

// ---- Task ----

export interface WfTask {
  id: string
  instanceId: string
  instanceTitle?: string
  definitionName?: string
  nodeId: string
  nodeName: string
  nodeType?: string
  taskType: number
  approvalMode: number
  createdTime: string
}

// ---- Task Assignee ----

export interface WfTaskAssignee {
  id: string
  taskId: string
  assigneeId: string
  assigneeName?: string
  status: number
  comment?: string
  operateTime?: string
}

// ---- Log ----

export interface WfLog {
  id: string
  instanceId: string
  nodeId: string
  nodeName: string
  action: string
  operatorId?: string
  operatorName?: string
  comment?: string
  detail?: string
  createdTime: string
}

// ---- Delegation ----

export interface WfDelegation {
  id: string
  delegatorId: string
  delegateId: string
  definitionId?: string
  startTime: string
  endTime: string
}

// ---- Trigger / Condition info ----

export interface TriggerItem {
  key: string
  name: string
  description: string
}

// ---- Stats ----

export interface WfStatsResp {
  totalDefinitions: number
  activeDefinitions: number
  runningInstances: number
  completedToday: number
  pendingTasks: number
  avgCompletionTime: number
}

// ---- List responses ----

export interface WfDefinitionListResp {
  items: WfDefinition[]
  total: number
}

export interface WfInstanceListResp {
  items: WfInstance[]
  total: number
}

export interface WfTaskListResp {
  items: WfTask[]
  total: number
}

export interface WfLogListResp {
  items: WfLog[]
  total: number
}

export interface WfVersionListResp {
  items: WfDefinitionVersion[]
  total: number
}

// ==================== API Methods ====================

const _n = '工作流'

export const workflowApi = {
  // ---- Definition CRUD ----
  urls: { definitionList: 'workflow/definitions' },

  definitionList: (params: Record<string, any>) =>
    http.get<RetResult<WfDefinitionListResp>>('workflow/definitions', {
      params,
      meta: { operate: 加载定义列表 }
    }),

  definitionDetail: (id: string) =>
    http.get<RetResult<WfDefinition>>(workflow/definitions/\, {
      meta: { operate: 加载定义 }
    }),

  definitionCreate: (data: { name: string; key: string; category?: string; description?: string }) =>
    http.post<RetResult<string>>('workflow/definitions', data, {
      meta: { operate: 创建定义 }
    }),

  definitionUpdate: (id: string, data: { name?: string; category?: string; description?: string; iconUrl?: string }) =>
    http.put<RetResult>(workflow/definitions/\, data, {
      meta: { operate: 更新定义 }
    }),

  definitionRemove: (id: string) =>
    http.delete<RetResult>(workflow/definitions/\, {
      meta: { operate: 删除定义 }
    }),

  definitionEnable: (id: string) =>
    http.post<RetResult>(workflow/definitions/\/enable, null, {
      meta: { operate: 启用 }
    }),

  definitionDisable: (id: string) =>
    http.post<RetResult>(workflow/definitions/\/disable, null, {
      meta: { operate: 停用 }
    }),

  // ---- Versions ----
  versionList: (definitionId: string, params?: Record<string, any>) =>
    http.get<RetResult<WfVersionListResp>>(workflow/definitions/\/versions, {
      params,
      meta: { operate: 加载版本列表 }
    }),

  versionDetail: (definitionId: string, versionId: string) =>
    http.get<RetResult<WfDefinitionVersion>>(workflow/definitions/\/versions/\, {
      meta: { operate: 加载版本详情 }
    }),

  versionSave: (definitionId: string, data: { graphJson: string; changeLog?: string }) =>
    http.post<RetResult<string>>(workflow/definitions/\/versions, data, {
      meta: { operate: 保存版本 }
    }),

  versionPublish: (definitionId: string, versionId: string) =>
    http.post<RetResult>(workflow/definitions/\/versions/\/publish, null, {
      meta: { operate: 发布版本 }
    }),

  // ---- Instances ----
  instanceList: (params: Record<string, any>) =>
    http.get<RetResult<WfInstanceListResp>>('workflow/instances', {
      params,
      meta: { operate: 加载流程实例列表 }
    }),

  instanceDetail: (id: string) =>
    http.get<RetResult<WfInstance>>(workflow/instances/\, {
      meta: { operate: 加载流程实例 }
    }),

  instanceStart: (data: { definitionId: string; title: string; bizType?: string; bizId?: string; variables?: Record<string, any> }) =>
    http.post<RetResult<string>>('workflow/instances', data, {
      meta: { operate: 发起流程 }
    }),

  instanceCancel: (id: string, data: { reason?: string }) =>
    http.post<RetResult>(workflow/instances/\/cancel, data, {
      meta: { operate: 撤销流程 }
    }),

  instanceSuspend: (id: string) =>
    http.post<RetResult>(workflow/instances/\/suspend, null, {
      meta: { operate: 挂起流程 }
    }),

  instanceResume: (id: string) =>
    http.post<RetResult>(workflow/instances/\/resume, null, {
      meta: { operate: 恢复流程 }
    }),

  instanceLogs: (id: string, params?: Record<string, any>) =>
    http.get<RetResult<WfLogListResp>>(workflow/instances/\/logs, {
      params,
      meta: { operate: 加载流程日志 }
    }),

  instanceGraph: (id: string) =>
    http.get<RetResult<{ graphJson: string; activeNodeIds: string[]; completedNodeIds: string[] }>>(workflow/instances/\/graph, {
      meta: { operate: 加载流程图 }
    }),

  // ---- Tasks ----
  tasksMine: (params?: Record<string, any>) =>
    http.get<RetResult<WfTaskListResp>>('workflow/tasks/mine', {
      params,
      meta: { operate: 加载我的待办 }
    }),

  tasksDone: (params?: Record<string, any>) =>
    http.get<RetResult<WfTaskListResp>>('workflow/tasks/done', {
      params,
      meta: { operate: 加载已处理 }
    }),

  tasksInitiated: (params?: Record<string, any>) =>
    http.get<RetResult<WfTaskListResp>>('workflow/tasks/initiated', {
      params,
      meta: { operate: 加载我发起的 }
    }),

  taskApprove: (id: string, data: { comment?: string }) =>
    http.post<RetResult>(workflow/tasks/\/approve, data, {
      meta: { operate: 通过审批 }
    }),

  taskReject: (id: string, data: { comment?: string; reason: string }) =>
    http.post<RetResult>(workflow/tasks/\/reject, data, {
      meta: { operate: 拒绝审批 }
    }),

  taskReturn: (id: string, data: { targetNodeId: string; comment?: string }) =>
    http.post<RetResult>(workflow/tasks/\/return, data, {
      meta: { operate: 回退 }
    }),

  taskDelegate: (id: string, data: { delegateId: string; comment?: string }) =>
    http.post<RetResult>(workflow/tasks/\/delegate, data, {
      meta: { operate: 转办 }
    }),

  taskCountersign: (id: string, data: { assigneeIds: string[]; comment?: string }) =>
    http.post<RetResult>(workflow/tasks/\/countersign, data, {
      meta: { operate: 加签 }
    }),

  taskRemoveSign: (id: string, data: { assigneeId: string; comment?: string }) =>
    http.post<RetResult>(workflow/tasks/\/remove-sign, data, {
      meta: { operate: 降签 }
    }),

  taskWithdraw: (id: string) =>
    http.post<RetResult>(workflow/tasks/\/withdraw, null, {
      meta: { operate: 撕回 }
    }),

  taskUrge: (id: string) =>
    http.post<RetResult>(workflow/tasks/\/urge, null, {
      meta: { operate: 催办 }
    }),

  // ---- Admin ----
  triggerList: () =>
    http.get<RetResult<{ items: TriggerItem[] }>>('workflow/triggers', {
      meta: { operate: 加载触发器列表 }
    }),

  conditionList: () =>
    http.get<RetResult<{ items: TriggerItem[] }>>('workflow/conditions', {
      meta: { operate: 加载条件列表 }
    }),

  delegationList: () =>
    http.get<RetResult<WfDelegation[]>>('workflow/delegations', {
      meta: { operate: 加载代理列表 }
    }),

  delegationCreate: (data: { delegateId: string; definitionId?: string; startTime: string; endTime: string }) =>
    http.post<RetResult<string>>('workflow/delegations', data, {
      meta: { operate: 设置代理 }
    }),

  delegationRemove: (id: string) =>
    http.delete<RetResult>(workflow/delegations/\, {
      meta: { operate: 撤销代理 }
    }),

  stats: () =>
    http.get<RetResult<WfStatsResp>>('workflow/stats', {
      meta: { operate: 加载统计数据 }
    }),
}
`

- [ ] **Step 2: Commit**

`ash
git add src/api/workflow.ts
git commit -m "feat(workflow): add workflow API layer with TypeScript types

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 2: Graph-Tree Conversion Utils + Pinia Store

Utilities for converting between flat graph (backend) and tree (frontend designer), plus the Pinia store with undo/redo.

**Files:**
- Create: `src/utils/workflow-graph.ts`
- Create: `src/stores/workflow.ts`

- [ ] **Step 1: Create graph-tree conversion utility**

`	ypescript
// src/utils/workflow-graph.ts
import { v4 as uuid } from 'uuid'
import type {
  WorkflowGraph, WorkflowNode, WorkflowEdge,
  DesignerTreeNode, DesignerBranch, NodeType
} from '@/api/workflow'

/**
 * Convert flat graph (backend) → tree (designer rendering).
 * Start from the START node and follow edges sequentially.
 * CONDITION_GROUP and PARALLEL nodes branch into multiple children.
 */
export function graphToTree(graph: WorkflowGraph): DesignerTreeNode {
  const nodeMap = new Map<string, WorkflowNode>()
  graph.nodes.forEach(n => nodeMap.set(n.id, n))

  // Build adjacency: sourceId → sorted target edges
  const edgesBySource = new Map<string, WorkflowEdge[]>()
  graph.edges.forEach(e => {
    const list = edgesBySource.get(e.sourceNodeId) || []
    list.push(e)
    edgesBySource.set(e.sourceNodeId, list)
  })
  // Sort edges by sortOrder
  edgesBySource.forEach(edges => edges.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)))

  const startNode = graph.nodes.find(n => n.type === 'START')
  if (!startNode) {
    return createDefaultTree()
  }

  // Find the END node so we know where branches reconverge
  const endNodeId = graph.nodes.find(n => n.type === 'END')?.id

  return buildSubtree(startNode.id, endNodeId)

  function buildSubtree(nodeId: string, stopAt?: string): DesignerTreeNode {
    const node = nodeMap.get(nodeId)!
    const treeNode: DesignerTreeNode = {
      id: node.id,
      type: node.type,
      name: node.name,
      config: node.config || {}
    }

    if (node.type === 'END') {
      return treeNode
    }

    const outEdges = edgesBySource.get(nodeId) || []

    if (node.type === 'CONDITION_GROUP' || node.type === 'PARALLEL') {
      // Each out-edge is a branch
      // Find the reconvergence point (node where all branches meet)
      const reconvergeId = findReconvergencePoint(nodeId, outEdges, endNodeId)

      treeNode.branches = outEdges.map((edge, idx) => {
        const branch: DesignerBranch = {
          id: edge.id,
          name: node.type === 'CONDITION_GROUP'
            ? (edge.conditionExpression ? 条件 \ : '默认')
            : 分支 \,
          conditionExpression: edge.conditionExpression,
          sortOrder: edge.sortOrder ?? idx,
          children: []
        }

        // Build chain from this edge target to reconvergence point
        let currentId: string | undefined = edge.targetNodeId
        while (currentId && currentId !== reconvergeId) {
          const child = buildSubtree(currentId, reconvergeId)
          branch.children.push(child)
          // Follow next edge (non-branch node has single out-edge)
          const nextEdges = edgesBySource.get(currentId) || []
          const childNode = nodeMap.get(currentId)
          if (childNode?.type === 'CONDITION_GROUP' || childNode?.type === 'PARALLEL') {
            // The subtree handles its own branching; follow reconvergence
            currentId = findReconvergencePoint(currentId, nextEdges, reconvergeId)
          } else {
            currentId = nextEdges.length > 0 ? nextEdges[0].targetNodeId : undefined
          }
        }

        return branch
      })

      // Continue after reconvergence
      if (reconvergeId) {
        treeNode.next = buildSubtree(reconvergeId, stopAt)
      }
    } else {
      // Sequential node: follow single out-edge
      if (outEdges.length > 0) {
        const nextId = outEdges[0].targetNodeId
        if (nextId !== stopAt) {
          treeNode.next = buildSubtree(nextId, stopAt)
        }
      }
    }

    return treeNode
  }

  function findReconvergencePoint(
    branchNodeId: string,
    outEdges: WorkflowEdge[],
    fallback?: string
  ): string | undefined {
    // Simple heuristic: find the first node that all branches can reach
    // For well-formed graphs, the reconvergence point is stored in config
    const node = nodeMap.get(branchNodeId)
    if (node?.config?.reconvergeNodeId) {
      return node.config.reconvergeNodeId
    }
    // Fallback: traverse all branches, find common descendant
    // For now, use the END node
    return fallback
  }
}

/**
 * Convert tree (designer) → flat graph (backend storage).
 */
export function treeToGraph(tree: DesignerTreeNode): WorkflowGraph {
  const nodes: WorkflowNode[] = []
  const edges: WorkflowEdge[] = []

  flattenNode(tree, undefined)

  return { nodes, edges }

  function flattenNode(treeNode: DesignerTreeNode, parentNodeId?: string): string {
    nodes.push({
      id: treeNode.id,
      type: treeNode.type,
      name: treeNode.name,
      config: { ...treeNode.config }
    })

    if (treeNode.branches && treeNode.branches.length > 0) {
      // Find or create reconvergence point
      let reconvergeId: string | undefined

      if (treeNode.next) {
        reconvergeId = treeNode.next.id
      } else {
        // Create implicit END connection
        reconvergeId = undefined
      }

      // Store reconverge reference in config
      if (reconvergeId) {
        const nodeIdx = nodes.findIndex(n => n.id === treeNode.id)
        nodes[nodeIdx].config = { ...nodes[nodeIdx].config, reconvergeNodeId: reconvergeId }
      }

      treeNode.branches.forEach((branch, idx) => {
        if (branch.children.length > 0) {
          // Edge from branch node to first child
          edges.push({
            id: branch.id,
            sourceNodeId: treeNode.id,
            targetNodeId: branch.children[0].id,
            conditionExpression: branch.conditionExpression,
            sortOrder: idx
          })

          // Flatten branch children sequentially
          let prevId = branch.children[0].id
          flattenNode(branch.children[0], treeNode.id)

          for (let i = 1; i < branch.children.length; i++) {
            edges.push({
              id: uuid(),
              sourceNodeId: prevId,
              targetNodeId: branch.children[i].id,
              sortOrder: 0
            })
            flattenNode(branch.children[i], prevId)
            prevId = branch.children[i].id
          }

          // Connect last child to reconvergence point
          if (reconvergeId) {
            edges.push({
              id: uuid(),
              sourceNodeId: prevId,
              targetNodeId: reconvergeId,
              sortOrder: 0
            })
          }
        } else if (reconvergeId) {
          // Empty branch: direct edge to reconvergence
          edges.push({
            id: branch.id,
            sourceNodeId: treeNode.id,
            targetNodeId: reconvergeId,
            conditionExpression: branch.conditionExpression,
            sortOrder: idx
          })
        }
      })

      // Flatten the reconvergence (next) node
      if (treeNode.next) {
        flattenNode(treeNode.next, treeNode.id)
      }
    } else if (treeNode.next) {
      // Sequential: edge to next
      edges.push({
        id: uuid(),
        sourceNodeId: treeNode.id,
        targetNodeId: treeNode.next.id,
        sortOrder: 0
      })
      flattenNode(treeNode.next, treeNode.id)
    }

    return treeNode.id
  }
}

/**
 * Create a default tree with START → END.
 */
export function createDefaultTree(): DesignerTreeNode {
  return {
    id: uuid(),
    type: 'START',
    name: '开始',
    config: {},
    next: {
      id: uuid(),
      type: 'END',
      name: '结束',
      config: {}
    }
  }
}

/**
 * Create a new node of given type with default config.
 */
export function createNode(type: NodeType): DesignerTreeNode {
  const defaults: Record<NodeType, { name: string; config: Record<string, any> }> = {
    START: { name: '开始', config: {} },
    END: { name: '结束', config: {} },
    APPROVAL: {
      name: '审批',
      config: {
        approvalMode: 'ANY',
        assigneeType: 'MANAGER',
        assigneeIds: [],
        allowDelegate: true,
        allowReturn: true,
        allowCountersign: true,
        allowWithdraw: true,
        timeoutHours: 0
      }
    },
    CC: {
      name: '抄送',
      config: {
        assigneeType: 'MANAGER',
        assigneeIds: []
      }
    },
    CONDITION: {
      name: '条件',
      config: {
        rules: [],
        logicOperator: 'AND'
      }
    },
    CONDITION_GROUP: {
      name: '条件分支',
      config: {}
    },
    PARALLEL: {
      name: '并行分支',
      config: {}
    },
    DELAY: {
      name: '延迟',
      config: {
        delayType: 'FIXED',
        delayValue: 1,
        delayUnit: 'HOURS'
      }
    },
    TRIGGER: {
      name: '触发器',
      config: {
        triggerKey: '',
        triggerParams: {}
      }
    },
    JUMP: {
      name: '跳转',
      config: {
        targetNodeId: ''
      }
    },
    SUB_PROCESS: {
      name: '子流程',
      config: {
        subDefinitionId: '',
        variableMapping: {}
      }
    }
  }

  const def = defaults[type]
  return {
    id: uuid(),
    type,
    name: def.name,
    config: { ...def.config },
    branches: (type === 'CONDITION_GROUP' || type === 'PARALLEL')
      ? [
          { id: uuid(), name: type === 'CONDITION_GROUP' ? '条件 1' : '分支 1', sortOrder: 0, children: [] },
          { id: uuid(), name: type === 'CONDITION_GROUP' ? '默认' : '分支 2', sortOrder: 1, children: [] }
        ]
      : undefined
  }
}

/**
 * Insert a new node after a given node in the tree (mutates tree in-place).
 * Returns true if insertion succeeded.
 */
export function insertNodeAfter(root: DesignerTreeNode, afterNodeId: string, newNode: DesignerTreeNode): boolean {
  if (root.id === afterNodeId) {
    newNode.next = root.next
    root.next = newNode
    return true
  }

  // Search in branches
  if (root.branches) {
    for (const branch of root.branches) {
      for (let i = 0; i < branch.children.length; i++) {
        if (branch.children[i].id === afterNodeId) {
          newNode.next = branch.children[i].next
          branch.children[i].next = newNode
          return true
        }
        if (insertNodeAfter(branch.children[i], afterNodeId, newNode)) {
          return true
        }
      }
    }
  }

  // Search in next
  if (root.next) {
    return insertNodeAfter(root.next, afterNodeId, newNode)
  }

  return false
}

/**
 * Remove a node from the tree by ID (mutates tree in-place).
 * Cannot remove START or END nodes.
 * Returns true if removal succeeded.
 */
export function removeNode(root: DesignerTreeNode, nodeId: string): boolean {
  // Check if next is the target
  if (root.next?.id === nodeId) {
    root.next = root.next.next
    return true
  }

  // Search in branches
  if (root.branches) {
    for (const branch of root.branches) {
      for (let i = 0; i < branch.children.length; i++) {
        if (branch.children[i].id === nodeId) {
          // Splice out: reconnect the chain
          if (branch.children[i].next) {
            branch.children.splice(i, 1, branch.children[i].next!)
          } else {
            branch.children.splice(i, 1)
          }
          return true
        }
        if (removeNode(branch.children[i], nodeId)) {
          return true
        }
      }
    }
  }

  if (root.next) {
    return removeNode(root.next, nodeId)
  }

  return false
}

/**
 * Find a node in the tree by ID.
 */
export function findNode(root: DesignerTreeNode, nodeId: string): DesignerTreeNode | undefined {
  if (root.id === nodeId) return root

  if (root.branches) {
    for (const branch of root.branches) {
      for (const child of branch.children) {
        const found = findNode(child, nodeId)
        if (found) return found
      }
    }
  }

  if (root.next) {
    return findNode(root.next, nodeId)
  }

  return undefined
}
`

- [ ] **Step 2: Create Pinia workflow store**

`	ypescript
// src/stores/workflow.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { DesignerTreeNode, WfDefinition, WfDefinitionVersion } from '@/api/workflow'
import { workflowApi } from '@/api/workflow'
import {
  createDefaultTree, graphToTree, treeToGraph,
  createNode, insertNodeAfter, removeNode, findNode
} from '@/utils/workflow-graph'
import type { NodeType, WorkflowGraph } from '@/api/workflow'

const MAX_UNDO_STEPS = 50

export const useWorkflowStore = defineStore('workflow', () => {
  // ---- Definition state ----
  const currentDefinition = ref<WfDefinition | null>(null)
  const currentVersion = ref<WfDefinitionVersion | null>(null)

  // ---- Designer tree ----
  const tree = ref<DesignerTreeNode>(createDefaultTree())
  const selectedNodeId = ref<string | null>(null)
  const isDirty = ref(false)

  // ---- Undo/Redo ----
  const undoStack = ref<string[]>([])
  const redoStack = ref<string[]>([])

  const canUndo = computed(() => undoStack.value.length > 0)
  const canRedo = computed(() => redoStack.value.length > 0)

  const selectedNode = computed(() => {
    if (!selectedNodeId.value) return null
    return findNode(tree.value, selectedNodeId.value) ?? null
  })

  function pushUndoState() {
    const snapshot = JSON.stringify(tree.value)
    undoStack.value.push(snapshot)
    if (undoStack.value.length > MAX_UNDO_STEPS) {
      undoStack.value.shift()
    }
    redoStack.value = []
    isDirty.value = true
  }

  function undo() {
    if (undoStack.value.length === 0) return
    redoStack.value.push(JSON.stringify(tree.value))
    const prev = undoStack.value.pop()!
    tree.value = JSON.parse(prev)
    isDirty.value = true
  }

  function redo() {
    if (redoStack.value.length === 0) return
    undoStack.value.push(JSON.stringify(tree.value))
    const next = redoStack.value.pop()!
    tree.value = JSON.parse(next)
    isDirty.value = true
  }

  // ---- Node operations ----

  function addNode(afterNodeId: string, type: NodeType) {
    pushUndoState()
    const newNode = createNode(type)
    insertNodeAfter(tree.value, afterNodeId, newNode)
    selectedNodeId.value = newNode.id
  }

  function deleteNode(nodeId: string) {
    pushUndoState()
    removeNode(tree.value, nodeId)
    if (selectedNodeId.value === nodeId) {
      selectedNodeId.value = null
    }
  }

  function updateNodeConfig(nodeId: string, config: Record<string, any>) {
    pushUndoState()
    const node = findNode(tree.value, nodeId)
    if (node) {
      node.config = { ...node.config, ...config }
    }
  }

  function updateNodeName(nodeId: string, name: string) {
    pushUndoState()
    const node = findNode(tree.value, nodeId)
    if (node) {
      node.name = name
    }
  }

  function selectNode(nodeId: string | null) {
    selectedNodeId.value = nodeId
  }

  // ---- Branch operations ----

  function addBranch(nodeId: string) {
    pushUndoState()
    const node = findNode(tree.value, nodeId)
    if (node && node.branches) {
      const idx = node.branches.length
      node.branches.push({
        id: crypto.randomUUID(),
        name: node.type === 'CONDITION_GROUP' ? 条件 \ : 分支 \,
        sortOrder: idx,
        children: []
      })
    }
  }

  function removeBranch(nodeId: string, branchId: string) {
    pushUndoState()
    const node = findNode(tree.value, nodeId)
    if (node && node.branches && node.branches.length > 2) {
      node.branches = node.branches.filter(b => b.id !== branchId)
    }
  }

  // ---- Load/Save ----

  async function loadDefinition(definitionId: string) {
    const { data: res } = await workflowApi.definitionDetail(definitionId)
    currentDefinition.value = res.data

    if (res.data.latestVersionId) {
      const { data: vRes } = await workflowApi.versionDetail(definitionId, res.data.latestVersionId)
      currentVersion.value = vRes.data
      if (vRes.data.graphJson) {
        const graph: WorkflowGraph = JSON.parse(vRes.data.graphJson)
        tree.value = graphToTree(graph)
      } else {
        tree.value = createDefaultTree()
      }
    } else {
      tree.value = createDefaultTree()
      currentVersion.value = null
    }

    undoStack.value = []
    redoStack.value = []
    selectedNodeId.value = null
    isDirty.value = false
  }

  async function saveVersion(changeLog?: string) {
    if (!currentDefinition.value) return
    const graph = treeToGraph(tree.value)
    const { data: res } = await workflowApi.versionSave(currentDefinition.value.id, {
      graphJson: JSON.stringify(graph),
      changeLog
    })
    currentVersion.value = {
      ...currentVersion.value!,
      id: res.data
    }
    isDirty.value = false
    return res.data
  }

  async function publishVersion() {
    if (!currentDefinition.value || !currentVersion.value) return
    await workflowApi.versionPublish(currentDefinition.value.id, currentVersion.value.id)
  }

  function resetDesigner() {
    tree.value = createDefaultTree()
    selectedNodeId.value = null
    undoStack.value = []
    redoStack.value = []
    isDirty.value = false
    currentDefinition.value = null
    currentVersion.value = null
  }

  return {
    currentDefinition, currentVersion, tree, selectedNodeId, isDirty,
    canUndo, canRedo, selectedNode,
    undo, redo,
    addNode, deleteNode, updateNodeConfig, updateNodeName, selectNode,
    addBranch, removeBranch,
    loadDefinition, saveVersion, publishVersion, resetDesigner
  }
})
`

- [ ] **Step 3: Commit**

`ash
git add src/utils/workflow-graph.ts src/stores/workflow.ts
git commit -m "feat(workflow): add graph utils and Pinia store with undo/redo

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 3: Vue Router Configuration

Add workflow routes to the existing router.

**Files:**
- Modify: `src/router/index.ts`

- [ ] **Step 1: Add workflow routes inside the SystemModule children array**

Find the `system/module` route group in `src/router/index.ts` and add the workflow children. The exact insertion point is inside the `children` array of the `SystemModule` route.

`	ypescript
// Add these route entries inside the SystemModule children array
{
  path: 'workflow',
  name: 'SystemModuleWorkflow',
  meta: { title: '工作流管理', empty: true },
  children: [
    {
      path: 'definition',
      name: 'WorkflowDefinitionIndex',
      component: () => import('@/views/System/SystemModule/Workflow/WorkflowDefinitionIndex.vue'),
      meta: {
        title: '流程定义',
        empty: false,
        permission: 'system:module:workflow'
      }
    },
    {
      path: 'designer/:definitionId',
      name: 'WorkflowDesigner',
      component: () => import('@/views/System/SystemModule/Workflow/WorkflowDesigner.vue'),
      meta: {
        title: '流程设计器',
        empty: false,
        parentRouteName: 'WorkflowDefinitionIndex',
        permission: 'system:module:workflow'
      }
    },
    {
      path: 'task/mine',
      name: 'WorkflowTaskMine',
      component: () => import('@/views/System/SystemModule/Workflow/WorkflowTaskMine.vue'),
      meta: {
        title: '我的待办',
        empty: false,
        permission: 'system:module:workflow'
      }
    },
    {
      path: 'task/done',
      name: 'WorkflowTaskDone',
      component: () => import('@/views/System/SystemModule/Workflow/WorkflowTaskDone.vue'),
      meta: {
        title: '已处理',
        empty: false,
        permission: 'system:module:workflow'
      }
    },
    {
      path: 'task/initiated',
      name: 'WorkflowTaskInitiated',
      component: () => import('@/views/System/SystemModule/Workflow/WorkflowTaskInitiated.vue'),
      meta: {
        title: '我发起的',
        empty: false,
        permission: 'system:module:workflow'
      }
    },
    {
      path: 'instance/:id',
      name: 'WorkflowInstanceDetail',
      component: () => import('@/views/System/SystemModule/Workflow/WorkflowInstanceDetail.vue'),
      meta: {
        title: '流程详情',
        empty: false,
        parentRouteName: 'WorkflowTaskMine',
        permission: 'system:module:workflow'
      }
    },
    {
      path: 'monitor',
      name: 'WorkflowMonitor',
      component: () => import('@/views/System/SystemModule/Workflow/WorkflowMonitor.vue'),
      meta: {
        title: '流程监控',
        empty: false,
        permission: 'system:module:workflow'
      }
    },
    {
      path: 'delegation',
      name: 'WorkflowDelegation',
      component: () => import('@/views/System/SystemModule/Workflow/WorkflowDelegation.vue'),
      meta: {
        title: '审批代理',
        empty: false,
        permission: 'system:module:workflow'
      }
    }
  ]
}
`

- [ ] **Step 2: Commit**

`ash
git add src/router/index.ts
git commit -m "feat(workflow): add workflow routes

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 4: Card Designer - Core Components

The heart of the designer: recursive tree rendering with card-based nodes.

**Files:**
- Create: `src/views/System/SystemModule/Workflow/components/designer/NodeCard.vue`
- Create: `src/views/System/SystemModule/Workflow/components/designer/AddNodeMenu.vue`
- Create: `src/views/System/SystemModule/Workflow/components/designer/NodeConnector.vue`
- Create: `src/views/System/SystemModule/Workflow/components/designer/DesignerCanvas.vue`

- [ ] **Step 1: Create NodeCard component**

Individual node card with icon, title, description, and actions.

`ue
<!-- src/views/System/SystemModule/Workflow/components/designer/NodeCard.vue -->
<template>
  <div
    class="wf-node-card"
    :class="[
      wf-node-card--\,
      { 'wf-node-card--selected': isSelected }
    ]"
    @click.stop="handleClick"
  >
    <div class="wf-node-card__header">
      <div class="wf-node-card__icon">
        <component :is="nodeIcon" :size="16" />
      </div>
      <span class="wf-node-card__title">{{ node.name }}</span>
      <div v-if="showActions" class="wf-node-card__actions">
        <n-button
          v-if="canDelete"
          text
          type="error"
          size="tiny"
          @click.stop="emit('delete', node.id)"
        >
          <template #icon><Trash2 :size="14" /></template>
        </n-button>
      </div>
    </div>
    <div class="wf-node-card__body">
      <span class="wf-node-card__desc">{{ description }}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { DesignerTreeNode, NodeType } from '@/api/workflow'
import { useWorkflowStore } from '@/stores/workflow'
import {
  Play, Square, UserCheck, Send, GitBranch, GitFork,
  Clock, Zap, CornerUpRight, Workflow, Trash2
} from 'lucide-vue-next'

const props = defineProps<{
  node: DesignerTreeNode
  readonly?: boolean
}>()

const emit = defineEmits<{
  delete: [nodeId: string]
}>()

const store = useWorkflowStore()
const isSelected = computed(() => store.selectedNodeId === props.node.id)
const canDelete = computed(() => !props.readonly && props.node.type !== 'START' && props.node.type !== 'END')
const showActions = computed(() => !props.readonly && props.node.type !== 'START' && props.node.type !== 'END')

const iconMap: Record<NodeType, any> = {
  START: Play,
  END: Square,
  APPROVAL: UserCheck,
  CC: Send,
  CONDITION: GitBranch,
  CONDITION_GROUP: GitBranch,
  PARALLEL: GitFork,
  DELAY: Clock,
  TRIGGER: Zap,
  JUMP: CornerUpRight,
  SUB_PROCESS: Workflow
}

const nodeIcon = computed(() => iconMap[props.node.type] || Workflow)

const description = computed(() => {
  const c = props.node.config
  switch (props.node.type) {
    case 'START': return '流程开始'
    case 'END': return '流程结束'
    case 'APPROVAL': {
      const modeMap: Record<string, string> = { ANY: '任一审批', ALL: '会签', SEQUENTIAL: '顺序审批' }
      return modeMap[c.approvalMode] || '审批'
    }
    case 'CC': return 抄送 \ 人
    case 'DELAY': return 等待 \ \
    case 'TRIGGER': return c.triggerKey || '未配置'
    case 'JUMP': return c.targetNodeId ? '跳转到指定节点' : '未配置'
    case 'CONDITION_GROUP': return \ 个条件分支
    case 'PARALLEL': return \ 个并行分支
    default: return ''
  }
})

function handleClick() {
  store.selectNode(props.node.id)
}
</script>

<style lang="scss" scoped>
.wf-node-card {
  width: 220px;
  border-radius: 8px;
  border: 2px solid #e4e7ed;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;

  &:hover { border-color: #409eff; box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15); }
  &--selected { border-color: #409eff; box-shadow: 0 2px 12px rgba(64, 158, 255, 0.3); }

  &--start { border-color: #67c23a; .wf-node-card__header { background: #f0f9eb; } }
  &--end { border-color: #909399; .wf-node-card__header { background: #f4f4f5; } }
  &--approval { border-color: #409eff; .wf-node-card__header { background: #ecf5ff; } }
  &--cc { border-color: #e6a23c; .wf-node-card__header { background: #fdf6ec; } }
  &--condition, &--condition_group { border-color: #e6a23c; .wf-node-card__header { background: #fdf6ec; } }
  &--parallel { border-color: #909399; .wf-node-card__header { background: #f4f4f5; } }
  &--delay { border-color: #f56c6c; .wf-node-card__header { background: #fef0f0; } }
  &--trigger { border-color: #9b59b6; .wf-node-card__header { background: #f3e8ff; } }
  &--jump { border-color: #00b894; .wf-node-card__header { background: #e8fff8; } }

  &__header {
    display: flex;
    align-items: center;
    padding: 8px 12px;
    border-radius: 6px 6px 0 0;
    gap: 8px;
  }

  &__icon { display: flex; align-items: center; }
  &__title { flex: 1; font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  &__actions { display: flex; gap: 4px; }

  &__body {
    padding: 8px 12px;
  }

  &__desc {
    font-size: 12px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }
}
</style>
`

- [ ] **Step 2: Create AddNodeMenu component**

Popover menu for selecting which node type to add.

`ue
<!-- src/views/System/SystemModule/Workflow/components/designer/AddNodeMenu.vue -->
<template>
  <n-popover trigger="click" placement="bottom" :show="showMenu" @update:show="showMenu = $vent">
    <template #trigger>
      <div class="wf-add-btn" @click.stop="showMenu = true">
        <n-button circle size="tiny" type="primary">
          <template #icon><Plus :size="12" /></template>
        </n-button>
      </div>
    </template>
    <div class="wf-add-menu">
      <div class="wf-add-menu__title">添加节点</div>
      <div class="wf-add-menu__grid">
        <div
          v-for="item in nodeOptions"
          :key="item.type"
          class="wf-add-menu__item"
          @click="handleSelect(item.type)"
        >
          <div class="wf-add-menu__icon" :style="{ background: item.color }">
            <component :is="item.icon" :size="16" color="#fff" />
          </div>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </div>
  </n-popover>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import type { NodeType } from '@/api/workflow'
import {
  Plus, UserCheck, Send, GitBranch, GitFork,
  Clock, Zap, CornerUpRight, Workflow
} from 'lucide-vue-next'

const emit = defineEmits<{
  select: [type: NodeType]
}>()

const showMenu = ref(false)

const nodeOptions = [
  { type: 'APPROVAL' as NodeType, label: '审批', icon: UserCheck, color: '#409eff' },
  { type: 'CC' as NodeType, label: '抄送', icon: Send, color: '#e6a23c' },
  { type: 'CONDITION_GROUP' as NodeType, label: '条件分支', icon: GitBranch, color: '#e6a23c' },
  { type: 'PARALLEL' as NodeType, label: '并行分支', icon: GitFork, color: '#909399' },
  { type: 'DELAY' as NodeType, label: '延迟', icon: Clock, color: '#f56c6c' },
  { type: 'TRIGGER' as NodeType, label: '触发器', icon: Zap, color: '#9b59b6' },
  { type: 'JUMP' as NodeType, label: '跳转', icon: CornerUpRight, color: '#00b894' },
  { type: 'SUB_PROCESS' as NodeType, label: '子流程', icon: Workflow, color: '#3498db' }
]

function handleSelect(type: NodeType) {
  showMenu.value = false
  emit('select', type)
}
</script>

<style lang="scss" scoped>
.wf-add-btn {
  display: flex;
  justify-content: center;
  padding: 4px 0;
}

.wf-add-menu {
  width: 280px;

  &__title {
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 8px;
    color: #303133;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 8px;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.2s;
    font-size: 12px;
    color: #606266;

    &:hover { background: #f5f7fa; }
  }

  &__icon {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
`

- [ ] **Step 3: Create NodeConnector component**

Vertical line with centered add button between nodes.

`ue
<!-- src/views/System/SystemModule/Workflow/components/designer/NodeConnector.vue -->
<template>
  <div class="wf-connector">
    <div class="wf-connector__line"></div>
    <add-node-menu v-if="!readonly" @select="type => emit('addNode', type)" />
    <div class="wf-connector__line"></div>
  </div>
</template>

<script lang="ts" setup>
import AddNodeMenu from './AddNodeMenu.vue'
import type { NodeType } from '@/api/workflow'

defineProps<{
  readonly?: boolean
}>()

const emit = defineEmits<{
  addNode: [type: NodeType]
}>()
</script>

<style lang="scss" scoped>
.wf-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;

  &__line {
    width: 2px;
    height: 16px;
    background: #cacdd1;
  }
}
</style>
`

- [ ] **Step 4: Create DesignerCanvas component**

The main recursive renderer for the workflow tree.

`ue
<!-- src/views/System/SystemModule/Workflow/components/designer/DesignerCanvas.vue -->
<template>
  <div class="wf-canvas" @click="store.selectNode(null)">
    <div class="wf-canvas__flow">
      <render-node :node="store.tree" :readonly="readonly" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { h, defineComponent } from 'vue'
import type { DesignerTreeNode, NodeType } from '@/api/workflow'
import { useWorkflowStore } from '@/stores/workflow'
import NodeCard from './NodeCard.vue'
import NodeConnector from './NodeConnector.vue'
import ConditionBranch from './ConditionBranch.vue'
import ParallelBranch from './ParallelBranch.vue'

defineProps<{
  readonly?: boolean
}>()

const store = useWorkflowStore()

// Recursive render component
const RenderNode = defineComponent({
  name: 'RenderNode',
  props: {
    node: { type: Object as () => DesignerTreeNode, required: true },
    readonly: { type: Boolean, default: false }
  },
  setup(props) {
    return () => {
      const elements: any[] = []

      // 1. Render the node card
      elements.push(
        h(NodeCard, {
          node: props.node,
          readonly: props.readonly,
          onDelete: (id: string) => store.deleteNode(id)
        })
      )

      // 2. If branches exist (CONDITION_GROUP or PARALLEL)
      if (props.node.branches && props.node.branches.length > 0) {
        const BranchComponent = props.node.type === 'CONDITION_GROUP'
          ? ConditionBranch
          : ParallelBranch

        elements.push(
          h(BranchComponent, {
            node: props.node,
            readonly: props.readonly
          })
        )
      }

      // 3. Connector + next node
      if (props.node.next) {
        elements.push(
          h(NodeConnector, {
            readonly: props.readonly,
            onAddNode: (type: NodeType) => store.addNode(props.node.id, type)
          })
        )
        elements.push(
          h(RenderNode, {
            node: props.node.next,
            readonly: props.readonly
          })
        )
      }

      return h('div', { class: 'wf-canvas__node-wrapper' }, elements)
    }
  }
})
</script>

<style lang="scss" scoped>
.wf-canvas {
  width: 100%;
  min-height: 400px;
  overflow: auto;
  padding: 40px 20px;
  background: #f7f8fa;
  border-radius: 8px;

  &__flow {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: fit-content;
  }
}

:deep(.wf-canvas__node-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
`

- [ ] **Step 5: Commit**

`ash
git add src/views/System/SystemModule/Workflow/components/designer/
git commit -m "feat(workflow): add core designer components

NodeCard, AddNodeMenu, NodeConnector, DesignerCanvas

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 5: Branch Components

Condition and parallel branch rendering with horizontal layout.

**Files:**
- Create: `src/views/System/SystemModule/Workflow/components/designer/ConditionBranch.vue`
- Create: `src/views/System/SystemModule/Workflow/components/designer/ParallelBranch.vue`

- [ ] **Step 1: Create ConditionBranch component**

Renders multiple condition branches horizontally with add-branch button.

`ue
<!-- src/views/System/SystemModule/Workflow/components/designer/ConditionBranch.vue -->
<template>
  <div class="wf-branch wf-branch--condition">
    <!-- Top connector: single line splits into branches -->
    <div class="wf-branch__top-line"></div>

    <div class="wf-branch__container">
      <!-- Add branch button -->
      <div v-if="!readonly" class="wf-branch__add" @click="store.addBranch(node.id)">
        <n-button size="tiny" quaternary type="warning">+ 添加条件</n-button>
      </div>

      <div class="wf-branch__lanes">
        <div
          v-for="(branch, idx) in node.branches"
          :key="branch.id"
          class="wf-branch__lane"
        >
          <!-- Branch header -->
          <div class="wf-branch__header">
            <span class="wf-branch__label">{{ branch.name }}</span>
            <n-button
              v-if="!readonly && node.branches!.length > 2"
              text type="error" size="tiny"
              @click="store.removeBranch(node.id, branch.id)"
            >
              <template #icon><X :size="12" /></template>
            </n-button>
          </div>

          <!-- Branch connector line -->
          <div class="wf-branch__line-down"></div>

          <!-- Branch children -->
          <div class="wf-branch__children">
            <template v-if="branch.children.length > 0">
              <template v-for="(child, ci) in branch.children" :key="child.id">
                <render-branch-node :node="child" :readonly="readonly" />
                <node-connector
                  v-if="ci < branch.children.length - 1"
                  :readonly="readonly"
                  @add-node="type => store.addNode(child.id, type)"
                />
              </template>
            </template>
            <div v-else class="wf-branch__empty">
              <add-node-menu v-if="!readonly" @select="type => addNodeToBranch(branch.id, idx, type)" />
            </div>
          </div>

          <!-- Bottom connector line -->
          <div class="wf-branch__line-down"></div>
        </div>
      </div>
    </div>

    <!-- Bottom connector: branches reconverge -->
    <div class="wf-branch__bottom-line"></div>
  </div>
</template>

<script lang="ts" setup>
import { h, defineComponent } from 'vue'
import type { DesignerTreeNode, NodeType } from '@/api/workflow'
import { useWorkflowStore } from '@/stores/workflow'
import { createNode } from '@/utils/workflow-graph'
import NodeCard from './NodeCard.vue'
import NodeConnector from './NodeConnector.vue'
import AddNodeMenu from './AddNodeMenu.vue'
import { X } from 'lucide-vue-next'

const props = defineProps<{
  node: DesignerTreeNode
  readonly?: boolean
}>()

const store = useWorkflowStore()

function addNodeToBranch(branchId: string, branchIdx: number, type: NodeType) {
  store.pushUndoState?.()
  const branch = props.node.branches?.find(b => b.id === branchId)
  if (branch) {
    const newNode = createNode(type)
    branch.children.push(newNode)
    store.selectNode(newNode.id)
  }
}

// Sub-renderer for branch children (avoids circular import)
const RenderBranchNode = defineComponent({
  name: 'RenderBranchNode',
  props: {
    node: { type: Object as () => DesignerTreeNode, required: true },
    readonly: { type: Boolean, default: false }
  },
  setup(nodeProps) {
    return () => {
      return h(NodeCard, {
        node: nodeProps.node,
        readonly: nodeProps.readonly,
        onDelete: (id: string) => store.deleteNode(id)
      })
    }
  }
})
</script>

<style lang="scss" scoped>
.wf-branch {
  display: flex;
  flex-direction: column;
  align-items: center;

  &__top-line, &__bottom-line {
    width: 2px;
    height: 16px;
    background: #cacdd1;
  }

  &__container {
    position: relative;
    border-top: 2px solid #cacdd1;
    border-bottom: 2px solid #cacdd1;
  }

  &__add {
    position: absolute;
    top: -16px;
    right: 0;
    z-index: 1;
  }

  &__lanes {
    display: flex;
    gap: 0;
  }

  &__lane {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 260px;
    padding: 0 20px;
    position: relative;

    &:not(:last-child) {
      border-right: 2px solid #cacdd1;
    }
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 0;
  }

  &__label {
    font-size: 12px;
    color: #909399;
    font-weight: 500;
  }

  &__line-down {
    width: 2px;
    height: 12px;
    background: #cacdd1;
  }

  &__children {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 40px;
  }

  &__empty {
    padding: 8px 0;
  }
}
</style>
`

- [ ] **Step 2: Create ParallelBranch component**

Same structure as ConditionBranch but with different styling and labels.

`ue
<!-- src/views/System/SystemModule/Workflow/components/designer/ParallelBranch.vue -->
<template>
  <div class="wf-branch wf-branch--parallel">
    <div class="wf-branch__top-line"></div>

    <div class="wf-branch__container">
      <div v-if="!readonly" class="wf-branch__add" @click="store.addBranch(node.id)">
        <n-button size="tiny" quaternary type="info">+ 添加分支</n-button>
      </div>

      <div class="wf-branch__lanes">
        <div
          v-for="(branch, idx) in node.branches"
          :key="branch.id"
          class="wf-branch__lane"
        >
          <div class="wf-branch__header">
            <span class="wf-branch__label">{{ branch.name }}</span>
            <n-button
              v-if="!readonly && node.branches!.length > 2"
              text type="error" size="tiny"
              @click="store.removeBranch(node.id, branch.id)"
            >
              <template #icon><X :size="12" /></template>
            </n-button>
          </div>

          <div class="wf-branch__line-down"></div>

          <div class="wf-branch__children">
            <template v-if="branch.children.length > 0">
              <template v-for="(child, ci) in branch.children" :key="child.id">
                <node-card
                  :node="child"
                  :readonly="readonly"
                  @delete="id => store.deleteNode(id)"
                />
                <node-connector
                  v-if="ci < branch.children.length - 1"
                  :readonly="readonly"
                  @add-node="type => store.addNode(child.id, type)"
                />
              </template>
            </template>
            <div v-else class="wf-branch__empty">
              <add-node-menu v-if="!readonly" @select="type => addNodeToBranch(branch.id, idx, type)" />
            </div>
          </div>

          <div class="wf-branch__line-down"></div>
        </div>
      </div>
    </div>

    <div class="wf-branch__bottom-line"></div>
  </div>
</template>

<script lang="ts" setup>
import type { DesignerTreeNode, NodeType } from '@/api/workflow'
import { useWorkflowStore } from '@/stores/workflow'
import { createNode } from '@/utils/workflow-graph'
import NodeCard from './NodeCard.vue'
import NodeConnector from './NodeConnector.vue'
import AddNodeMenu from './AddNodeMenu.vue'
import { X } from 'lucide-vue-next'

const props = defineProps<{
  node: DesignerTreeNode
  readonly?: boolean
}>()

const store = useWorkflowStore()

function addNodeToBranch(branchId: string, branchIdx: number, type: NodeType) {
  const branch = props.node.branches?.find(b => b.id === branchId)
  if (branch) {
    const newNode = createNode(type)
    branch.children.push(newNode)
    store.selectNode(newNode.id)
  }
}
</script>

<style lang="scss" scoped>
.wf-branch {
  display: flex;
  flex-direction: column;
  align-items: center;

  &__top-line, &__bottom-line {
    width: 2px;
    height: 16px;
    background: #cacdd1;
  }

  &__container {
    position: relative;
    border-top: 2px solid #b0b3b8;
    border-bottom: 2px solid #b0b3b8;
  }

  &__add {
    position: absolute;
    top: -16px;
    right: 0;
    z-index: 1;
  }

  &__lanes {
    display: flex;
  }

  &__lane {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 260px;
    padding: 0 20px;
    position: relative;

    &:not(:last-child) {
      border-right: 2px solid #b0b3b8;
    }
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 0;
  }

  &__label { font-size: 12px; color: #606266; font-weight: 500; }
  &__line-down { width: 2px; height: 12px; background: #b0b3b8; }

  &__children {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 40px;
  }

  &__empty { padding: 8px 0; }
}
</style>
`

- [ ] **Step 3: Commit**

`ash
git add src/views/System/SystemModule/Workflow/components/designer/ConditionBranch.vue \
        src/views/System/SystemModule/Workflow/components/designer/ParallelBranch.vue
git commit -m "feat(workflow): add branch components for conditions and parallel

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 6: Node Configuration Panels + AssigneeSelector

Right-side drawer for editing selected node configuration, plus reusable person/role selector.

**Files:**
- Create: `src/views/System/SystemModule/Workflow/components/common/AssigneeSelector.vue`
- Create: `src/views/System/SystemModule/Workflow/components/config/ApprovalNodeConfig.vue`
- Create: `src/views/System/SystemModule/Workflow/components/config/CcNodeConfig.vue`
- Create: `src/views/System/SystemModule/Workflow/components/config/ConditionNodeConfig.vue`
- Create: `src/views/System/SystemModule/Workflow/components/config/DelayNodeConfig.vue`
- Create: `src/views/System/SystemModule/Workflow/components/config/TriggerNodeConfig.vue`
- Create: `src/views/System/SystemModule/Workflow/components/config/JumpNodeConfig.vue`
- Create: `src/views/System/SystemModule/Workflow/components/config/NodeConfigDrawer.vue`

- [ ] **Step 1: Create AssigneeSelector component**

Reusable component for selecting managers (by person or role).

`ue
<!-- src/views/System/SystemModule/Workflow/components/common/AssigneeSelector.vue -->
<template>
  <div class="assignee-selector">
    <n-form-item label="审批人类型">
      <n-radio-group v-model:value="localType" @update:value="handleTypeChange">
        <n-radio value="MANAGER">指定人员</n-radio>
        <n-radio value="ROLE">指定角色</n-radio>
        <n-radio value="INITIATOR">发起人</n-radio>
        <n-radio value="INITIATOR_DEPT_LEADER">发起人部门主管</n-radio>
      </n-radio-group>
    </n-form-item>

    <n-form-item v-if="localType === 'MANAGER'" label="选择人员">
      <n-select
        v-model:value="localIds"
        multiple
        filterable
        remote
        :options="managerOptions"
        :loading="managerLoading"
        placeholder="搜索并选择人员"
        @search="searchManagers"
        @update:value="handleIdsChange"
      />
    </n-form-item>

    <n-form-item v-if="localType === 'ROLE'" label="选择角色">
      <n-select
        v-model:value="localIds"
        multiple
        filterable
        :options="roleOptions"
        :loading="roleLoading"
        placeholder="选择角色"
        @update:value="handleIdsChange"
      />
    </n-form-item>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, watch } from 'vue'
import type { SelectOption } from 'naive-ui'
import { http } from '@/plugins/axios'
import type { RetResult } from '@/api/types'

const props = defineProps<{
  assigneeType: string
  assigneeIds: string[]
}>()

const emit = defineEmits<{
  'update:assigneeType': [value: string]
  'update:assigneeIds': [value: string[]]
}>()

const localType = ref(props.assigneeType || 'MANAGER')
const localIds = ref<string[]>(props.assigneeIds || [])

watch(() => props.assigneeType, v => { localType.value = v })
watch(() => props.assigneeIds, v => { localIds.value = v || [] })

// Manager search
const managerOptions = ref<SelectOption[]>([])
const managerLoading = ref(false)

async function searchManagers(query: string) {
  if (!query || query.length < 1) return
  managerLoading.value = true
  try {
    const { data: res } = await http.get<RetResult<{ items: any[]; total: number }>>('system/manager', {
      params: { keyword: query, pageIndex: 1, pageSize: 20 }
    })
    managerOptions.value = res.data.items.map((m: any) => ({
      label: m.realName || m.managerName,
      value: m.id
    }))
  } finally {
    managerLoading.value = false
  }
}

// Role options
const roleOptions = ref<SelectOption[]>([])
const roleLoading = ref(false)

async function loadRoles() {
  roleLoading.value = true
  try {
    const { data: res } = await http.get<RetResult<{ items: any[]; total: number }>>('system/role', {
      params: { pageIndex: 1, pageSize: 100 }
    })
    roleOptions.value = res.data.items.map((r: any) => ({
      label: r.name,
      value: r.id
    }))
  } finally {
    roleLoading.value = false
  }
}

onMounted(loadRoles)

function handleTypeChange(value: string) {
  localIds.value = []
  emit('update:assigneeType', value)
  emit('update:assigneeIds', [])
}

function handleIdsChange(value: string[]) {
  emit('update:assigneeIds', value)
}
</script>

<style lang="scss" scoped>
.assignee-selector {
  width: 100%;
}
</style>
`

- [ ] **Step 2: Create ApprovalNodeConfig**

`ue
<!-- src/views/System/SystemModule/Workflow/components/config/ApprovalNodeConfig.vue -->
<template>
  <n-form label-placement="left" label-width="auto">
    <n-form-item label="节点名称">
      <n-input v-model:value="config.name" @update:value="emitUpdate" />
    </n-form-item>

    <n-form-item label="审批模式">
      <n-radio-group v-model:value="config.approvalMode" @update:value="emitUpdate">
        <n-radio-button value="ANY">任一通过</n-radio-button>
        <n-radio-button value="ALL">会签(全部)</n-radio-button>
        <n-radio-button value="SEQUENTIAL">顺序审批</n-radio-button>
      </n-radio-group>
    </n-form-item>

    <assignee-selector
      :assignee-type="config.assigneeType"
      :assignee-ids="config.assigneeIds"
      @update:assignee-type="v => { config.assigneeType = v; emitUpdate() }"
      @update:assignee-ids="v => { config.assigneeIds = v; emitUpdate() }"
    />

    <n-divider />

    <n-form-item label="允许转办">
      <n-switch v-model:value="config.allowDelegate" @update:value="emitUpdate" />
    </n-form-item>
    <n-form-item label="允许回退">
      <n-switch v-model:value="config.allowReturn" @update:value="emitUpdate" />
    </n-form-item>
    <n-form-item label="允许加签">
      <n-switch v-model:value="config.allowCountersign" @update:value="emitUpdate" />
    </n-form-item>
    <n-form-item label="允许撕回">
      <n-switch v-model:value="config.allowWithdraw" @update:value="emitUpdate" />
    </n-form-item>

    <n-divider />

    <n-form-item label="超时(小时)">
      <n-input-number v-model:value="config.timeoutHours" :min="0" :max="720" @update:value="emitUpdate" />
    </n-form-item>
  </n-form>
</template>

<script lang="ts" setup>
import { reactive, watch } from 'vue'
import type { DesignerTreeNode } from '@/api/workflow'
import AssigneeSelector from '../common/AssigneeSelector.vue'

const props = defineProps<{ node: DesignerTreeNode }>()
const emit = defineEmits<{ update: [config: Record<string, any>; name: string] }>()

const config = reactive({
  name: props.node.name,
  approvalMode: props.node.config.approvalMode || 'ANY',
  assigneeType: props.node.config.assigneeType || 'MANAGER',
  assigneeIds: props.node.config.assigneeIds || [],
  allowDelegate: props.node.config.allowDelegate ?? true,
  allowReturn: props.node.config.allowReturn ?? true,
  allowCountersign: props.node.config.allowCountersign ?? true,
  allowWithdraw: props.node.config.allowWithdraw ?? true,
  timeoutHours: props.node.config.timeoutHours ?? 0
})

watch(() => props.node.id, () => {
  Object.assign(config, {
    name: props.node.name,
    approvalMode: props.node.config.approvalMode || 'ANY',
    assigneeType: props.node.config.assigneeType || 'MANAGER',
    assigneeIds: props.node.config.assigneeIds || [],
    allowDelegate: props.node.config.allowDelegate ?? true,
    allowReturn: props.node.config.allowReturn ?? true,
    allowCountersign: props.node.config.allowCountersign ?? true,
    allowWithdraw: props.node.config.allowWithdraw ?? true,
    timeoutHours: props.node.config.timeoutHours ?? 0
  })
})

function emitUpdate() {
  const { name, ...rest } = config
  emit('update', rest, name)
}
</script>
`

- [ ] **Step 3: Create CcNodeConfig**

`ue
<!-- src/views/System/SystemModule/Workflow/components/config/CcNodeConfig.vue -->
<template>
  <n-form label-placement="left" label-width="auto">
    <n-form-item label="节点名称">
      <n-input v-model:value="config.name" @update:value="emitUpdate" />
    </n-form-item>

    <assignee-selector
      :assignee-type="config.assigneeType"
      :assignee-ids="config.assigneeIds"
      @update:assignee-type="v => { config.assigneeType = v; emitUpdate() }"
      @update:assignee-ids="v => { config.assigneeIds = v; emitUpdate() }"
    />
  </n-form>
</template>

<script lang="ts" setup>
import { reactive, watch } from 'vue'
import type { DesignerTreeNode } from '@/api/workflow'
import AssigneeSelector from '../common/AssigneeSelector.vue'

const props = defineProps<{ node: DesignerTreeNode }>()
const emit = defineEmits<{ update: [config: Record<string, any>; name: string] }>()

const config = reactive({
  name: props.node.name,
  assigneeType: props.node.config.assigneeType || 'MANAGER',
  assigneeIds: props.node.config.assigneeIds || []
})

watch(() => props.node.id, () => {
  Object.assign(config, {
    name: props.node.name,
    assigneeType: props.node.config.assigneeType || 'MANAGER',
    assigneeIds: props.node.config.assigneeIds || []
  })
})

function emitUpdate() {
  const { name, ...rest } = config
  emit('update', rest, name)
}
</script>
`

- [ ] **Step 4: Create ConditionNodeConfig**

`ue
<!-- src/views/System/SystemModule/Workflow/components/config/ConditionNodeConfig.vue -->
<template>
  <n-form label-placement="left" label-width="auto">
    <n-form-item label="节点名称">
      <n-input v-model:value="localName" @update:value="emitUpdate" />
    </n-form-item>

    <n-form-item label="逻辑关系">
      <n-radio-group v-model:value="logicOperator" @update:value="emitUpdate">
        <n-radio-button value="AND">全部满足 (AND)</n-radio-button>
        <n-radio-button value="OR">任一满足 (OR)</n-radio-button>
      </n-radio-group>
    </n-form-item>

    <n-divider>条件规则</n-divider>

    <div v-for="(rule, idx) in rules" :key="idx" class="condition-rule">
      <n-grid :cols="12" :x-gap="8">
        <n-gi :span="4">
          <n-input v-model:value="rule.field" placeholder="字段" @update:value="emitUpdate" />
        </n-gi>
        <n-gi :span="3">
          <n-select
            v-model:value="rule.operator"
            :options="operatorOptions"
            placeholder="运算符"
            @update:value="emitUpdate"
          />
        </n-gi>
        <n-gi :span="4">
          <n-input v-model:value="rule.value" placeholder="值" @update:value="emitUpdate" />
        </n-gi>
        <n-gi :span="1">
          <n-button text type="error" @click="removeRule(idx)">
            <template #icon><Trash2 :size="14" /></template>
          </n-button>
        </n-gi>
      </n-grid>
    </div>

    <n-button size="small" dashed block @click="addRule">+ 添加条件</n-button>
  </n-form>
</template>

<script lang="ts" setup>
import { ref, reactive, watch } from 'vue'
import type { DesignerTreeNode } from '@/api/workflow'
import { Trash2 } from 'lucide-vue-next'

const props = defineProps<{ node: DesignerTreeNode }>()
const emit = defineEmits<{ update: [config: Record<string, any>; name: string] }>()

const localName = ref(props.node.name)
const logicOperator = ref(props.node.config.logicOperator || 'AND')
const rules = reactive<Array<{ field: string; operator: string; value: string }>>(
  props.node.config.rules || []
)

const operatorOptions = [
  { label: '等于', value: 'EQ' },
  { label: '不等于', value: 'NEQ' },
  { label: '大于', value: 'GT' },
  { label: '大于等于', value: 'GTE' },
  { label: '小于', value: 'LT' },
  { label: '小于等于', value: 'LTE' },
  { label: '包含', value: 'CONTAINS' },
  { label: '不包含', value: 'NOT_CONTAINS' }
]

watch(() => props.node.id, () => {
  localName.value = props.node.name
  logicOperator.value = props.node.config.logicOperator || 'AND'
  rules.splice(0, rules.length, ...(props.node.config.rules || []))
})

function addRule() {
  rules.push({ field: '', operator: 'EQ', value: '' })
  emitUpdate()
}

function removeRule(idx: number) {
  rules.splice(idx, 1)
  emitUpdate()
}

function emitUpdate() {
  emit('update', { logicOperator: logicOperator.value, rules: [...rules] }, localName.value)
}
</script>

<style lang="scss" scoped>
.condition-rule {
  margin-bottom: 8px;
}
</style>
`

- [ ] **Step 5: Create DelayNodeConfig**

`ue
<!-- src/views/System/SystemModule/Workflow/components/config/DelayNodeConfig.vue -->
<template>
  <n-form label-placement="left" label-width="auto">
    <n-form-item label="节点名称">
      <n-input v-model:value="config.name" @update:value="emitUpdate" />
    </n-form-item>
    <n-form-item label="延迟类型">
      <n-radio-group v-model:value="config.delayType" @update:value="emitUpdate">
        <n-radio-button value="FIXED">固定时长</n-radio-button>
        <n-radio-button value="UNTIL">指定时间</n-radio-button>
      </n-radio-group>
    </n-form-item>
    <n-form-item v-if="config.delayType === 'FIXED'" label="延迟时长">
      <n-input-group>
        <n-input-number v-model:value="config.delayValue" :min="1" style="width: 60%" @update:value="emitUpdate" />
        <n-select
          v-model:value="config.delayUnit"
          :options="unitOptions"
          style="width: 40%"
          @update:value="emitUpdate"
        />
      </n-input-group>
    </n-form-item>
  </n-form>
</template>

<script lang="ts" setup>
import { reactive, watch } from 'vue'
import type { DesignerTreeNode } from '@/api/workflow'

const props = defineProps<{ node: DesignerTreeNode }>()
const emit = defineEmits<{ update: [config: Record<string, any>; name: string] }>()

const unitOptions = [
  { label: '分钟', value: 'MINUTES' },
  { label: '小时', value: 'HOURS' },
  { label: '天', value: 'DAYS' }
]

const config = reactive({
  name: props.node.name,
  delayType: props.node.config.delayType || 'FIXED',
  delayValue: props.node.config.delayValue ?? 1,
  delayUnit: props.node.config.delayUnit || 'HOURS'
})

watch(() => props.node.id, () => {
  Object.assign(config, {
    name: props.node.name,
    delayType: props.node.config.delayType || 'FIXED',
    delayValue: props.node.config.delayValue ?? 1,
    delayUnit: props.node.config.delayUnit || 'HOURS'
  })
})

function emitUpdate() {
  const { name, ...rest } = config
  emit('update', rest, name)
}
</script>
`

- [ ] **Step 6: Create TriggerNodeConfig**

`ue
<!-- src/views/System/SystemModule/Workflow/components/config/TriggerNodeConfig.vue -->
<template>
  <n-form label-placement="left" label-width="auto">
    <n-form-item label="节点名称">
      <n-input v-model:value="config.name" @update:value="emitUpdate" />
    </n-form-item>
    <n-form-item label="触发器">
      <n-select
        v-model:value="config.triggerKey"
        :options="triggerOptions"
        :loading="loading"
        placeholder="选择触发器"
        filterable
        @update:value="emitUpdate"
      />
    </n-form-item>
    <n-form-item label="参数 (JSON)">
      <n-input
        v-model:value="paramsJson"
        type="textarea"
        :rows="4"
        placeholder='{"key": "value"}'
        @update:value="handleParamsChange"
      />
    </n-form-item>
  </n-form>
</template>

<script lang="ts" setup>
import { ref, reactive, watch, onMounted } from 'vue'
import type { DesignerTreeNode, TriggerItem } from '@/api/workflow'
import { workflowApi } from '@/api/workflow'

const props = defineProps<{ node: DesignerTreeNode }>()
const emit = defineEmits<{ update: [config: Record<string, any>; name: string] }>()

const loading = ref(false)
const triggerOptions = ref<Array<{ label: string; value: string }>>([])

const config = reactive({
  name: props.node.name,
  triggerKey: props.node.config.triggerKey || ''
})

const paramsJson = ref(JSON.stringify(props.node.config.triggerParams || {}, null, 2))

onMounted(async () => {
  loading.value = true
  try {
    const { data: res } = await workflowApi.triggerList()
    triggerOptions.value = res.data.items.map((t: TriggerItem) => ({
      label: \ (\),
      value: t.key
    }))
  } finally {
    loading.value = false
  }
})

watch(() => props.node.id, () => {
  config.name = props.node.name
  config.triggerKey = props.node.config.triggerKey || ''
  paramsJson.value = JSON.stringify(props.node.config.triggerParams || {}, null, 2)
})

function handleParamsChange(value: string) {
  paramsJson.value = value
  emitUpdate()
}

function emitUpdate() {
  let triggerParams = {}
  try { triggerParams = JSON.parse(paramsJson.value) } catch {}
  emit('update', { triggerKey: config.triggerKey, triggerParams }, config.name)
}
</script>
`

- [ ] **Step 7: Create JumpNodeConfig**

`ue
<!-- src/views/System/SystemModule/Workflow/components/config/JumpNodeConfig.vue -->
<template>
  <n-form label-placement="left" label-width="auto">
    <n-form-item label="节点名称">
      <n-input v-model:value="config.name" @update:value="emitUpdate" />
    </n-form-item>
    <n-form-item label="跳转目标">
      <n-select
        v-model:value="config.targetNodeId"
        :options="nodeOptions"
        placeholder="选择目标节点"
        filterable
        @update:value="emitUpdate"
      />
    </n-form-item>
  </n-form>
</template>

<script lang="ts" setup>
import { ref, reactive, watch, computed } from 'vue'
import type { DesignerTreeNode } from '@/api/workflow'
import { useWorkflowStore } from '@/stores/workflow'
import { findNode } from '@/utils/workflow-graph'

const props = defineProps<{ node: DesignerTreeNode }>()
const emit = defineEmits<{ update: [config: Record<string, any>; name: string] }>()

const store = useWorkflowStore()

const config = reactive({
  name: props.node.name,
  targetNodeId: props.node.config.targetNodeId || ''
})

// Collect all node IDs from tree for selection (exclude self, START, END)
const nodeOptions = computed(() => {
  const options: Array<{ label: string; value: string }> = []
  collectNodes(store.tree, options)
  return options.filter(o => o.value !== props.node.id)
})

function collectNodes(node: DesignerTreeNode, acc: Array<{ label: string; value: string }>) {
  if (node.type !== 'START' && node.type !== 'END') {
    acc.push({ label: \ (\), value: node.id })
  }
  if (node.branches) {
    for (const branch of node.branches) {
      for (const child of branch.children) {
        collectNodes(child, acc)
      }
    }
  }
  if (node.next) {
    collectNodes(node.next, acc)
  }
}

watch(() => props.node.id, () => {
  config.name = props.node.name
  config.targetNodeId = props.node.config.targetNodeId || ''
})

function emitUpdate() {
  emit('update', { targetNodeId: config.targetNodeId }, config.name)
}
</script>
`

- [ ] **Step 8: Create NodeConfigDrawer**

Container drawer that dynamically renders the correct config panel based on node type.

`ue
<!-- src/views/System/SystemModule/Workflow/components/config/NodeConfigDrawer.vue -->
<template>
  <n-drawer :show="!!store.selectedNode" :width="400" placement="right" @update:show="handleClose">
    <n-drawer-content :title="drawerTitle" closable>
      <template v-if="store.selectedNode">
        <approval-node-config
          v-if="store.selectedNode.type === 'APPROVAL'"
          :node="store.selectedNode"
          @update="handleUpdate"
        />
        <cc-node-config
          v-else-if="store.selectedNode.type === 'CC'"
          :node="store.selectedNode"
          @update="handleUpdate"
        />
        <condition-node-config
          v-else-if="store.selectedNode.type === 'CONDITION'"
          :node="store.selectedNode"
          @update="handleUpdate"
        />
        <delay-node-config
          v-else-if="store.selectedNode.type === 'DELAY'"
          :node="store.selectedNode"
          @update="handleUpdate"
        />
        <trigger-node-config
          v-else-if="store.selectedNode.type === 'TRIGGER'"
          :node="store.selectedNode"
          @update="handleUpdate"
        />
        <jump-node-config
          v-else-if="store.selectedNode.type === 'JUMP'"
          :node="store.selectedNode"
          @update="handleUpdate"
        />
        <div v-else class="config-placeholder">
          <n-empty description="该节点类型无需配置" />
        </div>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useWorkflowStore } from '@/stores/workflow'
import ApprovalNodeConfig from './ApprovalNodeConfig.vue'
import CcNodeConfig from './CcNodeConfig.vue'
import ConditionNodeConfig from './ConditionNodeConfig.vue'
import DelayNodeConfig from './DelayNodeConfig.vue'
import TriggerNodeConfig from './TriggerNodeConfig.vue'
import JumpNodeConfig from './JumpNodeConfig.vue'

const store = useWorkflowStore()

const drawerTitle = computed(() => {
  if (!store.selectedNode) return ''
  return 配置: \
})

function handleUpdate(config: Record<string, any>, name: string) {
  if (!store.selectedNode) return
  store.updateNodeConfig(store.selectedNode.id, config)
  if (name !== store.selectedNode.name) {
    store.updateNodeName(store.selectedNode.id, name)
  }
}

function handleClose(show: boolean) {
  if (!show) {
    store.selectNode(null)
  }
}
</script>

<style lang="scss" scoped>
.config-placeholder {
  padding: 40px 0;
}
</style>
`

- [ ] **Step 9: Commit**

`ash
git add src/views/System/SystemModule/Workflow/components/common/AssigneeSelector.vue \
        src/views/System/SystemModule/Workflow/components/config/
git commit -m "feat(workflow): add node config panels and AssigneeSelector

Approval, CC, Condition, Delay, Trigger, Jump configs + NodeConfigDrawer

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 7: Designer Page + Definition List Page

The two primary management pages.

**Files:**
- Create: `src/views/System/SystemModule/Workflow/WorkflowDesigner.vue`
- Create: `src/views/System/SystemModule/Workflow/WorkflowDefinitionIndex.vue`

- [ ] **Step 1: Create WorkflowDesigner page**

`ue
<!-- src/views/System/SystemModule/Workflow/WorkflowDesigner.vue -->
<template>
  <div class="wf-designer">
    <!-- Toolbar -->
    <div class="wf-designer__toolbar">
      <div class="wf-designer__toolbar-left">
        <n-button quaternary @click="router.back()">
          <template #icon><ArrowLeft :size="16" /></template>
          返回
        </n-button>
        <n-divider vertical />
        <span class="wf-designer__title">{{ store.currentDefinition?.name || '流程设计器' }}</span>
        <n-tag v-if="store.isDirty" size="small" type="warning">未保存</n-tag>
      </div>
      <div class="wf-designer__toolbar-right">
        <n-button quaternary :disabled="!store.canUndo" @click="store.undo()">
          <template #icon><Undo :size="16" /></template>
          撤销
        </n-button>
        <n-button quaternary :disabled="!store.canRedo" @click="store.redo()">
          <template #icon><Redo :size="16" /></template>
          重做
        </n-button>
        <n-divider vertical />
        <n-button type="primary" ghost @click="handleSave">
          <template #icon><Save :size="16" /></template>
          保存
        </n-button>
        <n-button type="primary" @click="handlePublish">
          <template #icon><Upload :size="16" /></template>
          发布
        </n-button>
      </div>
    </div>

    <!-- Main content -->
    <div class="wf-designer__content">
      <designer-canvas :readonly="false" />
      <node-config-drawer />
    </div>

    <!-- Save dialog -->
    <n-modal v-model:show="showSaveDialog" preset="dialog" title="保存版本">
      <n-form-item label="变更说明">
        <n-input v-model:value="changeLog" type="textarea" :rows="3" placeholder="请输入变更说明（可选）" />
      </n-form-item>
      <template #action>
        <n-button @click="showSaveDialog = false">取消</n-button>
        <n-button type="primary" :loading="saving" @click="confirmSave">确定</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflow'
import { useMessage, useDialog } from 'naive-ui'
import DesignerCanvas from './components/designer/DesignerCanvas.vue'
import NodeConfigDrawer from './components/config/NodeConfigDrawer.vue'
import { ArrowLeft, Undo, Redo, Save, Upload } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const store = useWorkflowStore()
const message = useMessage()
const dialog = useDialog()

const showSaveDialog = ref(false)
const changeLog = ref('')
const saving = ref(false)

onMounted(async () => {
  const definitionId = route.params.definitionId as string
  if (definitionId) {
    await store.loadDefinition(definitionId)
  }
})

onBeforeUnmount(() => {
  store.resetDesigner()
})

function handleSave() {
  showSaveDialog.value = true
  changeLog.value = ''
}

async function confirmSave() {
  saving.value = true
  try {
    await store.saveVersion(changeLog.value || undefined)
    message.success('保存成功')
    showSaveDialog.value = false
  } catch (e: any) {
    message.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  if (store.isDirty) {
    message.warning('请先保存当前更改')
    return
  }
  dialog.warning({
    title: '确认发布',
    content: '发布后新发起的流程将使用此版本，是否继续？',
    positiveText: '发布',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await store.publishVersion()
        message.success('发布成功')
      } catch (e: any) {
        message.error('发布失败: ' + (e.message || '未知错误'))
      }
    }
  })
}

// Warn before leaving with unsaved changes
onBeforeUnmount(() => {
  // Router guard handled via beforeRouteLeave if needed
})
</script>

<style lang="scss" scoped>
.wf-designer {
  display: flex;
  flex-direction: column;
  height: 100%;

  &__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 16px;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
    flex-shrink: 0;

    &-left, &-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
  }

  &__content {
    flex: 1;
    overflow: auto;
    display: flex;
    justify-content: center;
    padding: 20px;
    background: #f7f8fa;
  }
}
</style>
`

- [ ] **Step 2: Create WorkflowDefinitionIndex page**

`ue
<!-- src/views/System/SystemModule/Workflow/WorkflowDefinitionIndex.vue -->
<template>
  <div>
    <strix-block cleanable @clear="clearSearch">
      <template #body>
        <n-grid :cols="6" :x-gap="20" :y-gap="10">
          <n-gi span="6 s:3 m:2">
            <n-input-group>
              <n-input v-model:value="listParams.keyword" clearable placeholder="搜索流程名称..." />
              <n-button ghost type="primary" @click="getDataList">搜索</n-button>
            </n-input-group>
          </n-gi>
          <n-gi :span="1">
            <n-button type="primary" @click="showAdd()">创建流程</n-button>
          </n-gi>
        </n-grid>
      </template>
    </strix-block>

    <n-data-table
      :columns="columns"
      :data="dataRef"
      :loading="dataLoading"
      :pagination="pagination"
      :row-key="(row: any) => row.id"
      table-layout="fixed"
    />

    <!-- Create definition modal -->
    <n-modal
      :show="addModal"
      title="创建流程定义"
      class="strix-form-modal"
      preset="card"
      @update:show="tryCloseAdd"
      size="huge"
      @after-leave="resetForms"
    >
      <n-form ref="addFormRef" :model="addForm" :rules="formRules" label-placement="left" label-width="auto" require-mark-placement="right-hanging">
        <n-form-item label="流程名称" path="name">
          <n-input v-model:value="addForm.name" clearable placeholder="请输入流程名称" />
        </n-form-item>
        <n-form-item label="流程标识" path="key">
          <n-input v-model:value="addForm.key" clearable placeholder="英文标识，如 leave-approval" />
        </n-form-item>
        <n-form-item label="分类" path="category">
          <n-input v-model:value="addForm.category" clearable placeholder="可选，如 人事、行政" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="addForm.description" type="textarea" :rows="3" clearable placeholder="流程描述" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-flex justify="end">
          <n-button @click="tryCloseAdd">取消</n-button>
          <n-button type="primary" @click="submitAdd">确定</n-button>
        </n-flex>
      </template>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StrixBlock from '@/components/common/StrixBlock.vue'
import StrixTag from '@/components/common/StrixTag.vue'
import { workflowApi } from '@/api/workflow'
import type { WfDefinition } from '@/api/workflow'
import { useCrud } from '@/composables/useCrud'
import { handleOperate } from '@/utils/strix-table-tool'
import { textField } from '@/utils/form-rules'
import { type DataTableColumns, type FormRules, useMessage, useDialog, NTag } from 'naive-ui'

const _baseName = '流程定义'
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const {
  listParams, clearSearch, pagination,
  addModal, addForm, addFormRef,
  showAdd, submitAdd, tryCloseAdd, resetForms
} = useCrud({
  list: { keyword: null, pageIndex: 1, pageSize: 10 },
  fetchList: () => getDataList(),
  addForm: { name: null, key: null, category: null, description: null },
  editForm: {},
  api: {
    list: (params: Record<string, any>) => workflowApi.definitionList(params),
    create: (data: any) => workflowApi.definitionCreate(data),
    remove: (id: string) => workflowApi.definitionRemove(id)
  },
  draftKey: 'WorkflowDefinition'
})

const formRules: FormRules = {
  name: textField('流程名称', { min: 2, max: 64 }),
  key: textField('流程标识', { min: 2, max: 64 })
}

const columns: DataTableColumns = [
  { key: 'name', title: '流程名称', width: 200 },
  { key: 'key', title: '流程标识', width: 180 },
  { key: 'category', title: '分类', width: 120 },
  {
    key: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    render(row: any) {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'default',
        size: 'small'
      }, () => row.status === 1 ? '已启用' : '已停用')
    }
  },
  { key: 'description', title: '描述', width: 200, ellipsis: { tooltip: true } },
  { key: 'createdTime', title: '创建时间', width: 180 },
  {
    key: 'actions',
    title: '操作',
    width: 280,
    align: 'center',
    render(row: any) {
      return handleOperate([
        {
          type: 'primary',
          label: '设计',
          icon: 'pencil-ruler',
          onClick: () => router.push({ name: 'WorkflowDesigner', params: { definitionId: row.id } })
        },
        {
          type: row.status === 1 ? 'warning' : 'success',
          label: row.status === 1 ? '停用' : '启用',
          icon: row.status === 1 ? 'pause' : 'play',
          onClick: () => toggleStatus(row)
        },
        {
          type: 'error',
          label: '删除',
          icon: 'trash',
          onClick: () => deleteDefinition(row.id),
          popconfirm: true
        }
      ])
    }
  }
]

const dataRef = ref<WfDefinition[]>([])
const dataLoading = ref(true)

async function getDataList() {
  dataLoading.value = true
  try {
    const { data: res } = await workflowApi.definitionList(listParams.value)
    dataRef.value = res.data.items
    pagination.itemCount = res.data.total
  } finally {
    dataLoading.value = false
  }
}

async function toggleStatus(row: WfDefinition) {
  try {
    if (row.status === 1) {
      await workflowApi.definitionDisable(row.id)
      message.success('已停用')
    } else {
      await workflowApi.definitionEnable(row.id)
      message.success('已启用')
    }
    getDataList()
  } catch {}
}

async function deleteDefinition(id: string) {
  try {
    await workflowApi.definitionRemove(id)
    message.success('删除成功')
    getDataList()
  } catch {}
}

onMounted(getDataList)
</script>
`

- [ ] **Step 3: Commit**

`ash
git add src/views/System/SystemModule/Workflow/WorkflowDesigner.vue \
        src/views/System/SystemModule/Workflow/WorkflowDefinitionIndex.vue
git commit -m "feat(workflow): add designer and definition list pages

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 8: Task Workbench Pages + TaskActionDialog

Three task list pages (mine/done/initiated) and the action dialog for task operations.

**Files:**
- Create: `src/views/System/SystemModule/Workflow/components/common/TaskActionDialog.vue`
- Create: `src/views/System/SystemModule/Workflow/WorkflowTaskMine.vue`
- Create: `src/views/System/SystemModule/Workflow/WorkflowTaskDone.vue`
- Create: `src/views/System/SystemModule/Workflow/WorkflowTaskInitiated.vue`

- [ ] **Step 1: Create TaskActionDialog**

Modal for performing task operations (approve, reject, return, delegate, etc.).

`ue
<!-- src/views/System/SystemModule/Workflow/components/common/TaskActionDialog.vue -->
<template>
  <n-modal :show="modelValue" preset="card" :title="dialogTitle" size="medium" @update:show="emit('update:modelValue', false)">
    <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="auto">
      <!-- Comment (all actions) -->
      <n-form-item label="意见" path="comment">
        <n-input v-model:value="form.comment" type="textarea" :rows="3" placeholder="请输入处理意见" />
      </n-form-item>

      <!-- Reject reason -->
      <n-form-item v-if="action === 'reject'" label="拒绝原因" path="reason">
        <n-input v-model:value="form.reason" placeholder="请输入拒绝原因" />
      </n-form-item>

      <!-- Return target -->
      <n-form-item v-if="action === 'return'" label="回退至" path="targetNodeId">
        <n-select v-model:value="form.targetNodeId" :options="returnNodeOptions" placeholder="选择回退节点" />
      </n-form-item>

      <!-- Delegate person -->
      <n-form-item v-if="action === 'delegate'" label="转办人" path="delegateId">
        <n-select
          v-model:value="form.delegateId"
          filterable remote
          :options="managerOptions"
          :loading="managerLoading"
          placeholder="搜索人员"
          @search="searchManagers"
        />
      </n-form-item>

      <!-- Countersign persons -->
      <n-form-item v-if="action === 'countersign'" label="加签人" path="assigneeIds">
        <n-select
          v-model:value="form.assigneeIds"
          multiple filterable remote
          :options="managerOptions"
          :loading="managerLoading"
          placeholder="搜索人员"
          @search="searchManagers"
        />
      </n-form-item>

      <!-- Remove sign person -->
      <n-form-item v-if="action === 'remove-sign'" label="降签人" path="assigneeId">
        <n-select
          v-model:value="form.assigneeId"
          filterable
          :options="currentAssigneeOptions"
          placeholder="选择降签人"
        />
      </n-form-item>
    </n-form>

    <template #footer>
      <n-flex justify="end">
        <n-button @click="emit('update:modelValue', false)">取消</n-button>
        <n-button :type="actionType" :loading="loading" @click="handleSubmit">{{ dialogTitle }}</n-button>
      </n-flex>
    </template>
  </n-modal>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, watch } from 'vue'
import type { FormRules } from 'naive-ui'
import { workflowApi } from '@/api/workflow'
import { http } from '@/plugins/axios'
import type { RetResult } from '@/api/types'
import { textField } from '@/utils/form-rules'

type ActionType = 'approve' | 'reject' | 'return' | 'delegate' | 'countersign' | 'remove-sign'

const props = defineProps<{
  modelValue: boolean
  taskId: string
  action: ActionType
  returnNodes?: Array<{ label: string; value: string }>
  currentAssignees?: Array<{ label: string; value: string }>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref()
const loading = ref(false)
const managerOptions = ref<Array<{ label: string; value: string }>>([])
const managerLoading = ref(false)

const form = reactive({
  comment: '',
  reason: '',
  targetNodeId: '',
  delegateId: '',
  assigneeIds: [] as string[],
  assigneeId: ''
})

const titleMap: Record<ActionType, string> = {
  approve: '通过', reject: '拒绝', return: '回退',
  delegate: '转办', countersign: '加签', 'remove-sign': '降签'
}

const typeMap: Record<ActionType, string> = {
  approve: 'success', reject: 'error', return: 'warning',
  delegate: 'info', countersign: 'info', 'remove-sign': 'warning'
}

const dialogTitle = computed(() => titleMap[props.action] || '操作')
const actionType = computed(() => typeMap[props.action] || 'primary')
const returnNodeOptions = computed(() => props.returnNodes || [])
const currentAssigneeOptions = computed(() => props.currentAssignees || [])

const formRules = computed<FormRules>(() => {
  const rules: FormRules = {}
  if (props.action === 'reject') {
    rules.reason = textField('拒绝原因')
  }
  if (props.action === 'return') {
    rules.targetNodeId = { required: true, message: '请选择回退节点', trigger: 'change' }
  }
  if (props.action === 'delegate') {
    rules.delegateId = { required: true, message: '请选择转办人', trigger: 'change' }
  }
  if (props.action === 'countersign') {
    rules.assigneeIds = { type: 'array', required: true, min: 1, message: '请选择加签人', trigger: 'change' }
  }
  return rules
})

watch(() => props.modelValue, (val) => {
  if (val) {
    Object.assign(form, { comment: '', reason: '', targetNodeId: '', delegateId: '', assigneeIds: [], assigneeId: '' })
  }
})

async function searchManagers(query: string) {
  if (!query) return
  managerLoading.value = true
  try {
    const { data: res } = await http.get<RetResult<{ items: any[] }>>('system/manager', {
      params: { keyword: query, pageIndex: 1, pageSize: 20 }
    })
    managerOptions.value = res.data.items.map((m: any) => ({
      label: m.realName || m.managerName,
      value: m.id
    }))
  } finally {
    managerLoading.value = false
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch { return }

  loading.value = true
  try {
    const apiMap: Record<ActionType, () => Promise<any>> = {
      approve: () => workflowApi.taskApprove(props.taskId, { comment: form.comment }),
      reject: () => workflowApi.taskReject(props.taskId, { comment: form.comment, reason: form.reason }),
      return: () => workflowApi.taskReturn(props.taskId, { targetNodeId: form.targetNodeId, comment: form.comment }),
      delegate: () => workflowApi.taskDelegate(props.taskId, { delegateId: form.delegateId, comment: form.comment }),
      countersign: () => workflowApi.taskCountersign(props.taskId, { assigneeIds: form.assigneeIds, comment: form.comment }),
      'remove-sign': () => workflowApi.taskRemoveSign(props.taskId, { assigneeId: form.assigneeId, comment: form.comment })
    }
    await apiMap[props.action]()
    emit('success')
    emit('update:modelValue', false)
  } finally {
    loading.value = false
  }
}
</script>
`

- [ ] **Step 2: Create WorkflowTaskMine page**

`ue
<!-- src/views/System/SystemModule/Workflow/WorkflowTaskMine.vue -->
<template>
  <div>
    <strix-block cleanable @clear="clearSearch">
      <template #body>
        <n-grid :cols="6" :x-gap="20" :y-gap="10">
          <n-gi span="6 s:3 m:2">
            <n-input-group>
              <n-input v-model:value="listParams.keyword" clearable placeholder="搜索流程名称..." />
              <n-button ghost type="primary" @click="getDataList">搜索</n-button>
            </n-input-group>
          </n-gi>
        </n-grid>
      </template>
    </strix-block>

    <n-data-table
      :columns="columns"
      :data="dataRef"
      :loading="dataLoading"
      :pagination="pagination"
      :row-key="(row: any) => row.id"
      table-layout="fixed"
    />

    <task-action-dialog
      v-model="showAction"
      :task-id="currentTaskId"
      :action="currentAction"
      @success="getDataList"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StrixBlock from '@/components/common/StrixBlock.vue'
import TaskActionDialog from './components/common/TaskActionDialog.vue'
import { workflowApi } from '@/api/workflow'
import type { WfTask } from '@/api/workflow'
import { handleOperate } from '@/utils/strix-table-tool'
import type { DataTableColumns } from 'naive-ui'

const router = useRouter()
const dataRef = ref<WfTask[]>([])
const dataLoading = ref(true)
const listParams = ref({ keyword: null as string | null, pageIndex: 1, pageSize: 10 })
const pagination = ref({ page: 1, pageSize: 10, itemCount: 0, showSizePicker: true })

const showAction = ref(false)
const currentTaskId = ref('')
const currentAction = ref<any>('approve')

function clearSearch() {
  listParams.value.keyword = null
  getDataList()
}

const columns: DataTableColumns = [
  { key: 'instanceTitle', title: '流程名称', width: 200 },
  { key: 'nodeName', title: '当前节点', width: 150 },
  { key: 'createdTime', title: '到达时间', width: 180 },
  {
    key: 'actions',
    title: '操作',
    width: 320,
    align: 'center',
    render(row: any) {
      return handleOperate([
        { type: 'success', label: '通过', icon: 'check', onClick: () => openAction(row.id, 'approve') },
        { type: 'error', label: '拒绝', icon: 'x', onClick: () => openAction(row.id, 'reject') },
        { type: 'warning', label: '回退', icon: 'undo', onClick: () => openAction(row.id, 'return') },
        { type: 'info', label: '转办', icon: 'user-plus', onClick: () => openAction(row.id, 'delegate') },
        {
          type: 'primary',
          label: '详情',
          icon: 'eye',
          onClick: () => router.push({ name: 'WorkflowInstanceDetail', params: { id: row.instanceId } })
        }
      ])
    }
  }
]

function openAction(taskId: string, action: string) {
  currentTaskId.value = taskId
  currentAction.value = action
  showAction.value = true
}

async function getDataList() {
  dataLoading.value = true
  try {
    const { data: res } = await workflowApi.tasksMine(listParams.value)
    dataRef.value = res.data.items
    pagination.value.itemCount = res.data.total
  } finally {
    dataLoading.value = false
  }
}

onMounted(getDataList)
</script>
`

- [ ] **Step 3: Create WorkflowTaskDone page**

`ue
<!-- src/views/System/SystemModule/Workflow/WorkflowTaskDone.vue -->
<template>
  <div>
    <strix-block cleanable @clear="clearSearch">
      <template #body>
        <n-grid :cols="6" :x-gap="20">
          <n-gi span="6 s:3 m:2">
            <n-input-group>
              <n-input v-model:value="listParams.keyword" clearable placeholder="搜索..." />
              <n-button ghost type="primary" @click="getDataList">搜索</n-button>
            </n-input-group>
          </n-gi>
        </n-grid>
      </template>
    </strix-block>

    <n-data-table
      :columns="columns"
      :data="dataRef"
      :loading="dataLoading"
      :pagination="pagination"
      :row-key="(row: any) => row.id"
      table-layout="fixed"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StrixBlock from '@/components/common/StrixBlock.vue'
import { workflowApi } from '@/api/workflow'
import type { WfTask } from '@/api/workflow'
import { handleOperate } from '@/utils/strix-table-tool'
import type { DataTableColumns } from 'naive-ui'

const router = useRouter()
const dataRef = ref<WfTask[]>([])
const dataLoading = ref(true)
const listParams = ref({ keyword: null as string | null, pageIndex: 1, pageSize: 10 })
const pagination = ref({ page: 1, pageSize: 10, itemCount: 0, showSizePicker: true })

function clearSearch() {
  listParams.value.keyword = null
  getDataList()
}

const columns: DataTableColumns = [
  { key: 'instanceTitle', title: '流程名称', width: 200 },
  { key: 'nodeName', title: '处理节点', width: 150 },
  { key: 'createdTime', title: '处理时间', width: 180 },
  {
    key: 'actions',
    title: '操作',
    width: 120,
    align: 'center',
    render(row: any) {
      return handleOperate([
        {
          type: 'primary',
          label: '详情',
          icon: 'eye',
          onClick: () => router.push({ name: 'WorkflowInstanceDetail', params: { id: row.instanceId } })
        }
      ])
    }
  }
]

async function getDataList() {
  dataLoading.value = true
  try {
    const { data: res } = await workflowApi.tasksDone(listParams.value)
    dataRef.value = res.data.items
    pagination.value.itemCount = res.data.total
  } finally {
    dataLoading.value = false
  }
}

onMounted(getDataList)
</script>
`

- [ ] **Step 4: Create WorkflowTaskInitiated page**

`ue
<!-- src/views/System/SystemModule/Workflow/WorkflowTaskInitiated.vue -->
<template>
  <div>
    <strix-block cleanable @clear="clearSearch">
      <template #body>
        <n-grid :cols="6" :x-gap="20">
          <n-gi span="6 s:3 m:2">
            <n-input-group>
              <n-input v-model:value="listParams.keyword" clearable placeholder="搜索..." />
              <n-button ghost type="primary" @click="getDataList">搜索</n-button>
            </n-input-group>
          </n-gi>
        </n-grid>
      </template>
    </strix-block>

    <n-data-table
      :columns="columns"
      :data="dataRef"
      :loading="dataLoading"
      :pagination="pagination"
      :row-key="(row: any) => row.id"
      table-layout="fixed"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StrixBlock from '@/components/common/StrixBlock.vue'
import { workflowApi } from '@/api/workflow'
import type { WfTask } from '@/api/workflow'
import { handleOperate } from '@/utils/strix-table-tool'
import { type DataTableColumns, NTag } from 'naive-ui'

const router = useRouter()
const dataRef = ref<WfTask[]>([])
const dataLoading = ref(true)
const listParams = ref({ keyword: null as string | null, pageIndex: 1, pageSize: 10 })
const pagination = ref({ page: 1, pageSize: 10, itemCount: 0, showSizePicker: true })

function clearSearch() {
  listParams.value.keyword = null
  getDataList()
}

const columns: DataTableColumns = [
  { key: 'instanceTitle', title: '流程名称', width: 200 },
  { key: 'nodeName', title: '当前节点', width: 150 },
  { key: 'createdTime', title: '发起时间', width: 180 },
  {
    key: 'actions',
    title: '操作',
    width: 180,
    align: 'center',
    render(row: any) {
      return handleOperate([
        {
          type: 'primary',
          label: '详情',
          icon: 'eye',
          onClick: () => router.push({ name: 'WorkflowInstanceDetail', params: { id: row.instanceId } })
        },
        {
          type: 'warning',
          label: '撤销',
          icon: 'rotate-ccw',
          onClick: () => withdrawInstance(row.instanceId),
          popconfirm: true
        }
      ])
    }
  }
]

async function withdrawInstance(instanceId: string) {
  try {
    await workflowApi.instanceCancel(instanceId, { reason: '发起人撤销' })
    getDataList()
  } catch {}
}

async function getDataList() {
  dataLoading.value = true
  try {
    const { data: res } = await workflowApi.tasksInitiated(listParams.value)
    dataRef.value = res.data.items
    pagination.value.itemCount = res.data.total
  } finally {
    dataLoading.value = false
  }
}

onMounted(getDataList)
</script>
`

- [ ] **Step 5: Commit**

`ash
git add src/views/System/SystemModule/Workflow/components/common/TaskActionDialog.vue \
        src/views/System/SystemModule/Workflow/WorkflowTaskMine.vue \
        src/views/System/SystemModule/Workflow/WorkflowTaskDone.vue \
        src/views/System/SystemModule/Workflow/WorkflowTaskInitiated.vue
git commit -m "feat(workflow): add task workbench pages with action dialog

Mine/Done/Initiated task lists + approve/reject/return/delegate dialog

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 9: Instance Detail Page

Instance detail with timeline, flow visualization, and comments.

**Files:**
- Create: `src/views/System/SystemModule/Workflow/components/common/InstanceTimeline.vue`
- Create: `src/views/System/SystemModule/Workflow/components/common/InstanceFlowView.vue`
- Create: `src/views/System/SystemModule/Workflow/WorkflowInstanceDetail.vue`

- [ ] **Step 1: Create InstanceTimeline component**

Renders the audit log as a timeline.

`ue
<!-- src/views/System/SystemModule/Workflow/components/common/InstanceTimeline.vue -->
<template>
  <n-timeline>
    <n-timeline-item
      v-for="log in logs"
      :key="log.id"
      :type="getLogType(log.action)"
      :title="log.nodeName"
      :content="log.comment || log.detail || ''"
      :time="log.createdTime"
    >
      <template #header>
        <n-flex align="center" :size="8">
          <span class="timeline-action">{{ getActionLabel(log.action) }}</span>
          <span v-if="log.operatorName" class="timeline-operator">{{ log.operatorName }}</span>
        </n-flex>
      </template>
    </n-timeline-item>
  </n-timeline>
</template>

<script lang="ts" setup>
import type { WfLog } from '@/api/workflow'

defineProps<{
  logs: WfLog[]
}>()

const actionLabels: Record<string, string> = {
  START: '发起流程',
  APPROVE: '通过',
  REJECT: '拒绝',
  RETURN: '回退',
  DELEGATE: '转办',
  COUNTERSIGN: '加签',
  REMOVE_SIGN: '降签',
  WITHDRAW: '撕回',
  CC: '抄送',
  AUTO_COMPLETE: '自动完成',
  TIMEOUT: '超时',
  CANCEL: '撤销',
  COMPLETE: '流程结束',
  SUSPEND: '挂起',
  RESUME: '恢复'
}

const typeMap: Record<string, string> = {
  START: 'info',
  APPROVE: 'success',
  REJECT: 'error',
  RETURN: 'warning',
  DELEGATE: 'info',
  COMPLETE: 'success',
  CANCEL: 'error'
}

function getActionLabel(action: string): string {
  return actionLabels[action] || action
}

function getLogType(action: string): 'default' | 'info' | 'success' | 'warning' | 'error' {
  return (typeMap[action] || 'default') as any
}
</script>

<style lang="scss" scoped>
.timeline-action {
  font-weight: 600;
  font-size: 13px;
}
.timeline-operator {
  font-size: 12px;
  color: #909399;
}
</style>
`

- [ ] **Step 2: Create InstanceFlowView component**

Read-only flow visualization showing current state (active/completed nodes).

`ue
<!-- src/views/System/SystemModule/Workflow/components/common/InstanceFlowView.vue -->
<template>
  <div class="wf-flow-view">
    <div v-if="loading" class="wf-flow-view__loading">
      <n-spin size="large" />
    </div>
    <designer-canvas v-else-if="tree" :readonly="true" />
    <n-empty v-else description="暂无流程图数据" />
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, watch } from 'vue'
import { workflowApi } from '@/api/workflow'
import { graphToTree } from '@/utils/workflow-graph'
import type { DesignerTreeNode, WorkflowGraph } from '@/api/workflow'
import { useWorkflowStore } from '@/stores/workflow'
import DesignerCanvas from '../designer/DesignerCanvas.vue'

const props = defineProps<{
  instanceId: string
}>()

const store = useWorkflowStore()
const loading = ref(true)
const tree = ref<DesignerTreeNode | null>(null)

async function loadGraph() {
  loading.value = true
  try {
    const { data: res } = await workflowApi.instanceGraph(props.instanceId)
    if (res.data.graphJson) {
      const graph: WorkflowGraph = JSON.parse(res.data.graphJson)
      const graphTree = graphToTree(graph)
      // For read-only view, directly set the store tree temporarily
      store.tree = graphTree
      tree.value = graphTree
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadGraph)
watch(() => props.instanceId, loadGraph)
</script>

<style lang="scss" scoped>
.wf-flow-view {
  width: 100%;
  min-height: 300px;

  &__loading {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 200px;
  }
}
</style>
`

- [ ] **Step 3: Create WorkflowInstanceDetail page**

`ue
<!-- src/views/System/SystemModule/Workflow/WorkflowInstanceDetail.vue -->
<template>
  <div v-if="instance" class="instance-detail">
    <!-- Header -->
    <n-card size="small">
      <n-descriptions :column="3" label-placement="left" bordered>
        <n-descriptions-item label="流程名称">{{ instance.title }}</n-descriptions-item>
        <n-descriptions-item label="发起人">{{ instance.initiatorName || instance.initiatorId }}</n-descriptions-item>
        <n-descriptions-item label="状态">
          <n-tag :type="statusType" size="small">{{ statusLabel }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="发起时间">{{ instance.startTime }}</n-descriptions-item>
        <n-descriptions-item label="结束时间">{{ instance.endTime || '-' }}</n-descriptions-item>
        <n-descriptions-item label="业务关联">{{ instance.bizType ? \:\ : '-' }}</n-descriptions-item>
      </n-descriptions>
    </n-card>

    <!-- Tabs: Timeline / Flow View -->
    <n-tabs type="line" style="margin-top: 16px">
      <n-tab-pane name="timeline" tab="审批记录">
        <instance-timeline :logs="logs" />
      </n-tab-pane>
      <n-tab-pane name="flow" tab="流程图">
        <instance-flow-view :instance-id="instanceId" />
      </n-tab-pane>
    </n-tabs>

    <!-- Actions (for running instances) -->
    <n-card v-if="instance.status === 1" size="small" style="margin-top: 16px">
      <n-flex :size="12">
        <n-button type="warning" ghost @click="handleSuspend" :disabled="instance.status !== 1">挂起</n-button>
        <n-button type="error" ghost @click="handleCancel">撤销流程</n-button>
      </n-flex>
    </n-card>
  </div>
  <div v-else class="instance-detail__loading">
    <n-spin size="large" />
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { workflowApi } from '@/api/workflow'
import type { WfInstance, WfLog } from '@/api/workflow'
import InstanceTimeline from './components/common/InstanceTimeline.vue'
import InstanceFlowView from './components/common/InstanceFlowView.vue'

const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const instanceId = route.params.id as string

const instance = ref<WfInstance | null>(null)
const logs = ref<WfLog[]>([])

const statusMap: Record<number, { label: string; type: string }> = {
  1: { label: '运行中', type: 'info' },
  2: { label: '已完成', type: 'success' },
  3: { label: '已拒绝', type: 'error' },
  4: { label: '已撤销', type: 'warning' },
  5: { label: '已挂起', type: 'default' }
}

const statusLabel = computed(() => statusMap[instance.value?.status ?? 0]?.label || '未知')
const statusType = computed(() => (statusMap[instance.value?.status ?? 0]?.type || 'default') as any)

onMounted(async () => {
  const [instRes, logsRes] = await Promise.all([
    workflowApi.instanceDetail(instanceId),
    workflowApi.instanceLogs(instanceId)
  ])
  instance.value = instRes.data.data
  logs.value = logsRes.data.data.items
})

function handleSuspend() {
  dialog.warning({
    title: '确认挂起',
    content: '挂起后该流程将暂停执行，是否继续？',
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      await workflowApi.instanceSuspend(instanceId)
      message.success('已挂起')
      instance.value!.status = 5
    }
  })
}

function handleCancel() {
  dialog.error({
    title: '确认撤销',
    content: '撤销后该流程将终止，是否继续？',
    positiveText: '确认撤销',
    negativeText: '取消',
    onPositiveClick: async () => {
      await workflowApi.instanceCancel(instanceId, { reason: '管理员撤销' })
      message.success('已撤销')
      instance.value!.status = 4
    }
  })
}
</script>

<style lang="scss" scoped>
.instance-detail {
  &__loading {
    display: flex;
    justify-content: center;
    padding: 60px 0;
  }
}
</style>
`

- [ ] **Step 4: Commit**

`ash
git add src/views/System/SystemModule/Workflow/components/common/InstanceTimeline.vue \
        src/views/System/SystemModule/Workflow/components/common/InstanceFlowView.vue \
        src/views/System/SystemModule/Workflow/WorkflowInstanceDetail.vue
git commit -m "feat(workflow): add instance detail page with timeline and flow view

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 10: Monitoring Dashboard + Delegation Settings

Admin dashboard with stats + delegation management page.

**Files:**
- Create: `src/views/System/SystemModule/Workflow/WorkflowMonitor.vue`
- Create: `src/views/System/SystemModule/Workflow/WorkflowDelegation.vue`

- [ ] **Step 1: Create WorkflowMonitor page**

Dashboard with summary stats cards and recent instances list.

`ue
<!-- src/views/System/SystemModule/Workflow/WorkflowMonitor.vue -->
<template>
  <div class="wf-monitor">
    <!-- Stats Cards -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <n-gi span="4 s:2 m:1" v-for="card in statsCards" :key="card.label">
        <n-card size="small">
          <n-statistic :label="card.label" :value="card.value">
            <template #prefix>
              <component :is="card.icon" :size="20" :color="card.color" />
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- Recent instances -->
    <n-card size="small" title="运行中的流程实例" style="margin-top: 16px">
      <n-data-table
        :columns="columns"
        :data="instances"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(row: any) => row.id"
        table-layout="fixed"
        size="small"
      />
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { workflowApi } from '@/api/workflow'
import type { WfStatsResp, WfInstance } from '@/api/workflow'
import { handleOperate } from '@/utils/strix-table-tool'
import { Layers, Play, CheckCircle, Clock } from 'lucide-vue-next'
import { type DataTableColumns, NTag } from 'naive-ui'

const router = useRouter()
const stats = ref<WfStatsResp>({
  totalDefinitions: 0, activeDefinitions: 0,
  runningInstances: 0, completedToday: 0,
  pendingTasks: 0, avgCompletionTime: 0
})
const instances = ref<WfInstance[]>([])
const loading = ref(true)

const statsCards = computed(() => [
  { label: '流程定义', value: stats.value.totalDefinitions, icon: Layers, color: '#409eff' },
  { label: '运行中实例', value: stats.value.runningInstances, icon: Play, color: '#e6a23c' },
  { label: '今日完成', value: stats.value.completedToday, icon: CheckCircle, color: '#67c23a' },
  { label: '待办任务', value: stats.value.pendingTasks, icon: Clock, color: '#f56c6c' }
])

const columns: DataTableColumns = [
  { key: 'title', title: '流程名称', width: 200 },
  { key: 'initiatorName', title: '发起人', width: 120 },
  {
    key: 'status',
    title: '状态',
    width: 100,
    render(row: any) {
      const map: Record<number, { label: string; type: string }> = {
        1: { label: '运行中', type: 'info' },
        2: { label: '已完成', type: 'success' },
        3: { label: '已拒绝', type: 'error' },
        4: { label: '已撤销', type: 'warning' },
        5: { label: '已挂起', type: 'default' }
      }
      const s = map[row.status] || { label: '未知', type: 'default' }
      return h(NTag, { type: s.type as any, size: 'small' }, () => s.label)
    }
  },
  { key: 'startTime', title: '发起时间', width: 180 },
  {
    key: 'actions',
    title: '操作',
    width: 120,
    render(row: any) {
      return handleOperate([
        {
          type: 'primary',
          label: '详情',
          icon: 'eye',
          onClick: () => router.push({ name: 'WorkflowInstanceDetail', params: { id: row.id } })
        }
      ])
    }
  }
]

onMounted(async () => {
  loading.value = true
  try {
    const [statsRes, instancesRes] = await Promise.all([
      workflowApi.stats(),
      workflowApi.instanceList({ pageIndex: 1, pageSize: 20, status: 1 })
    ])
    stats.value = statsRes.data.data
    instances.value = instancesRes.data.data.items
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.wf-monitor {
  padding: 0;
}
</style>
`

- [ ] **Step 2: Create WorkflowDelegation page**

Delegation CRUD management page.

`ue
<!-- src/views/System/SystemModule/Workflow/WorkflowDelegation.vue -->
<template>
  <div>
    <strix-block>
      <template #body>
        <n-flex :size="12">
          <n-button type="primary" @click="showAddModal = true">设置代理</n-button>
        </n-flex>
      </template>
    </strix-block>

    <n-data-table
      :columns="columns"
      :data="delegations"
      :loading="loading"
      :row-key="(row: any) => row.id"
      table-layout="fixed"
    />

    <!-- Add delegation modal -->
    <n-modal v-model:show="showAddModal" preset="card" title="设置审批代理" size="medium">
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="auto">
        <n-form-item label="代理人" path="delegateId">
          <n-select
            v-model:value="form.delegateId"
            filterable remote
            :options="managerOptions"
            :loading="managerLoading"
            placeholder="搜索选择代理人"
            @search="searchManagers"
          />
        </n-form-item>
        <n-form-item label="代理流程" path="definitionId">
          <n-select
            v-model:value="form.definitionId"
            :options="definitionOptions"
            placeholder="全部流程（可选）"
            clearable
          />
        </n-form-item>
        <n-form-item label="开始时间" path="startTime">
          <n-date-picker v-model:formatted-value="form.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" clearable style="width:100%" />
        </n-form-item>
        <n-form-item label="结束时间" path="endTime">
          <n-date-picker v-model:formatted-value="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" clearable style="width:100%" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-flex justify="end">
          <n-button @click="showAddModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitDelegation">确定</n-button>
        </n-flex>
      </template>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, h } from 'vue'
import StrixBlock from '@/components/common/StrixBlock.vue'
import { workflowApi } from '@/api/workflow'
import type { WfDelegation } from '@/api/workflow'
import { handleOperate } from '@/utils/strix-table-tool'
import { http } from '@/plugins/axios'
import type { RetResult } from '@/api/types'
import { type DataTableColumns, type FormRules, useMessage } from 'naive-ui'

const message = useMessage()
const delegations = ref<WfDelegation[]>([])
const loading = ref(true)

const showAddModal = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  delegateId: null as string | null,
  definitionId: null as string | null,
  startTime: '',
  endTime: ''
})

const formRules: FormRules = {
  delegateId: { required: true, message: '请选择代理人', trigger: 'change' },
  startTime: { required: true, message: '请选择开始时间', trigger: 'change' },
  endTime: { required: true, message: '请选择结束时间', trigger: 'change' }
}

// Manager search
const managerOptions = ref<Array<{ label: string; value: string }>>([])
const managerLoading = ref(false)

async function searchManagers(query: string) {
  if (!query) return
  managerLoading.value = true
  try {
    const { data: res } = await http.get<RetResult<{ items: any[] }>>('system/manager', {
      params: { keyword: query, pageIndex: 1, pageSize: 20 }
    })
    managerOptions.value = res.data.items.map((m: any) => ({
      label: m.realName || m.managerName,
      value: m.id
    }))
  } finally {
    managerLoading.value = false
  }
}

// Definition options
const definitionOptions = ref<Array<{ label: string; value: string }>>([])

async function loadDefinitions() {
  try {
    const { data: res } = await workflowApi.definitionList({ pageIndex: 1, pageSize: 100 })
    definitionOptions.value = res.data.items.map(d => ({ label: d.name, value: d.id }))
  } catch {}
}

const columns: DataTableColumns = [
  { key: 'delegatorId', title: '委托人', width: 150 },
  { key: 'delegateId', title: '代理人', width: 150 },
  { key: 'definitionId', title: '流程范围', width: 200, render: (row: any) => row.definitionId || '全部流程' },
  { key: 'startTime', title: '开始时间', width: 180 },
  { key: 'endTime', title: '结束时间', width: 180 },
  {
    key: 'actions',
    title: '操作',
    width: 100,
    render(row: any) {
      return handleOperate([
        {
          type: 'error',
          label: '撤销',
          icon: 'trash',
          onClick: () => removeDelegation(row.id),
          popconfirm: true
        }
      ])
    }
  }
]

async function loadDelegations() {
  loading.value = true
  try {
    const { data: res } = await workflowApi.delegationList()
    delegations.value = res.data
  } finally {
    loading.value = false
  }
}

async function submitDelegation() {
  try {
    await formRef.value?.validate()
  } catch { return }

  submitting.value = true
  try {
    await workflowApi.delegationCreate({
      delegateId: form.delegateId!,
      definitionId: form.definitionId || undefined,
      startTime: form.startTime,
      endTime: form.endTime
    })
    message.success('代理设置成功')
    showAddModal.value = false
    loadDelegations()
  } finally {
    submitting.value = false
  }
}

async function removeDelegation(id: string) {
  await workflowApi.delegationRemove(id)
  message.success('已撤销')
  loadDelegations()
}

onMounted(() => {
  loadDelegations()
  loadDefinitions()
})
</script>
`

- [ ] **Step 3: Commit**

`ash
git add src/views/System/SystemModule/Workflow/WorkflowMonitor.vue \
        src/views/System/SystemModule/Workflow/WorkflowDelegation.vue
git commit -m "feat(workflow): add monitor dashboard and delegation page

Stats cards, running instances list, delegation CRUD

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Task 11: Build Verification

Verify the frontend compiles successfully.

- [ ] **Step 1: Run frontend build**

`ash
cd Z:\Projects\VueProjects\StrixPage
npm run build
`

Expected: Build succeeds with no TypeScript errors.

- [ ] **Step 2: Fix any issues and commit**

`ash
git add -A
git commit -m "fix(workflow): build fixups for frontend

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
`

---

## Summary

Plan 4 creates the complete frontend implementation:

| Category | Count | Location |
|----------|-------|----------|
| API + Types | 1 | `src/api/workflow.ts` |
| Utils | 1 | `src/utils/workflow-graph.ts` |
| Store | 1 | `src/stores/workflow.ts` |
| Router | 1 (modify) | `src/router/index.ts` |
| Designer Components | 6 | `components/designer/` |
| Config Panels | 7 | `components/config/` |
| Common Components | 4 | `components/common/` |
| Pages | 8 | `Workflow/` |
| **Total** | **~29 files** | |

After all 4 plans are complete, the full workflow system is ready for implementation.
