package cn.projectan.strix.model.wechat.payment;

import lombok.Data;

/**
 * @author ProjectAn
 * @date 2021/8/24 19:10
 */
@Data
public class WxPayConfig extends BasePaymentConfig {
    /**
     * 公众号AppId
     */
    private String v3AppId;

    /**
     * 收款商户Id
     */
    private String mchId;

    /**
     * ApiV3秘钥
     */
    private String v3ApiKey;

    /**
     * ApiV2秘钥
     */
    private String v2ApiKey;

    /**
     * 回调Url地址
     */
    private String callbackUrl;

    /**
     * 秘钥文件
     */
    private String v3KeyPath;

    /**
     * CA证书文件
     */
    private String v3CertPath;

    /**
     * CA P12证书文件
     */
    private String v3CertP12Path;

    /**
     * 平台证书文件
     */
    private String v3PlatformCertPath;

    /**
     * 商户证书序列号
     */
    private String serialNumber;

    /**
     * 平台证书序列号
     */
    private String platformSerialNumber;

    @Data
    public static class WxPayConfigFull{

        /**
         * 公众号AppId
         */
        private String v3AppId;

        /**
         * 收款商户Id
         */
        private String mchId;

        /**
         * ApiV3秘钥
         */
        private String v3ApiKey;

        /**
         * ApiV2秘钥
         */
        private String v2ApiKey;

        /**
         * 回调Url地址
         */
        private String callbackUrl;

        /**
         * 秘钥文件 (开发环境)
         */
        private String v3KeyPathDev;

        /**
         * 秘钥文件 (线上环境)
         */
        private String v3KeyPathProd;

        /**
         * CA证书文件 (开发环境)
         */
        private String v3CertPathDev;

        /**
         * CA证书文件 (线上环境)
         */
        private String v3CertPathProd;

        /**
         * CA P12证书文件 (开发环境)
         */
        private String v3CertP12PathDev;

        /**
         * CA P12 证书文件 (线上环境)
         */
        private String v3CertP12PathProd;

        /**
         * 平台证书文件 (开发环境)
         */
        private String v3PlatformCertPathDev;

        /**
         * 平台证书文件 (线上环境)
         */
        private String v3PlatformCertPathProd;
    }
}
