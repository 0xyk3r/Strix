package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.WorkflowMapper;
import cn.projectan.strix.model.db.system.Workflow;
import cn.projectan.strix.model.db.system.WorkflowConfig;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.base.NameFetcherService;
import cn.projectan.strix.util.common.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * <p>
 * Strix 工作流 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService extends ServiceImpl<WorkflowMapper, Workflow> implements NameFetcherService<Workflow> {

    /**
     * 获取下拉选择数据
     *
     * @return 下拉选择数据
     */
    public CommonSelectDataResp getSelectData() {
        List<Workflow> list = lambdaQuery()
                .select(Workflow::getId, Workflow::getName)
                .list();
        return new CommonSelectDataResp(list, "id", "name", null);
    }

    /**
     * 保存工作流配置
     *
     * @param workflowId 工作流 ID
     * @param configJson 配置内容
     */
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

    @Override
    public String getDataNameById(String id) {
        Workflow data = lambdaQuery()
                .select(Workflow::getName)
                .eq(Workflow::getId, id)
                .one();
        return data == null ? null : data.getName();
    }

}
