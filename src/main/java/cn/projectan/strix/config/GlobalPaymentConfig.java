package cn.projectan.strix.config;

import cn.projectan.strix.core.module.payment.wxpay.WxPayTools;
import cn.projectan.strix.model.db.PaymentConfig;
import cn.projectan.strix.model.wechat.payment.BasePaymentConfig;
import cn.projectan.strix.model.wechat.payment.WxPayConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 安炯奕
 * @date 2021/8/24 13:00
 */
@Slf4j
@Component
public class GlobalPaymentConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.profiles.active}")
    private String env;
    @Value("${spring.application.name}")
    private String applicationName;

    private final Map<String, BasePaymentConfig> paymentConfigInstanceMap = new HashMap<>();

    public void addInstance(String id, PaymentConfig paymentConfig) {
        // TODO 增加其他平台的处理 并改为枚举
        if (paymentConfig.getPlatform() == 1) {
            String configData = paymentConfig.getConfigData();
            if (StringUtils.hasText(configData)) {
                try {
                    WxPayConfig.WxPayConfigFull config = objectMapper.readValue(configData, new TypeReference<>() {
                    });
                    WxPayConfig wxPayConfig = new WxPayConfig();
                    wxPayConfig.setId(paymentConfig.getId());
                    wxPayConfig.setName(paymentConfig.getName());
                    wxPayConfig.setPlatform(paymentConfig.getPlatform());
                    wxPayConfig.setV3AppId(config.getV3AppId());
                    wxPayConfig.setMchId(config.getMchId());
                    wxPayConfig.setV3ApiKey(config.getV3ApiKey());
                    wxPayConfig.setV2ApiKey(config.getV2ApiKey());
                    wxPayConfig.setCallbackUrl(config.getCallbackUrl());
                    if ("prod".equals(env)) {
                        wxPayConfig.setV3KeyPath(config.getV3KeyPathProd());
                        wxPayConfig.setV3CertPath(config.getV3CertPathProd());
                        wxPayConfig.setV3CertP12Path(config.getV3CertP12PathProd());
                        wxPayConfig.setV3PlatformCertPath(config.getV3PlatformCertPathProd());
                    } else {
                        wxPayConfig.setV3KeyPath(config.getV3KeyPathDev());
                        wxPayConfig.setV3CertPath(config.getV3CertPathDev());
                        wxPayConfig.setV3CertP12Path(config.getV3CertP12PathDev());
                        wxPayConfig.setV3PlatformCertPath(config.getV3PlatformCertPathDev());
                    }
                    if (!"Strix".equalsIgnoreCase(applicationName)) {
                        // 框架程序不执行该代码 因为没有相关证书
                        wxPayConfig.setSerialNumber(WxPayTools.getCertSerialNumber(wxPayConfig.getV3CertPath()));
                        wxPayConfig.setPlatformSerialNumber(WxPayTools.getCertSerialNumber(wxPayConfig.getV3PlatformCertPath()));
                    }
                    paymentConfigInstanceMap.put(id, wxPayConfig);
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public BasePaymentConfig getInstance(String id) {
        return paymentConfigInstanceMap.get(id);
    }

    public static void main(String[] args) throws JsonProcessingException {
        WxPayConfig.WxPayConfigFull config = new WxPayConfig.WxPayConfigFull();
        config.setV3AppId("wxb8b2a1abb8fa74b4");
        config.setMchId("1628824886");
        config.setV3ApiKey("98aI8gaAOoha3gZ0paG399ChpaaaAA13");
        config.setV2ApiKey("");
        config.setCallbackUrl("https://sd.huiboche.cn/api/payment/wxpay/notify");
        config.setV3KeyPathDev("payment/wxpay/apiclient_key.pem");
        config.setV3KeyPathProd("/mnt/WxPayPartner/payment/wxpay/apiclient_key.pem");
        config.setV3CertPathDev("payment/wxpay/apiclient_cert.pem");
        config.setV3CertPathProd("/mnt/WxPayPartner/payment/wxpay/apiclient_cert.pem");
        config.setV3CertP12PathDev("payment/wxpay/apiclient_cert.p12");
        config.setV3CertP12PathProd("/mnt/WxPayPartner/payment/wxpay/apiclient_cert.p12");
        config.setV3PlatformCertPathDev("payment/wxpay/v3_cert.pem");
        config.setV3PlatformCertPathProd("/mnt/WxPayPartner/payment/wxpay/v3_cert.pem");
        System.out.println(new ObjectMapper().writeValueAsString(config));
    }

}
