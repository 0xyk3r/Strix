package cn.projectan.strix.controller;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.response.common.CommonOperatorInfoResp;
import cn.projectan.strix.service.common.OperatorService;
import cn.projectan.strix.service.system.WorkflowInstanceService;
import cn.projectan.strix.service.system.WorkflowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 调试
 *
 * @author ProjectAn
 * @since 2022/7/29 14:19
 */
@Slf4j
@Anonymous
@IgnoreEncryption
@RestController
@RequestMapping("debug")
@ConditionalOnProperty(prefix = "spring.profiles", name = "active", havingValue = "dev")
@RequiredArgsConstructor
@Tag(name = "调试")
public class DebugController extends BaseController {

    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowTaskService workflowTaskService;
    private final OperatorService operatorService;

    @Operation(summary = "创建调试数据")
    @GetMapping("wf/create/{workflowId}")
    public RetResult<Object> create(@Parameter(description = "工作流 ID") @PathVariable String workflowId) {
        workflowInstanceService.createInstance(workflowId, "DEBUG发起的流程");
        return RetBuilder.success();
    }

    @Operation(summary = "审批调试")
    @GetMapping("wf/completeTask/{taskId}/{operationType}")
    public RetResult<Object> approval(@Parameter(description = "任务 ID") @PathVariable String taskId, @Parameter(description = "操作类型") @PathVariable Short operationType) {
        workflowTaskService.completeTask(taskId, "anjiongyi", operationType, "test comment");
        return RetBuilder.success();
    }

    @Operation(summary = "查询操作人信息")
    @GetMapping("operator/{operatorType}/{operatorId}")
    public RetResult<Object> queryOperatorInfo(@Parameter(description = "操作人类型") @PathVariable Short operatorType, @Parameter(description = "操作人 ID") @PathVariable String operatorId) {
        CommonOperatorInfoResp operatorInfoResp = operatorService.queryOperatorInfo(operatorType, operatorId);
        Assert.notNull(operatorInfoResp, "未找到该人员信息");
        return RetBuilder.success(operatorInfoResp);
    }

}
