package cn.projectan.strix.core.payment.wxpay;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.config.GlobalPaymentConfig;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.wechat.payment.WxPayConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijpay.core.IJPayHttpResponse;
import com.ijpay.core.enums.RequestMethod;
import com.ijpay.core.kit.AesUtil;
import com.ijpay.core.kit.PayKit;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.wxpay.WxPayApi;
import com.ijpay.wxpay.enums.WxApiType;
import com.ijpay.wxpay.enums.WxDomain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
 * @author 安炯奕
 * @date 2021/8/24 14:45
 */
@Slf4j
@Component
public class WxPayTools {

    @Autowired
    private GlobalPaymentConfig globalPaymentConfig;

    /**
     * 获取商户证书序列号
     */
    public static String getCertSerialNumber(String certPath) {
        X509Certificate certificate = PayKit.getCertificate(FileUtil.getInputStream(certPath));
        return certificate.getSerialNumber().toString(16).toUpperCase();
    }

    /**
     * 敏感消息加密
     */
    public static String encryptSensitiveString(String str, String platformCertPath) {
        X509Certificate certificate = PayKit.getCertificate(FileUtil.getInputStream(platformCertPath));
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
    public static String decryptSensitiveString(String encryptStr, String keyPath) {
        String decrypt = null;
        try {
            PrivateKey privateKey = PayKit.getPrivateKey(keyPath);
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
    public static String merchantUploadMedia(File file, boolean deleteAfterHandle, WxPayConfig wxPayConfig) {
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileInputStream is = new FileInputStream(file)) {
            String fileName = file.getName();
            String sha256 = DigestUtils.sha256Hex(is);
            Map<String, String> requestMap = new HashMap<>(2);
            requestMap.put("filename", fileName);
            requestMap.put("sha256", sha256);
            IJPayHttpResponse response = WxPayApi.v3(
                    WxDomain.CHINA.toString(),
                    WxApiType.MERCHANT_UPLOAD_MEDIA.getUrl(),
                    wxPayConfig.getMchId(),
                    WxPayTools.getCertSerialNumber(wxPayConfig.getV3CertPath()),
                    WxPayTools.getCertSerialNumber(wxPayConfig.getV3CertPath()),
                    wxPayConfig.getV3KeyPath(),
                    objectMapper.writeValueAsString(requestMap),
                    file
            );

            boolean verifySignature = WxPayKit.verifySignature(response, wxPayConfig.getV3PlatformCertPath());
            Assert.isTrue(verifySignature, "校验微信JSAPI支付请求响应签名失败： " + response.getBody());
            Map<String, Object> respMap = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });

            String mediaId = MapUtil.getStr(respMap, "media_id");
            Assert.hasText(mediaId, "上传图片至微信支付平台时失败");

            if (deleteAfterHandle) {
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
            WxPayConfig wxPayConfig = (WxPayConfig) globalPaymentConfig.getInstance("HuiBoChe");
            X509Certificate certificate = PayKit.getCertificate(FileUtil.getInputStream(wxPayConfig.getV3CertPath()));
            String serialNo = certificate.getSerialNumber().toString(16).toUpperCase();

            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethod.GET,
                    WxDomain.CHINA.toString(),
                    WxApiType.GET_CERTIFICATES.toString(),
                    wxPayConfig.getMchId(),
                    serialNo,
                    null,
                    wxPayConfig.getV3KeyPath(),
                    ""
            );

            Map<String, Object> result = WxPayApi.buildResMap(response);

            String serialNumber = MapUtil.getStr(result, "serialNumber");
            String body = MapUtil.getStr(result, "body");
            int status = MapUtil.getInt(result, "status");
            System.out.println("serialNumber:" + serialNumber);
            System.out.println("status:" + status);
            // 根据证书序列号查询对应的证书来验证签名结果
            boolean verifySignature = WxPayKit.verifySignature(response, wxPayConfig.getV3PlatformCertPath());
            System.out.println("verifySignature:" + verifySignature + "\nbody:" + body);

            //TODO 解析body 判断是否有证书更新 并调用savePlatformCert方法存储最新文件
        } catch (Exception e) {
            e.printStackTrace();
        }

        return RetMarker.makeSuccessRsp();
    }

    /**
     * 保存平台证书到文件
     */
    private String savePlatformCert(String associatedData, String nonce, String cipherText, String certPath) {
        WxPayConfig wxPayConfig = (WxPayConfig) globalPaymentConfig.getInstance("HuiBoChe");
        try {
            AesUtil aesUtil = new AesUtil(wxPayConfig.getV3ApiKey().getBytes(StandardCharsets.UTF_8));
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
            e.printStackTrace();
            return e.getMessage();
        }
    }

}
