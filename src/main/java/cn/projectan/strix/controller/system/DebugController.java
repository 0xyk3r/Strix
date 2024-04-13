package cn.projectan.strix.controller.system;

import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.dict.PayType;
import cn.projectan.strix.service.PayOrderService;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.utils.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ProjectAn
 * @date 2022/7/29 14:19
 */
@Slf4j
@Anonymous
@IgnoreDataEncryption
@RestController
@RequestMapping("debug")
@ConditionalOnProperty(prefix = "spring.profiles", name = "active", havingValue = "dev")
@RequiredArgsConstructor
public class DebugController {

    private final SystemManagerService systemManagerService;
    private final ObjectMapper objectMapper;
    private final PayOrderService payOrderService;
    private final RedisUtil redisUtil;

    @GetMapping("pay1")
    public void test1() {
        payOrderService.createOrder("AliPaySandbox", "testorder", null, "toatt", 1, 1, "test", PayType.WAP);

    }

    @GetMapping("pay2")
    public void test2() {
        payOrderService.createOrder("AliPaySandbox", "testorder", null, "toatt", 1, 1, "test", PayType.WEB);
    }

}
