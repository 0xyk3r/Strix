package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.WorkflowMapper;
import cn.projectan.strix.model.db.Workflow;
import cn.projectan.strix.model.db.WorkflowConfig;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.WorkflowConfigService;
import cn.projectan.strix.service.WorkflowService;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * <p>
 * Strix 工作流 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
@Service
public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {

    @Override
    public CommonSelectDataResp getSelectData() {
        List<Workflow> list = getBaseMapper().selectList(Wrappers.<Workflow>lambdaQuery().select(Workflow::getId, Workflow::getName));
        return new CommonSelectDataResp(list, "id", "name", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(String workflowId, String configJson) {
        WorkflowService workflowService = SpringUtil.getAopProxy(this);
        WorkflowConfigService workflowConfigService = SpringUtil.getBean(WorkflowConfigService.class);

        Workflow workflow = workflowService.getById(workflowId);
        Assert.notNull(workflow, "工作流不存在");

        WorkflowConfig workflowConfig = new WorkflowConfig();
        workflowConfig.setWorkflowId(workflow.getId());
        workflowConfig.setVersion(workflow.getVersion() == null ? 1 : workflow.getVersion() + 1);
        workflowConfig.setContent(configJson);
        Assert.isTrue(workflowConfigService.save(workflowConfig), "保存失败");

        workflow.setVersion(workflowConfig.getVersion());
        Assert.isTrue(workflowService.updateById(workflow), "保存失败");
    }

}
