package cn.projectan.strix.controller;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.service.WorkflowInstanceService;
import cn.projectan.strix.service.WorkflowTaskService;
import cn.projectan.strix.util.SpringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
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
@IgnoreDataEncryption
@RestController
@RequestMapping("debug")
@ConditionalOnProperty(prefix = "spring.profiles", name = "active", havingValue = "dev")
@RequiredArgsConstructor
public class DebugController extends BaseController {

    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowTaskService workflowTaskService;

    @GetMapping("wf/create/{workflowId}")
    public RetResult<Object> create(@PathVariable String workflowId) {
        workflowInstanceService.createInstance(workflowId, "anjiongyi");
        return RetBuilder.success();
    }

    @GetMapping("wf/completeTask/{taskId}/{operationType}")
    public RetResult<Object> approval(@PathVariable String taskId, @PathVariable Byte operationType) {
        workflowTaskService.completeTask(taskId, "anjiongyi", operationType, "test comment");
        return RetBuilder.success();
    }

    @GetMapping("wf/createInstance")
    public void createInstance() {
    }

    @GetMapping("shutdown")
    public void shutdown() {
        ApplicationContext context = SpringUtil.getApplicationContext();
        new Thread(() -> SpringApplication.exit(context)).start();
    }

}
