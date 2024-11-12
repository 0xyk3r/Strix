package cn.projectan.strix.controller.system.workflow;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.cache.WorkflowConfigCache;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.Workflow;
import cn.projectan.strix.model.db.WorkflowConfig;
import cn.projectan.strix.model.db.WorkflowInstance;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.system.workflow.WorkflowConfigUpdateReq;
import cn.projectan.strix.model.request.system.workflow.WorkflowListReq;
import cn.projectan.strix.model.request.system.workflow.WorkflowUpdateReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.model.response.system.workflow.WorkflowListResp;
import cn.projectan.strix.model.response.system.workflow.WorkflowResp;
import cn.projectan.strix.service.WorkflowConfigService;
import cn.projectan.strix.service.WorkflowInstanceService;
import cn.projectan.strix.service.WorkflowService;
import cn.projectan.strix.util.UniqueChecker;
import cn.projectan.strix.util.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流程配置管理
 *
 * @author ProjectAn
 * @since 2024/4/24 下午12:50
 */
@Slf4j
@RestController
@RequestMapping("system/workflow/config")
@RequiredArgsConstructor
public class SystemWorkflowConfigController extends BaseSystemController {

    private final WorkflowService workflowService;
    private final WorkflowConfigService workflowConfigService;
    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowConfigCache workflowConfigCache;

    /**
     * 查询工作流引擎列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:workflow:config')")
    @StrixLog(operationGroup = "工作流引擎", operationName = "查询工作流引擎列表")
    public RetResult<WorkflowListResp> list(WorkflowListReq req) {
        Page<Workflow> page = workflowService.lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), Workflow::getName, req.getKeyword())
                .page(req.getPage());

        List<String> workflowIdList = page.getRecords().stream().map(Workflow::getId).toList();
        List<WorkflowConfig> workflowConfigList = workflowConfigService.lambdaQuery()
                .in(WorkflowConfig::getWorkflowId, workflowIdList)
                .list();

        return RetBuilder.success(new WorkflowListResp(page.getRecords(), page.getTotal(), workflowConfigList));
    }

    /**
     * 查询工作流引擎信息
     */
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:workflow:config')")
    @StrixLog(operationGroup = "工作流引擎", operationName = "查询工作流引擎信息")
    public RetResult<WorkflowResp> info(@PathVariable String id) {
        Workflow workflow = workflowService.getById(id);
        Assert.notNull(workflow, "工作流信息不存在");

        return RetBuilder.success(new WorkflowResp(workflow));
    }

    /**
     * 新增工作流引擎
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:workflow:config:add')")
    @StrixLog(operationGroup = "工作流引擎", operationName = "新增工作流引擎", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) WorkflowUpdateReq req) {
        Workflow workflow = new Workflow();
        workflow.setName(req.getName());

        UniqueChecker.check(workflow);

        Assert.isTrue(workflowService.save(workflow), "保存失败");
        workflowConfigCache.refresh();
        return RetBuilder.success();
    }

    /**
     * 修改工作流引擎
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:workflow:config:update')")
    @StrixLog(operationGroup = "工作流引擎", operationName = "修改工作流引擎", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) WorkflowUpdateReq req) {
        Workflow workflow = workflowService.getById(id);
        Assert.notNull(workflow, "原记录不存在");

        LambdaUpdateWrapper<Workflow> updateWrapper = UpdateBuilder.build(workflow, req);
        UniqueChecker.check(workflow);

        Assert.isTrue(workflowService.update(updateWrapper), "保存失败");
        workflowConfigCache.refresh();
        return RetBuilder.success();
    }

    /**
     * 删除工作流引擎
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:workflow:config:remove')")
    @StrixLog(operationGroup = "工作流引擎", operationName = "删除工作流引擎", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        workflowService.removeById(id);
        // 删除关联的配置信息、实例信息等
        workflowConfigService.lambdaUpdate()
                .eq(WorkflowConfig::getWorkflowId, id)
                .remove();
        workflowInstanceService.lambdaUpdate()
                .eq(WorkflowInstance::getWorkflowId, id)
                .remove();
        workflowConfigCache.refresh();

        return RetBuilder.success();
    }

    /**
     * 获取工作流引擎下拉列表
     */
    @GetMapping("select")
    public RetResult<CommonSelectDataResp> getSmsConfigSelectList() {
        return RetBuilder.success(workflowService.getSelectData());
    }

    /**
     * 获取工作流配置
     */
    @GetMapping("config/{configId}")
    @PreAuthorize("@ss.hasPermission('system:workflow:config')")
    @StrixLog(operationGroup = "工作流引擎", operationName = "获取工作流配置")
    public RetResult<Object> getConfig(@PathVariable String configId) {
        WorkflowConfig workflowConfig = workflowConfigService.getById(configId);
        Assert.notNull(workflowConfig, "配置信息不存在");

        return RetBuilder.success(workflowConfig);
    }

    /**
     * 添加工作流配置
     */
    @PostMapping("update/{id}/config")
    @PreAuthorize("@ss.hasPermission('system:workflow:config:update')")
    @StrixLog(operationGroup = "工作流引擎", operationName = "添加工作流配置", operationType = SysLogOperType.ADD)
    public RetResult<Object> updateConfig(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) WorkflowConfigUpdateReq req) {
        workflowService.saveConfig(id, req.getContent());
        workflowConfigCache.refresh();

        return RetBuilder.success();
    }

}
