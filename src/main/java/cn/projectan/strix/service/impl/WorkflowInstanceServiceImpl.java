package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.cache.WorkflowConfigCache;
import cn.projectan.strix.core.module.workflow.WorkflowHandler;
import cn.projectan.strix.core.module.workflow.WorkflowTool;
import cn.projectan.strix.mapper.WorkflowInstanceMapper;
import cn.projectan.strix.model.db.WorkflowConfig;
import cn.projectan.strix.model.db.WorkflowInstance;
import cn.projectan.strix.model.dict.WorkflowInstanceStatus;
import cn.projectan.strix.model.dict.WorkflowNodeType;
import cn.projectan.strix.model.other.system.workflow.WorkflowNode;
import cn.projectan.strix.service.WorkflowConfigService;
import cn.projectan.strix.service.WorkflowInstanceService;
import cn.projectan.strix.service.WorkflowTaskService;
import cn.projectan.strix.util.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * Strix 工作流实例 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowInstanceServiceImpl extends ServiceImpl<WorkflowInstanceMapper, WorkflowInstance> implements WorkflowInstanceService {

    private final WorkflowConfigService workflowConfigService;
    private final WorkflowTaskService workflowTaskService;
    private final WorkflowConfigCache workflowConfigCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createInstance(String workflowId, String creatorId) {
        WorkflowConfig config = workflowConfigService.getLatestConfig(workflowId);
        Assert.notNull(config, "工作流配置不存在");

        List<WorkflowNode> nodes = workflowConfigCache.getConfig(workflowId);
        Assert.notEmpty(nodes, "工作流配置为空");
        WorkflowNode rootNode = WorkflowTool.findRootNode(nodes);
        Assert.notNull(rootNode, "工作流配置根节点不存在");

        WorkflowInstance instance = new WorkflowInstance().setWorkflowId(config.getWorkflowId()).setWorkflowConfigId(config.getId()).setCurrentNodeId(rootNode.getId()).setCurrentNodeType(rootNode.getType()).setStartTime(LocalDateTime.now()).setStatus(WorkflowInstanceStatus.ACTIVE);
        SpringUtil.getAopProxy(this).saveAndProcess(instance);
    }

    @Override
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
        SpringUtil.getAopProxy(this).saveAndProcess(instance);
    }

    @Override
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
        SpringUtil.getAopProxy(this).saveAndProcess(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAndProcess(WorkflowInstance instance) {
        if (instance.getId() == null) {
            Assert.isTrue(SpringUtil.getAopProxy(this).save(instance), "保存失败");
        } else {
            Assert.isTrue(SpringUtil.getAopProxy(this).updateById(instance), "更新失败");
        }
        // 只对活动状态的实例进行后处理操作
        if (instance.getStatus() != WorkflowInstanceStatus.ACTIVE) {
            return;
        }

        // 创建任务
        workflowTaskService.createTask(instance);
        // 自动流转
        switch (instance.getCurrentNodeType()) {
            case WorkflowNodeType.ROOT, WorkflowNodeType.CC -> SpringUtil.getAopProxy(this).toNext(instance);
            case WorkflowNodeType.CONDITIONS -> {
                List<WorkflowNode> nodes = workflowConfigCache.getConfigById(instance.getWorkflowConfigId());
                Assert.notEmpty(nodes, "工作流配置为空");
                WorkflowNode targetNode = WorkflowTool.findNodeById(nodes, instance.getCurrentNodeId());
                WorkflowHandler handler = new WorkflowHandler(targetNode);
                String conditionsBranchNodeId = handler.getConditionsBranchNodeId();
                WorkflowNode nextNode = WorkflowTool.findNextNode(nodes, conditionsBranchNodeId);
                SpringUtil.getAopProxy(this).toNode(instance, nextNode.getId(), false);
            }

        }
    }

}
