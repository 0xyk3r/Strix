package cn.projectan.strix.model.other.module.pay.alipay;

import cn.projectan.strix.model.other.module.pay.BasePaymentConfig;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2024/4/3 2:12
 */
@Data
public class AlipayPayConfig extends BasePaymentConfig {

    /**
     * 支付宝AppId
     */
    private String appId;

    /**
     * 应用私钥
     */
    private String privateKey;

    /**
     * 应用公钥
     */
    private String publicKey;

    /**
     * 应用证书路径
     */
    private String appCertPath;

    /**
     * 支付宝证书路径
     */
    private String aliPayCertPath;

    /**
     * 支付宝根证书路径
     */
    private String aliPayRootCertPath;

    /**
     * 支付宝支付网关
     */
    private String serverUrl;

    /**
     * 项目域名
     */
    private String domain;

    /**
     * 同步通知URL
     */
    private String returnUrl;

    /**
     * 异步通知URL
     */
    private String notifyUrl;

}
