package cn.projectan.strix.controller.system.workflow;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.WorkflowInstance;
import cn.projectan.strix.model.db.WorkflowTask;
import cn.projectan.strix.model.db.WorkflowTaskAssign;
import cn.projectan.strix.model.dict.WorkflowNodeType;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.service.WorkflowInstanceService;
import cn.projectan.strix.service.WorkflowTaskAssignService;
import cn.projectan.strix.service.WorkflowTaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ProjectAn
 * @since 2024-10-30 09:49:02
 */
@Slf4j
@RestController
@RequestMapping("system/workflow")
@RequiredArgsConstructor
public class SystemWorkflowController extends BaseSystemController {

    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowTaskService workflowTaskService;
    private final WorkflowTaskAssignService workflowTaskAssignService;

    /**
     * 查询我的工作区待处理任务列表
     */
    @GetMapping("unfinished")
    @PreAuthorize("@ss.hasPermission('system:workflow')")
    @StrixLog(operationGroup = "工作区", operationName = "查询我的工作区待处理任务列表")
    public RetResult<Object> unfinished(BasePageReq<WorkflowTask> req) {
        Page<WorkflowTask> page = workflowTaskService.lambdaQuery()
                .eq(WorkflowTask::getOperatorId, loginManagerId())
                .isNull(WorkflowTask::getOperationType)
                .page(req.getPage());

        return RetBuilder.success(page);
    }

    /**
     * 查询我的工作区已处理任务列表
     */
    @GetMapping("finished")
    @PreAuthorize("@ss.hasPermission('system:workflow')")
    @StrixLog(operationGroup = "工作区", operationName = "查询我的工作区已处理任务列表")
    public RetResult<Object> finished(BasePageReq<WorkflowTask> req) {
        Page<WorkflowTask> page = workflowTaskService.lambdaQuery()
                .eq(WorkflowTask::getOperatorId, loginManagerId())
                .isNotNull(WorkflowTask::getOperationType)
                .page(req.getPage());

        return RetBuilder.success(page);
    }

    /**
     * 查询我的工作区已发起任务列表
     */
    @GetMapping("initiated")
    @PreAuthorize("@ss.hasPermission('system:workflow')")
    @StrixLog(operationGroup = "工作区", operationName = "查询我的工作区已发起任务列表")
    public RetResult<Object> initiated(BasePageReq<WorkflowInstance> req) {
        Page<WorkflowInstance> page = workflowInstanceService.lambdaQuery()
                .eq(WorkflowInstance::getCreateBy, loginManagerId())
                .page(req.getPage());

        return RetBuilder.success(page);
    }

    /**
     * 查询我的工作区被抄送任务列表
     */
    @GetMapping("cc")
    @PreAuthorize("@ss.hasPermission('system:workflow')")
    @StrixLog(operationGroup = "工作区", operationName = "查询我的工作区被抄送任务列表")
    public RetResult<Object> cc(BasePageReq<WorkflowTaskAssign> req) {
        Page<WorkflowTaskAssign> page = workflowTaskAssignService.lambdaQuery()
                .eq(WorkflowTaskAssign::getOperationType, WorkflowNodeType.CC)
                .eq(WorkflowTaskAssign::getOperatorId, loginManagerId())
                .page(req.getPage());

        return RetBuilder.success(page);
    }

}
