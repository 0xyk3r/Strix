package cn.projectan.strix.controller.pay;

import cn.projectan.strix.controller.BaseController;
import cn.projectan.strix.core.module.pay.StrixPayClient;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.other.system.module.pay.BasePayResult;
import cn.projectan.strix.service.system.PayConfigService;
import cn.projectan.strix.service.system.PayOrderService;
import cn.projectan.strix.util.common.I18nUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 支付回调
 *
 * @author ProjectAn
 * @since 2024/4/10 下午4:12
 */
@Slf4j
@Anonymous
@IgnoreEncryption
@RestController
@RequestMapping("pay/{configId}")
@ConditionalOnProperty(prefix = "strix.module", name = "pay", havingValue = "true")
@RequiredArgsConstructor
@Tag(name = "支付 - 回调通知")
public class PayNotifyController extends BaseController {

    private final PayOrderService payOrderService;
    private final PayConfigService payConfigService;

    /**
     * 支付回调
     */
    @Operation(summary = "支付结果通知回调")
    @RequestMapping("notify")
    public void payNotify(@Parameter(description = "支付配置 ID") @PathVariable String configId, HttpServletRequest request, HttpServletResponse response) {
        StrixPayClient client = payConfigService.getInstance(configId);

        boolean verified = client.verifyNotify(request);
        try {
            Assert.isTrue(verified, I18nUtil.get("error.pay.invalidSign"));
            BasePayResult payResult = client.resolveResult(request);
            Assert.isTrue(payResult.getSuccess(), I18nUtil.get("error.pay.badStatus"));
            Assert.hasText(payResult.getOrderId(), I18nUtil.get("error.pay.badOrderId"));

            payOrderService.handlePayResult(payResult);

            client.respondNotify(true, response);
        } catch (Exception e) {
            log.error("Bad Pay Callback", e);
            client.respondNotify(false, response);
        }
    }

}
