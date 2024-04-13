package cn.projectan.strix.controller.pay;

import cn.projectan.strix.core.module.pay.StrixPayClient;
import cn.projectan.strix.core.module.pay.StrixPayStore;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.other.module.pay.BasePayResult;
import cn.projectan.strix.service.PayOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ProjectAn
 * @date 2024/4/10 下午4:12
 */
@Slf4j
@Anonymous
@IgnoreDataEncryption
@RestController
@RequestMapping("pay/{configId}")
@ConditionalOnProperty(prefix = "strix.module", name = "pay", havingValue = "true")
@RequiredArgsConstructor
public class PayNotifyController {

    private final PayOrderService payOrderService;
    private final StrixPayStore strixPayStore;

    @RequestMapping("notify")
    public void payNotify(@PathVariable String configId, HttpServletRequest request, HttpServletResponse response) {
        StrixPayClient client = strixPayStore.getInstance(configId);

        boolean verified = client.verifyNotify(request);
        try {
            Assert.isTrue(verified, "支付回调: 验签失败");
            BasePayResult payResult = client.resolveResult(request);
            Assert.isTrue(payResult.getSuccess(), "支付回调: 支付状态非成功");
            Assert.hasText(payResult.getOrderId(), "支付回调: 订单号为空");

            payOrderService.handlePayResult(payResult);

            client.respondNotify(true, response);
        } catch (Exception e) {
            log.error("支付回调: 处理异常", e);
            client.respondNotify(false, response);
        }
    }

}
