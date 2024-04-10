package cn.projectan.strix.controller.system;

import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.dict.PayType;
import cn.projectan.strix.model.other.module.pay.wxpay.WechatPayPaymentData;
import cn.projectan.strix.model.other.security.*;
import cn.projectan.strix.service.PayOrderService;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.utils.RedisUtil;
import cn.projectan.strix.utils.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final TokenUtil tokenUtil;
    private final RedisUtil redisUtil;

    @GetMapping("pay1")
    public void test1() {
        payOrderService.createOrder("AliPaySandbox", "testorder",
                new WechatPayPaymentData("o5KMAt-6GqVzWNlAmJXMPPVY8vew"),
                "toatt", 1, PayType.WAP);

    }

    @GetMapping("pay2")
    public void test2() {
        payOrderService.createOrder("AliPaySandbox", "testorder",
                new WechatPayPaymentData("o5KMAt-6GqVzWNlAmJXMPPVY8vew"),
                "toatt", 1, PayType.WEB);
    }

    @GetMapping("getJwt")
    public void getJwt() {
        SystemManager systemManager = systemManagerService.getById("anjiongyi");

        TokenDTO info = tokenUtil.createToken(
                new SystemManagerTokenInfo(
                        UserType.SYSTEM_MANAGER,
                        systemManager.getId(),
                        systemManager.getNickname(),
                        systemManager.getStatus(),
                        systemManager.getType(),
                        systemManager.getRegionId(),
                        List.of("menuKey1", "menuKey2"),
                        List.of("permissionKey1", "permissionKey2")
                ));

        System.out.println(info.getToken());
        System.out.println(info.getRefreshToken());
    }

    @GetMapping("verifyToken")
    public void verifyToken(String token) {
        BaseTokenInfo baseTokenInfo = tokenUtil.parseToken(token);

        System.out.println(baseTokenInfo);
    }

    @GetMapping("verifyRefreshToken")
    public void verifyRefreshToken(String refreshToken) {
        BaseRefreshTokenInfo baseRefreshTokenInfo = tokenUtil.parseRefreshToken(refreshToken);

        System.out.println(baseRefreshTokenInfo);
    }

}
