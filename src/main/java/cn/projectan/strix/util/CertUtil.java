package cn.projectan.strix.util;

import cn.hutool.core.io.FileUtil;
import com.ijpay.core.kit.PayKit;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.security.cert.X509Certificate;

/**
 * 证书工具类
 *
 * @author ProjectAn
 * @since 2024/4/3 1:28
 */
@Slf4j
public class CertUtil {

    /**
     * 获取证书序列号
     *
     * @param certPath 证书路径
     * @return 证书序列号
     */
    public static String getCertSerialNumber(String certPath) {
        try (BufferedInputStream is = FileUtil.getInputStream(certPath)) {
            X509Certificate certificate = PayKit.getCertificate(is);
            return certificate.getSerialNumber().toString(16).toUpperCase();
        } catch (Exception e) {
            log.error("获取证书序列号时出错", e);
        }
        return null;
    }

    /**
     * 获取证书内容
     *
     * @param certPath 证书路径
     * @return 证书内容
     */
    public static String getCertContent(String certPath) {
        try {
            return PayKit.getCertFileContent(certPath);
        } catch (Exception e) {
            log.error("获取证书内容时出错", e);
        }
        return null;
    }

}
