package cn.projectan.strix.util.file;

import cn.hutool.core.util.StrUtil;
import cn.projectan.strix.util.common.I18nUtil;
import com.ijpay.core.kit.PayKit;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.Security;
import java.security.cert.*;

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
        X509Certificate certificate = getCertificate(certPath);
        if (certificate == null) {
            log.warn("获取证书序列号时出错，证书为空，certPath={}", certPath);
            return null;
        }
        return certificate.getSerialNumber().toString(16).toUpperCase();
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

    /**
     * 获取证书
     *
     * @param inputStream 证书文件
     * @return {@link X509Certificate} 获取证书
     */
    public static X509Certificate getCertificate(InputStream inputStream) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            CertificateFactory cf = CertificateFactory.getInstance("X.509", new BouncyCastleProvider());
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inputStream);
            cert.checkValidity();
            return cert;
        } catch (CertificateExpiredException e) {
            throw new RuntimeException(I18nUtil.get("error.cert.expired"), e);
        } catch (CertificateNotYetValidException e) {
            throw new RuntimeException(I18nUtil.get("error.cert.notYetValid"), e);
        } catch (CertificateException e) {
            throw new RuntimeException(I18nUtil.get("error.cert.invalid"), e);
        }
    }

    /**
     * 获取证书
     *
     * @param path 证书路径，支持相对路径以及绝得路径
     * @return {@link X509Certificate} 获取证书
     */
    public static X509Certificate getCertificate(String path) {
        if (StrUtil.isEmpty(path)) {
            return null;
        }
        try (InputStream inputStream = FileUtil.open(Path.of(path))) {
            return getCertificate(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(I18nUtil.get("error.cert.pathError"), e);
        }
    }

}
