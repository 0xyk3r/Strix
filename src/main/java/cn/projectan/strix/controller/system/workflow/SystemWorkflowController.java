package cn.projectan.strix.controller.system.workflow;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.WorkflowInstance;
import cn.projectan.strix.model.db.system.WorkflowTask;
import cn.projectan.strix.model.db.system.WorkflowTaskAssign;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.response.system.workflow.task.WorkflowTaskFinishedListResp;
import cn.projectan.strix.model.response.system.workflow.task.WorkflowTaskUnfinishedListResp;
import cn.projectan.strix.service.system.WorkflowInstanceService;
import cn.projectan.strix.service.system.WorkflowTaskAssignService;
import cn.projectan.strix.service.system.WorkflowTaskService;
import cn.projectan.strix.util.async.ParallelExecution;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
    public RetResult<WorkflowTaskUnfinishedListResp> unfinished(BasePageReq<WorkflowTaskAssign> req) {
        // 查询指派给当前用户的未处理任务
        Page<WorkflowTaskAssign> page = workflowTaskAssignService.listUnfinishedPage(loginManagerId(), req.getPage());

        // 查询任务和实例信息
        Set<String> workflowTaskIdList = page.getRecords().stream()
                .map(WorkflowTaskAssign::getTaskId)
                .collect(Collectors.toSet());
        Set<String> workflowInstanceIdList = page.getRecords().stream()
                .map(WorkflowTaskAssign::getInstanceId)
                .collect(Collectors.toSet());
        AtomicReference<List<WorkflowTask>> workflowTaskList = new AtomicReference<>(Collections.emptyList());
        AtomicReference<List<WorkflowInstance>> workflowInstanceList = new AtomicReference<>(Collections.emptyList());
        ParallelExecution.allOf(
                () -> workflowTaskList.set(workflowTaskService.listByTaskIds(workflowTaskIdList)),
                () -> workflowInstanceList.set(workflowInstanceService.listByInstanceIds(workflowInstanceIdList))
        );
        // 组装返回结果
        return RetBuilder.success(
                new WorkflowTaskUnfinishedListResp(
                        page.getRecords(),
                        page.getTotal(),
                        workflowTaskList.get(),
                        workflowInstanceList.get()
                )
        );
    }

    /**
     * 查询我的工作区已处理任务列表
     */
    @GetMapping("finished")
    @PreAuthorize("@ss.hasPermission('system:workflow')")
    @StrixLog(operationGroup = "工作区", operationName = "查询我的工作区已处理任务列表")
    public RetResult<WorkflowTaskFinishedListResp> finished(BasePageReq<WorkflowTaskAssign> req) {
        // 查询指派给当前用户的已处理任务
        Page<WorkflowTaskAssign> page = workflowTaskAssignService.listFinishedPage(loginManagerId(), req.getPage());

        // 查询任务和实例信息
        Set<String> workflowTaskIdList = page.getRecords().stream()
                .map(WorkflowTaskAssign::getTaskId)
                .collect(Collectors.toSet());
        Set<String> workflowInstanceIdList = page.getRecords().stream()
                .map(WorkflowTaskAssign::getInstanceId)
                .collect(Collectors.toSet());
        AtomicReference<List<WorkflowTask>> workflowTaskList = new AtomicReference<>(Collections.emptyList());
        AtomicReference<List<WorkflowInstance>> workflowInstanceList = new AtomicReference<>(Collections.emptyList());
        ParallelExecution.allOf(
                () -> workflowTaskList.set(workflowTaskService.listByTaskIds(workflowTaskIdList)),
                () -> workflowInstanceList.set(workflowInstanceService.listByInstanceIds(workflowInstanceIdList))
        );
        // 组装返回结果
        return RetBuilder.success(
                new WorkflowTaskFinishedListResp(
                        page.getRecords(),
                        page.getTotal(),
                        workflowTaskList.get(),
                        workflowInstanceList.get()
                )
        );
    }

    /**
     * 查询我的工作区已发起任务列表
     */
    @GetMapping("initiated")
    @PreAuthorize("@ss.hasPermission('system:workflow')")
    @StrixLog(operationGroup = "工作区", operationName = "查询我的工作区已发起任务列表")
    public RetResult<Object> initiated(BasePageReq<WorkflowInstance> req) {
        Page<WorkflowInstance> page = workflowInstanceService.listPageByCreator(loginManagerId(), req.getPage());

        return RetBuilder.success(page);
    }

    /**
     * 查询我的工作区被抄送任务列表
     */
    @GetMapping("cc")
    @PreAuthorize("@ss.hasPermission('system:workflow')")
    @StrixLog(operationGroup = "工作区", operationName = "查询我的工作区被抄送任务列表")
    public RetResult<Object> cc(BasePageReq<WorkflowTaskAssign> req) {
        Page<WorkflowTaskAssign> page = workflowTaskAssignService.listCcPage(loginManagerId(), req.getPage());

        return RetBuilder.success(page);
    }

}
