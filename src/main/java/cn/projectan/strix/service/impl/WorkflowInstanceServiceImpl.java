package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.WorkflowInstanceMapper;
import cn.projectan.strix.model.db.WorkflowConfig;
import cn.projectan.strix.model.db.WorkflowInstance;
import cn.projectan.strix.model.dict.WorkflowInstanceStatus;
import cn.projectan.strix.model.other.module.workflow.WorkflowNode;
import cn.projectan.strix.service.WorkflowConfigService;
import cn.projectan.strix.service.WorkflowInstanceService;
import cn.projectan.strix.service.WorkflowTaskService;
import cn.projectan.strix.util.SpringUtil;
import cn.projectan.strix.util.WorkflowUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
public class WorkflowInstanceServiceImpl extends ServiceImpl<WorkflowInstanceMapper, WorkflowInstance> implements WorkflowInstanceService {

    private final WorkflowConfigService workflowConfigService;
    private final WorkflowTaskService workflowTaskService;
    private final WorkflowUtil workflowUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createInstance(String workflowId, String creatorId) {
        WorkflowConfig config = workflowConfigService.getLatestConfig(workflowId);
        Assert.notNull(config, "工作流配置不存在");

        List<WorkflowNode> nodes = workflowUtil.parseConfig(workflowId);
        Assert.notEmpty(nodes, "工作流配置为空");
        WorkflowNode rootNode = workflowUtil.findRootNode(nodes);
        Assert.notNull(rootNode, "工作流配置根节点不存在");

        WorkflowInstance instance = new WorkflowInstance()
                .setWorkflowId(config.getWorkflowId())
                .setWorkflowConfigId(config.getId())
                .setWorkflowConfigVersion(config.getVersion())
                .setCurrentNodeId(rootNode.getId())
                .setCurrentNodeType(rootNode.getType())
                .setCurrentOperatorId(creatorId)
                .setStartTime(LocalDateTime.now())
                .setStatus(WorkflowInstanceStatus.ACTIVE);
        Assert.isTrue(SpringUtil.getAopProxy(this).save(instance), "工作流实例创建失败");
        workflowTaskService.createTask(instance);

    }

}
