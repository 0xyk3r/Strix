package cn.projectan.strix.core.module.pay;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.PayOrder;
import cn.projectan.strix.model.dict.PayPlatform;
import cn.projectan.strix.model.other.module.pay.BasePayResult;
import cn.projectan.strix.model.other.module.pay.wxpay.WechatPayConfig;
import cn.projectan.strix.model.other.module.pay.wxpay.WechatPayPayParam;
import cn.projectan.strix.utils.CertUtil;
import cn.projectan.strix.utils.ServletUtils;
import cn.projectan.strix.utils.SpringUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijpay.core.IJPayHttpResponse;
import com.ijpay.core.enums.AuthTypeEnum;
import com.ijpay.core.enums.RequestMethodEnum;
import com.ijpay.core.kit.AesUtil;
import com.ijpay.core.kit.HttpKit;
import com.ijpay.core.kit.PayKit;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.wxpay.WxPayApi;
import com.ijpay.wxpay.enums.WxDomainEnum;
import com.ijpay.wxpay.enums.v3.BasePayApiEnum;
import com.ijpay.wxpay.enums.v3.OtherApiEnum;
import com.ijpay.wxpay.model.v3.Amount;
import com.ijpay.wxpay.model.v3.Payer;
import com.ijpay.wxpay.model.v3.UnifiedOrderModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信 Pay 客户端
 *
 * @author ProjectAn
 * @since 2024/4/2 17:46
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
            WechatPayPayParam pd = objectMapper.readValue(payOrder.getParams(), WechatPayPayParam.class);
            String expireTime = payOrder.getExpireTime().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

            UnifiedOrderModel unifiedOrderModel = new UnifiedOrderModel()
                    .setAppid(config.getV3AppId())
                    .setMchid(config.getMchId())
                    .setDescription(payOrder.getTitle())
                    .setOut_trade_no(payOrder.getId())
                    .setTime_expire(expireTime)
                    .setAttach(payOrder.getAttach())
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
            String expireTime = payOrder.getExpireTime().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
            UnifiedOrderModel unifiedOrderModel = new UnifiedOrderModel()
                    .setAppid(config.getV3AppId())
                    .setMchid(config.getMchId())
                    .setDescription(payOrder.getTitle())
                    .setOut_trade_no(payOrder.getId())
                    .setTime_expire(expireTime)
                    .setAttach(payOrder.getAttach())
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

    @Override
    public boolean verifyNotify(HttpServletRequest request) {
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String serialNo = request.getHeader("Wechatpay-Serial");
        String signature = request.getHeader("Wechatpay-Signature");
        String signatureType = request.getHeader("Wechatpay-Signature-Type");
        try {
            String result = HttpKit.readData(request);
            InputStream certInputStream = PayKit.getCertFileInputStream(config.getV3CertPath());
            return WxPayKit.verifySignature(signatureType, signature, result, nonce, timestamp, certInputStream);
        } catch (Exception e) {
            log.error("微信支付回调验证失败", e);
            return false;
        }
    }

    @Override
    public BasePayResult resolveResult(HttpServletRequest request) {
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String serialNo = request.getHeader("Wechatpay-Serial");
        String signature = request.getHeader("Wechatpay-Signature");
        String body = HttpKit.readData(request);
        try {
            String payResult = WxPayKit.verifyNotify(serialNo, body, signature, nonce, timestamp,
                    config.getV3ApiKey(), config.getV3PlatformCertPath());
            Assert.hasText(payResult, "微信支付回调异常: 解密数据失败: " + payResult);
            Map<String, Object> params = objectMapper.readValue(payResult, new TypeReference<>() {
            });

            BasePayResult result = new BasePayResult();
            result.setSuccess("SUCCESS".equals(MapUtil.getStr(params, "trade_state")));
            result.setOrderId(MapUtil.getStr(params, "out_trade_no"));
            result.setPlatformOrderNo(MapUtil.getStr(params, "transaction_id"));
            Map<String, Object> amountMap = MapUtil.get(params, "amount", new cn.hutool.core.lang.TypeReference<>() {
            });
            // 注意: 这里使用的是 `total` 参数，而不是 `payer_total` 参数
            // 因为 `payer_total` 参数是用户实际支付的金额，在用户使用某些优惠时可能导致实际支付金额与订单金额不一致
            result.setTotalAmount(MapUtil.getInt(amountMap, "total"));
            Map<String, Object> payerMap = MapUtil.get(params, "payer", new cn.hutool.core.lang.TypeReference<>() {
            });
            result.setPlatformUserId(MapUtil.getStr(payerMap, "openid"));
            result.setAttach(MapUtil.getStr(params, "attach"));
            result.setOriginalResult(payResult);
            return result;
        } catch (Exception e) {
            log.error("微信支付回调验证失败", e);
            return null;
        }
    }

    @Override
    public void respondNotify(boolean success, HttpServletResponse response) {
        response.setStatus(success ? 200 : 500);
        ServletUtils.write(response, success ? "{\"code\":\"SUCCESS\",\"message\":\"SUCCESS\"}" : "{\"code\":\"FAIL\",\"message\":\"FAIL\"}");
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
