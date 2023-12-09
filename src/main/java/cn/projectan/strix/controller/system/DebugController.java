package cn.projectan.strix.controller.system;

import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.utils.WorkflowUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 安炯奕
 * @date 2022/7/29 14:19
 */
@Slf4j
@Anonymous
@RestController
@RequestMapping("debug")
@ConditionalOnProperty(prefix = "spring.profiles", name = "active", havingValue = "dev")
@RequiredArgsConstructor
public class DebugController {

    private final ObjectMapper objectMapper;
    private final WorkflowUtil workflowUtil;

    @IgnoreDataEncryption
    @GetMapping("create/{configId}")
    public RetResult<Object> test(@PathVariable String configId,
                                  HttpServletRequest request) {
        workflowUtil.createInstance(configId);
        return RetMarker.makeSuccessRsp();
    }

    @IgnoreDataEncryption
    @GetMapping("next/{instanceId}/{nextType}")
    public RetResult<Object> test(@PathVariable String instanceId,
                                  @PathVariable String nextType,
                                  HttpServletRequest request) {
        workflowUtil.nextStep(instanceId, Byte.valueOf(nextType));
        return RetMarker.makeSuccessRsp();
    }

}
