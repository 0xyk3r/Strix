package cn.projectan.strix.core.module.pay;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.PayOrder;
import cn.projectan.strix.model.dict.PayPlatform;
import cn.projectan.strix.model.other.module.pay.wxpay.WechatPayConfig;
import cn.projectan.strix.model.other.module.pay.wxpay.WechatPayPaymentData;
import cn.projectan.strix.utils.CertUtil;
import cn.projectan.strix.utils.SpringUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijpay.core.IJPayHttpResponse;
import com.ijpay.core.enums.AuthTypeEnum;
import com.ijpay.core.enums.RequestMethodEnum;
import com.ijpay.core.kit.AesUtil;
import com.ijpay.core.kit.PayKit;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.core.utils.DateTimeZoneUtil;
import com.ijpay.wxpay.WxPayApi;
import com.ijpay.wxpay.enums.WxDomainEnum;
import com.ijpay.wxpay.enums.v3.BasePayApiEnum;
import com.ijpay.wxpay.enums.v3.OtherApiEnum;
import com.ijpay.wxpay.model.v3.Amount;
import com.ijpay.wxpay.model.v3.Payer;
import com.ijpay.wxpay.model.v3.UnifiedOrderModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ProjectAn
 * @date 2024/4/2 17:46
 */
@Slf4j
public class WechatPayClient extends StrixPayClient {

    protected final WechatPayConfig config;
    private final ObjectMapper objectMapper;

    public WechatPayClient(WechatPayConfig config) {
        super();
        Assert.notNull(config, "Strix Pay: 初始化微信支付服务实例失败. (配置信息为空)");
        this.config = config;
        this.objectMapper = SpringUtil.getBean(ObjectMapper.class);
    }

    @Override
    public String getConfigId() {
        return config.getId();
    }

    @Override
    public String getConfigName() {
        return config.getName();
    }

    @Override
    public int getPlatform() {
        return PayPlatform.WX_PAY;
    }

    @Override
    public Map<String, String> createWapPay(PayOrder payOrder) {
        try {
            WechatPayPaymentData pd = objectMapper.readValue(payOrder.getPaymentData(), WechatPayPaymentData.class);
            String expireTime = DateTimeZoneUtil.dateToTimeZone(System.currentTimeMillis() + (1000 * 60 * 30));

            UnifiedOrderModel unifiedOrderModel = new UnifiedOrderModel()
                    .setAppid(config.getV3AppId())
                    .setMchid(config.getMchId())
                    .setDescription(payOrder.getPaymentTitle())
                    .setOut_trade_no(payOrder.getId())
                    .setTime_expire(expireTime)
                    .setAttach(payOrder.getPaymentAttach())
                    .setNotify_url(config.getCallbackUrl())
                    .setAmount(new Amount().setTotal(payOrder.getTotalAmount()))
                    .setPayer(new Payer().setOpenid(pd.getOpenId()));

            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.POST,
                    WxDomainEnum.CHINA.getDomain(),
                    BasePayApiEnum.JS_API_PAY.getUrl(),
                    config.getMchId(),
                    config.getSerialNumber(),
                    null,
                    config.getV3KeyPath(),
                    objectMapper.writeValueAsString(unifiedOrderModel)
            );

            Assert.isTrue(response.getStatus() == 200, "Strix Pay: 微信创建JSAPI订单响应异常： " + response.getBody());
            // 根据证书序列号查询对应的证书来验证签名结果
            boolean verifySignature = WxPayKit.verifySignature(response, config.getV3PlatformCertPath());
            Assert.isTrue(verifySignature, "Strix Pay: 校验微信支付响应签名失败： " + response.getBody());

            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            String prepayId = MapUtil.getStr(responseMap, "prepay_id");

            return WxPayKit.jsApiCreateSign(config.getV3AppId(), prepayId, config.getV3KeyPath());
        } catch (Exception e) {
            log.error("微信JSAPI支付信息生成时发生异常", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return null;
    }

    @Override
    public Map<String, String> createWebPay(PayOrder payOrder) {
        try {
            String expireTime = DateTimeZoneUtil.dateToTimeZone(System.currentTimeMillis() + (1000 * 60 * 30));
            UnifiedOrderModel unifiedOrderModel = new UnifiedOrderModel()
                    .setAppid(config.getV3AppId())
                    .setMchid(config.getMchId())
                    .setDescription(payOrder.getPaymentTitle())
                    .setOut_trade_no(payOrder.getId())
                    .setTime_expire(expireTime)
                    .setAttach(payOrder.getPaymentAttach())
                    .setNotify_url(config.getCallbackUrl())
                    .setAmount(new Amount().setTotal(payOrder.getTotalAmount()));

            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.POST,
                    WxDomainEnum.CHINA.toString(),
                    BasePayApiEnum.NATIVE_PAY.toString(),
                    config.getMchId(),
                    config.getSerialNumber(),
                    null,
                    config.getV3KeyPath(),
                    objectMapper.writeValueAsString(unifiedOrderModel)
            );

            Assert.isTrue(response.getStatus() == 200, "Strix Pay: 微信创建JSAPI订单响应异常： " + response.getBody());
            // 根据证书序列号查询对应的证书来验证签名结果
            boolean verifySignature = WxPayKit.verifySignature(response, config.getV3PlatformCertPath());
            Assert.isTrue(verifySignature, "Strix Pay: 校验微信支付响应签名失败： " + response.getBody());

            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            String codeUrl = MapUtil.getStr(responseMap, "code_url");

            return Map.of("codeUrl", codeUrl);
        } catch (Exception e) {
            log.error("微信NATIVE支付信息生成时发生异常", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return null;
    }

    /**
     * 敏感消息加密
     */
    public String encryptSensitiveString(String str) {
        X509Certificate certificate = PayKit.getCertificate(FileUtil.getInputStream(config.getV3PlatformCertPath()));
        String encrypt = null;
        try {
            encrypt = PayKit.rsaEncryptOAEP(str, certificate);
        } catch (Exception e) {
            log.error("敏感消息加密时出错", e);
        }
        return encrypt;
    }

    /**
     * 敏感消息解密
     */
    public String decryptSensitiveString(String encryptStr) {
        String decrypt = null;
        try {
            PrivateKey privateKey = PayKit.getPrivateKey(config.getV3KeyPath(), AuthTypeEnum.SM2.getCode());
            decrypt = PayKit.rsaDecryptOAEP(encryptStr, privateKey);
        } catch (Exception e) {
            log.error("敏感消息加密时出错", e);
        }
        return decrypt;
    }

    /**
     * 通用图片上传
     *
     * @param file 图片文件
     * @return MediaID
     */
    public String merchantUploadMedia(File file, boolean deleteAfterHandle) {
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileInputStream is = new FileInputStream(file)) {
            String fileName = file.getName();
            String sha256 = DigestUtils.sha256Hex(is);
            Map<String, String> requestMap = new HashMap<>(2);
            requestMap.put("filename", fileName);
            requestMap.put("sha256", sha256);
            IJPayHttpResponse response = WxPayApi.v3(
                    WxDomainEnum.CHINA.getDomain(),
                    OtherApiEnum.MERCHANT_UPLOAD_MEDIA.getUrl(),
                    config.getMchId(),
                    CertUtil.getCertSerialNumber(config.getV3CertPath()),
                    CertUtil.getCertSerialNumber(config.getV3CertPath()),
                    config.getV3KeyPath(),
                    objectMapper.writeValueAsString(requestMap),
                    file
            );

            boolean verifySignature = WxPayKit.verifySignature(response, config.getV3PlatformCertPath());
            Assert.isTrue(verifySignature, "校验微信JSAPI支付请求响应签名失败： " + response.getBody());
            Map<String, Object> respMap = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });

            String mediaId = MapUtil.getStr(respMap, "media_id");
            Assert.hasText(mediaId, "上传图片至微信支付平台时失败");

            if (deleteAfterHandle) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }

