package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.WorkflowMapper;
import cn.projectan.strix.model.db.system.Workflow;
import cn.projectan.strix.model.db.system.WorkflowConfig;
import cn.projectan.strix.model.request.system.workflow.WorkflowListReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.base.NameFetcherService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

    private final WorkflowConfigService workflowConfigService;

    /**
     * 分页查询工作流列表
     *
     * @param req 查询请求
     * @return 分页数据
     */
    public Page<Workflow> listPage(WorkflowListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), Workflow::getName, req.getKeyword())
                .page(req.getPage());
    }

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

        Workflow workflow = workflowService.getById(workflowId);
        Assert.notNull(workflow, I18nUtil.notFound("field.workflow"));

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
