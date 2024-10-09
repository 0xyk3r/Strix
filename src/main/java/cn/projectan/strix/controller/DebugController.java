package cn.projectan.strix.controller;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.dict.PayType;
import cn.projectan.strix.service.PayOrderService;
import cn.projectan.strix.service.WorkflowInstanceService;
import cn.projectan.strix.util.WorkflowUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    private final WorkflowUtil workflowUtil;

    private final PayOrderService payOrderService;

    @GetMapping("wf/test/{workflowId}")
    public RetResult<Object> test0(@PathVariable String workflowId) {
        workflowInstanceService.createInstance(workflowId, "test");
        return RetBuilder.success();
    }

    @GetMapping("wf/createInstance")
    public void createInstance() {
    }

    @GetMapping("pay1")
    public void test1() {
        payOrderService.createOrder("AliPaySandbox", "testorder", null, "toatt", 1, 1, "test", PayType.WAP);
    }

    @GetMapping("pay2")
    public void test2() {
        payOrderService.createOrder("AliPaySandbox", "testorder", null, "toatt", 1, 1, "test", PayType.WEB);
    }

}
