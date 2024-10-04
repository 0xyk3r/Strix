package cn.projectan.strix.controller;

import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.dict.PayType;
import cn.projectan.strix.service.PayOrderService;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.utils.PopularityUtil;
import cn.projectan.strix.utils.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final SystemUserService systemUserService;
    private final ObjectMapper objectMapper;
    private final PayOrderService payOrderService;
    private final RedisUtil redisUtil;
    private final PopularityUtil popularityUtil;

    @GetMapping("test")
    public void test0() {
        systemUserService.bindThirdUser("1", 1, "test");
    }

    @GetMapping("pay1")
    public void test1() {
        payOrderService.createOrder("AliPaySandbox", "testorder", null, "toatt", 1, 1, "test", PayType.WAP);
    }

    @GetMapping("pay2")
    public void test2() {
        payOrderService.createOrder("AliPaySandbox", "testorder", null, "toatt", 1, 1, "test", PayType.WEB);
    }

    @GetMapping("popularity/incr/{configKey}/{dataId}")
    public void incrPopularity(@PathVariable String configKey, @PathVariable String dataId) {
        popularityUtil.incr(configKey, dataId);
    }

    @GetMapping("popularity/get/{configKey}/{dataId}")
    public Long getPopularity(@PathVariable String configKey, @PathVariable String dataId) {
        return popularityUtil.get(configKey, dataId);
    }

    @GetMapping("popularity/save")
    public void savePopularity() {
        popularityUtil.syncToDB();
    }

}
