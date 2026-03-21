package cn.projectan.strix.core.module.pay;

import cn.projectan.strix.model.db.system.PayConfig;
import cn.projectan.strix.model.dict.system.PayPlatform;
import cn.projectan.strix.model.other.system.module.pay.wxpay.WechatPayConfig;
import cn.projectan.strix.util.file.CertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 微信支付客户端工厂
 *
 * @author ProjectAn
 */
@Component
@RequiredArgsConstructor
public class WechatPayClientFactory implements PayClientFactory {

    private final ObjectMapper objectMapper;

    @Override
    public short supportedPlatform() {
        return PayPlatform.WX_PAY;
    }

    @Override
    public StrixPayClient createClient(PayConfig config) throws Exception {
        WechatPayConfig wechatPayConfig = objectMapper.readValue(config.getConfigData(), WechatPayConfig.class);
        wechatPayConfig.setId(config.getId());
        wechatPayConfig.setName(config.getName());
        wechatPayConfig.setPlatform(config.getPlatform());
        wechatPayConfig.setSerialNumber(CertUtil.getCertSerialNumber(wechatPayConfig.getV3CertPath()));
        wechatPayConfig.setPlatformSerialNumber(CertUtil.getCertSerialNumber(wechatPayConfig.getV3PlatformCertPath()));
        return new WechatPayClient(wechatPayConfig);
    }

}
