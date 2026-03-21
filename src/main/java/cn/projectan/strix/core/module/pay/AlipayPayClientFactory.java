package cn.projectan.strix.core.module.pay;

import cn.projectan.strix.model.db.system.PayConfig;
import cn.projectan.strix.model.dict.system.PayPlatform;
import cn.projectan.strix.model.other.system.module.pay.alipay.AlipayPayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 支付宝客户端工厂
 *
 * @author ProjectAn
 */
@Component
@RequiredArgsConstructor
public class AlipayPayClientFactory implements PayClientFactory {

    private final ObjectMapper objectMapper;

    @Override
    public short supportedPlatform() {
        return PayPlatform.ALI_PAY;
    }

    @Override
    public StrixPayClient createClient(PayConfig config) throws Exception {
        AlipayPayConfig alipayPayConfig = objectMapper.readValue(config.getConfigData(), AlipayPayConfig.class);
        alipayPayConfig.setId(config.getId());
        alipayPayConfig.setName(config.getName());
        alipayPayConfig.setPlatform(config.getPlatform());
        return new AlipayPayClient(alipayPayConfig);
    }

}
