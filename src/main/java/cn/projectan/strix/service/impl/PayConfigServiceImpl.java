package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.module.pay.AlipayPayClient;
import cn.projectan.strix.core.module.pay.StrixPayStore;
import cn.projectan.strix.core.module.pay.WechatPayClient;
import cn.projectan.strix.mapper.PayConfigMapper;
import cn.projectan.strix.model.db.PayConfig;
import cn.projectan.strix.model.dict.PayPlatform;
import cn.projectan.strix.model.other.module.pay.alipay.AlipayPayConfig;
import cn.projectan.strix.model.other.module.pay.wxpay.WechatPayConfig;
import cn.projectan.strix.service.PayConfigService;
import cn.projectan.strix.utils.CertUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayConfigServiceImpl extends ServiceImpl<PayConfigMapper, PayConfig> implements PayConfigService {

    private final StrixPayStore strixPayStore;
    private final ObjectMapper objectMapper;

    @Override
    public void createInstance(List<PayConfig> payConfigList) {
        for (PayConfig payConfig : payConfigList) {
            Assert.hasText(payConfig.getConfigData(), "Strix Pay: 初始化支付服务实例 <" + payConfig.getName() + "> 失败. (配置信息为空)");
            try {
                switch (payConfig.getPlatform()) {
                    case PayPlatform.WX_PAY -> {
                        WechatPayConfig wechatPayConfig = objectMapper.readValue(payConfig.getConfigData(), WechatPayConfig.class);
                        wechatPayConfig.setId(payConfig.getId());
                        wechatPayConfig.setName(payConfig.getName());
                        wechatPayConfig.setPlatform(payConfig.getPlatform());
                        // 获取证书序列号
                        wechatPayConfig.setSerialNumber(CertUtil.getCertSerialNumber(wechatPayConfig.getV3CertPath()));
                        wechatPayConfig.setPlatformSerialNumber(CertUtil.getCertSerialNumber(wechatPayConfig.getV3PlatformCertPath()));
                        strixPayStore.addInstance(payConfig.getId(), new WechatPayClient(wechatPayConfig));
                        log.info("Strix Pay: 初始化支付服务实例 <" + payConfig.getName() + "> 成功.");
                    }
                    case PayPlatform.ALI_PAY -> {
                        AlipayPayConfig alipayPayConfig = objectMapper.readValue(payConfig.getConfigData(), AlipayPayConfig.class);
                        alipayPayConfig.setId(payConfig.getId());
                        alipayPayConfig.setName(payConfig.getName());
                        alipayPayConfig.setPlatform(payConfig.getPlatform());
                        strixPayStore.addInstance(payConfig.getId(), new AlipayPayClient(alipayPayConfig));
                        log.info("Strix Pay: 初始化支付服务实例 <" + payConfig.getName() + "> 成功.");
                    }
                    default ->
                            log.error("Strix Pay: 初始化支付服务实例 <" + payConfig.getName() + "> 失败. (不支持的支付平台)");
                }
            } catch (Exception e) {
                log.error("Strix Pay: 初始化支付服务实例 <" + payConfig.getName() + "> 失败. (配置解析失败)", e);
            }
        }
    }

}
