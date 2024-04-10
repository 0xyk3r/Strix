package cn.projectan.strix.model.other.module.oauth;

import lombok.Data;

/**
 * @author ProjectAn
 * @date 2024/4/3 16:52
 */
@Data
public class AlipayOAuthConfig extends BaseOAuthConfig {

    /**
     * AppId
     */
    private String appId;

    /**
     * 开发者私钥，由开发者自己生成。
     */
    private String privateKey;

    /**
     * 支付宝网关（固定）。
     */
    private String serverUrl;

    /**
     * 参数返回格式，只支持 JSON（固定）。
     */
    private String format;

    /**
     * 编码集，支持 GBK/UTF-8。
     */
    private String charset;

    /**
     * 生成签名字符串所使用的签名算法类型，目前支持 RSA2 算法。
     */
    private String signType;

    /**
     * 应用证书路径
     */
    private String appCertPath;

    /**
     * 支付宝证书路径
     */
    private String alipayCertPath;

    /**
     * 支付宝根证书路径
     */
    private String alipayRootCertPath;

}
