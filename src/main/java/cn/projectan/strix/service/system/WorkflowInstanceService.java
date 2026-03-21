package cn.projectan.strix.service.system;

import cn.projectan.strix.core.cache.system.WorkflowConfigCache;
import cn.projectan.strix.core.module.workflow.WorkflowHandler;
import cn.projectan.strix.core.module.workflow.WorkflowTool;
import cn.projectan.strix.mapper.system.WorkflowInstanceMapper;
import cn.projectan.strix.model.db.system.WorkflowConfig;
import cn.projectan.strix.model.db.system.WorkflowInstance;
import cn.projectan.strix.model.dict.system.WorkflowInstanceStatus;
import cn.projectan.strix.model.dict.system.WorkflowNodeType;
import cn.projectan.strix.model.other.system.workflow.WorkflowNode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Strix 工作流实例 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowInstanceService extends ServiceImpl<WorkflowInstanceMapper, WorkflowInstance> {

    private final WorkflowConfigService workflowConfigService;
    private final WorkflowTaskService workflowTaskService;
    private final WorkflowConfigCache workflowConfigCache;

    /**
     * 根据工作流ID删除实例
     *
     * @param workflowId 工作流ID
     */
    public void deleteByWorkflowId(String workflowId) {
        lambdaUpdate()
                .eq(WorkflowInstance::getWorkflowId, workflowId)
                .remove();
    }

    /**
     * 根据实例ID集合查询实例列表
     *
     * @param instanceIds 实例ID集合
     * @return 实例列表
     */
    public List<WorkflowInstance> listByInstanceIds(Set<String> instanceIds) {
        if (instanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .in(WorkflowInstance::getId, instanceIds)
                .list();
    }

    /**
     * 分页查询指定创建人的实例列表
     *
     * @param creatorId 创建人ID
     * @param page      分页参数
     * @return 分页数据
     */
    public Page<WorkflowInstance> listPageByCreator(String creatorId, Page<WorkflowInstance> page) {
        return lambdaQuery()
                .eq(WorkflowInstance::getCreatedBy, creatorId)
                .page(page);
    }

    /**
     * 创建工作流实例
     *
     * @param workflowId 工作流 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void createInstance(String workflowId, String workflowName) {
        WorkflowConfig config = workflowConfigService.getLatestConfig(workflowId);
        Assert.notNull(config, "工作流配置不存在");

        List<WorkflowNode> nodes = workflowConfigCache.getConfig(workflowId);
        Assert.notEmpty(nodes, "工作流配置为空");
        WorkflowNode rootNode = WorkflowTool.findRootNode(nodes);
        Assert.notNull(rootNode, "工作流配置根节点不存在");

        WorkflowInstance instance = new WorkflowInstance()
                .setName(workflowName)
                .setWorkflowId(config.getWorkflowId())
                .setWorkflowConfigId(config.getId())
                .setCurrentNodeId(rootNode.getId())
                .setCurrentNodeType(rootNode.getType())
                .setStartTime(LocalDateTime.now())
                .setStatus(WorkflowInstanceStatus.ACTIVE);
        saveAndProcess(instance);
    }

    /**
     * 转到指定节点
     *
     * @param instance 工作流实例
     * @param nodeId   节点 ID
     * @param isBack   是否为回退
     */
    @Transactional(rollbackFor = Exception.class)
    public void toNode(WorkflowInstance instance, String nodeId, boolean isBack) {
        List<WorkflowNode> nodes = workflowConfigCache.getConfigById(instance.getWorkflowConfigId());
        Assert.notEmpty(nodes, "工作流配置为空");

        WorkflowNode targetNode = WorkflowTool.findNodeById(nodes, nodeId);
        if (targetNode != null) {
            instance.setCurrentNodeId(targetNode.getId());
            instance.setCurrentNodeType(targetNode.getType());
        } else {
            instance.setStatus(isBack ? WorkflowInstanceStatus.CANCEL : WorkflowInstanceStatus.DONE);
            instance.setEndTime(LocalDateTime.now());
        }
        saveAndProcess(instance);
    }

    /**
     * 转到下一个节点
     *
     * @param instance 工作流实例
     */
    @Transactional(rollbackFor = Exception.class)
    public void toNext(WorkflowInstance instance) {
        List<WorkflowNode> nodes = workflowConfigCache.getConfigById(instance.getWorkflowConfigId());
        Assert.notEmpty(nodes, "工作流配置为空");

        WorkflowNode currentNode = WorkflowTool.findNodeById(nodes, instance.getCurrentNodeId());
        WorkflowNode nextNode = WorkflowTool.findNextNode(nodes, instance.getCurrentNodeId());
        if (nextNode != null) {
            // 进入下一个节点
            instance.setCurrentNodeId(nextNode.getId());
            instance.setCurrentNodeType(nextNode.getType());
        } else if (currentNode.getConditionsId() != null) {
            // 当前在条件分支内, 且处于节点末尾, 则结束条件分支
            WorkflowNode nextNode2 = WorkflowTool.findNextNode(nodes, currentNode.getConditionsId());
            instance.setCurrentNodeId(nextNode2.getId());
            instance.setCurrentNodeType(nextNode2.getType());
        } else {
            // 结束流程
            instance.setStatus(WorkflowInstanceStatus.DONE);
            instance.setEndTime(LocalDateTime.now());
        }
        saveAndProcess(instance);
    }

    /**
     * 更新实例信息并进行后置处理
     *
     * @param instance 工作流实例
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveAndProcess(WorkflowInstance instance) {
        if (instance.getId() == null) {
            Assert.isTrue(save(instance), "保存失败");
        } else {
            Assert.isTrue(updateById(instance), "更新失败");
        }
        // 只对活动状态的实例进行后处理操作
        if (instance.getStatus() != WorkflowInstanceStatus.ACTIVE) {
            return;
        }

        // 创建任务
        workflowTaskService.createTask(instance);

        // 自动流转
        switch (instance.getCurrentNodeType()) {
            case WorkflowNodeType.ROOT, WorkflowNodeType.CC -> toNext(instance);
            case WorkflowNodeType.CONDITIONS -> {
                List<WorkflowNode> nodes = workflowConfigCache.getConfigById(instance.getWorkflowConfigId());
                Assert.notEmpty(nodes, "工作流配置为空");
                WorkflowNode targetNode = WorkflowTool.findNodeById(nodes, instance.getCurrentNodeId());
                WorkflowHandler handler = new WorkflowHandler(targetNode);
                String conditionsBranchNodeId = handler.getConditionsBranchNodeId();
                WorkflowNode nextNode = WorkflowTool.findNextNode(nodes, conditionsBranchNodeId);
                toNode(instance, nextNode.getId(), false);
            }

        }
    }

}