            return mediaId;
        } catch (Exception e) {
            log.error("微信支付上传图片API发生错误", e);
        }
        return null;
    }

    /**
     * 获取最新的平台证书信息
     */
    public RetResult<Object> getV3PlatformCert() {
        try {
            X509Certificate certificate = PayKit.getCertificate(FileUtil.getInputStream(config.getV3CertPath()));
            String serialNo = certificate.getSerialNumber().toString(16).toUpperCase();

            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.GET,
                    WxDomainEnum.CHINA.getDomain(),
                    OtherApiEnum.GET_CERTIFICATES.getUrl(),
                    config.getMchId(),
                    serialNo,
                    null,
                    config.getV3KeyPath(),
                    ""
            );

            Map<String, Object> result = WxPayApi.buildResMap(response);

            String serialNumber = MapUtil.getStr(result, "serialNumber");
            String body = MapUtil.getStr(result, "body");
            int status = MapUtil.getInt(result, "status");
            System.out.println("serialNumber:" + serialNumber);
            System.out.println("status:" + status);
            // 根据证书序列号查询对应的证书来验证签名结果
            boolean verifySignature = WxPayKit.verifySignature(response, config.getV3PlatformCertPath());
            System.out.println("verifySignature:" + verifySignature + "\nbody:" + body);

            //TODO 解析body 判断是否有证书更新 并调用savePlatformCert方法存储最新文件
        } catch (Exception e) {
            log.error("获取微信支付平台证书时发生异常", e);
        }

        return RetBuilder.success();
    }

    /**
     * 保存平台证书到文件
     */
    private String savePlatformCert(String associatedData, String nonce, String cipherText, String certPath) {
        try {
            AesUtil aesUtil = new AesUtil(config.getV3ApiKey().getBytes(StandardCharsets.UTF_8));
            // 平台证书密文解密
            // encrypt_certificate 中的  associated_data nonce  ciphertext
            String publicKey = aesUtil.decryptToString(
                    associatedData.getBytes(StandardCharsets.UTF_8),
                    nonce.getBytes(StandardCharsets.UTF_8),
                    cipherText
            );
            // 保存证书
            FileWriter writer = new FileWriter(certPath);
            writer.write(publicKey);
            // 获取平台证书序列号
            X509Certificate certificate = PayKit.getCertificate(new ByteArrayInputStream(publicKey.getBytes()));
            return certificate.getSerialNumber().toString(16).toUpperCase();
        } catch (Exception e) {
            log.error("保存微信支付平台证书时发生异常", e);
        }
        return null;
    }

}
