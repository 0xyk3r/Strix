package cn.projectan.strix.job;

import cn.projectan.strix.model.annotation.StrixJob;
import cn.projectan.strix.model.db.SystemConfig;
import cn.projectan.strix.service.SystemConfigService;
import cn.projectan.strix.utils.SecurityUtils;
import cn.projectan.strix.utils.WorkflowUtil;
import cn.projectan.strix.utils.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author 安炯奕
 * @date 2023/7/30 16:51
 */
@Slf4j
@StrixJob
@Component("strixTestJob")
@RequiredArgsConstructor
public class StrixTestJob {

    private final SystemConfigService systemConfigService;
    private final WorkflowUtil workflowUtil;

    public void testSomething() {
        log.info("Do job: `StrixTestJob.testSomething`");
    }

    public void testParams(String str, Integer i, Double d, Long l, Boolean b) {
        log.info("Do job: `StrixTestJob.testParams`, your params is: " + str + ", " + i + ", " + d + ", " + l + ", " + b);
    }

    public void testUpdateDB(String str) {
        log.info("Do job: `StrixTestJob.testUpdateDB`, your params is: " + str);
        SystemConfig testConfig = systemConfigService.getById("DevTest");
        testConfig.setValue(str);
        systemConfigService.updateById(testConfig);
    }

    public boolean testCheckSystemManager() {
        log.info("Do job: `StrixTestJob.testCheckSystemManager` " + SecurityUtils.getManagerName());
        return SecurityUtils.getManagerName().equals("anjiongyi");
    }

    public void testSetParam() {
        Object workflowInstanceId = ContextHolder.get("STRIX_WORKFLOW_INSTANCE_ID");
        log.info("Do job: `StrixTestJob.testSetParam`" + workflowInstanceId);
        workflowUtil.setParam(workflowInstanceId.toString(), "testParam", "testValue");
    }

    public void testGetParam() {
        Object workflowInstanceId = ContextHolder.get("STRIX_WORKFLOW_INSTANCE_ID");
        String testParam = workflowUtil.getParam(workflowInstanceId.toString(), "testParam");
        log.info("Do job: `StrixTestJob.testGetParam` " + testParam);
    }

}
