package cn.projectan.strix.utils.wechat.payment;

import cn.projectan.strix.core.payment.wxpay.WxPayTools;

/**
 * 微信支付v3接口 敏感消息字符串
 *
 * @author 安炯奕
 * @date 2022/7/22 17:34
 */
public class SensitiveStringHandler {

    private final String platformCertPath;

    public SensitiveStringHandler(String platformCertPath) {
        this.platformCertPath = platformCertPath;
    }

    public String handle(String str) {
        return WxPayTools.encryptSensitiveString(str, platformCertPath);
    }

}
