package cn.projectan.strix.aot;

import cn.hutool.core.util.ClassUtil;
import cn.projectan.strix.core.ss.SystemManagerSecurityService;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.mapper.SystemLogMapper;
import cn.projectan.strix.model.properties.StrixPackageScanProperties;
import cn.projectan.strix.utils.SpringUtil;
import com.alipay.api.domain.*;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySmsSignListResponse;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySmsTemplateListResponse;
import com.aliyuncs.http.clients.ApacheHttpClient;
import com.zaxxer.hikari.HikariConfig;
import io.jsonwebtoken.impl.DefaultClaimsBuilder;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import io.jsonwebtoken.impl.DefaultJwtHeaderBuilder;
import io.jsonwebtoken.impl.DefaultJwtParserBuilder;
import io.jsonwebtoken.impl.io.StandardCompressionAlgorithms;
import io.jsonwebtoken.impl.security.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.asymmetric.RSA;
import org.bouncycastle.jcajce.provider.asymmetric.X509;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.Optional;
import java.util.Set;

/**
 * Strix AOT 配置
 *
 * @author ProjectAn
 * @date 2024/3/25 0:56
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(StrixAOTConfig.StrixRuntimeHintsRegistrar.class)
@RequiredArgsConstructor
public class StrixAOTConfig {

    static class StrixRuntimeHintsRegistrar implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // 获取 Strix Package 配置
            StrixPackageScanProperties packageProperties = SpringUtil.getBean(StrixPackageScanProperties.class);

            // HikariCP
            hints.reflection().registerType(HikariConfig.class, MemberCategory.values());

            // MP 动态数据源 非默认数据源使用的 Mapper 需要注册反射和代理
            hints.reflection().registerType(SystemLogMapper.class, MemberCategory.values());
            hints.proxies().registerJdkProxy(
                    cn.projectan.strix.mapper.SystemLogMapper.class,
                    org.springframework.aop.SpringProxy.class,
                    org.springframework.aop.framework.Advised.class,
                    org.springframework.core.DecoratingProxy.class
            );

            // ClickHouse 使用的 LZ4 库
            try {
                hints.reflection().registerType(Class.forName("net.jpountz.lz4.LZ4Compressor", true, classLoader), MemberCategory.values());
                hints.reflection().registerType(Class.forName("net.jpountz.lz4.LZ4JavaSafeCompressor", true, classLoader), MemberCategory.values());
                hints.reflection().registerType(Class.forName("net.jpountz.lz4.LZ4HCJavaSafeCompressor", true, classLoader), MemberCategory.values());
                hints.reflection().registerType(Class.forName("net.jpountz.lz4.LZ4JavaSafeFastDecompressor", true, classLoader), MemberCategory.values());
                hints.reflection().registerType(Class.forName("net.jpountz.lz4.LZ4JavaSafeSafeDecompressor", true, classLoader), MemberCategory.values());
                hints.reflection().registerType(Class.forName("net.jpountz.lz4.LZ4JavaUnsafeCompressor", true, classLoader), MemberCategory.values());
                hints.reflection().registerType(Class.forName("net.jpountz.lz4.LZ4HCJavaUnsafeCompressor", true, classLoader), MemberCategory.values());
                hints.reflection().registerType(Class.forName("net.jpountz.lz4.LZ4JavaUnsafeFastDecompressor", true, classLoader), MemberCategory.values());
                hints.reflection().registerType(Class.forName("net.jpountz.lz4.LZ4JavaUnsafeSafeDecompressor", true, classLoader), MemberCategory.values());
            } catch (Exception e) {
                log.warn("Register ClickHouse LZ4 Class Error: " + e.getMessage());
            }

            // Aliyun OSS
            hints.reflection().registerType(ApacheHttpClient.class, MemberCategory.values());
            hints.reflection().registerType(QuerySmsSignListResponse.class, MemberCategory.values());
            hints.reflection().registerType(QuerySmsTemplateListResponse.class, MemberCategory.values());

            // IJPay 微信支付SDK 请求/响应类
            Set<Class<?>> ijpayModelClazzSet = ClassUtil.scanPackage("com.ijpay.wxpay");
            log.info("Scan IJPay WxPay Model Class Count: " + ijpayModelClazzSet.size());
            ijpayModelClazzSet.forEach(clazz -> hints.reflection().registerType(clazz, MemberCategory.values()));

            // Alipay 支付宝SDK 请求/响应类
            // FIXME 这几个包下有TM两万多个个类, 扫NM, 只注册常用的
//            Set<Class<?>> alipayDomainClazzSet = ClassUtil.scanPackage("com.alipay.api", AlipayObject.class::isAssignableFrom);
//            log.info("Scan Alipay Domain Class Count: " + alipayDomainClazzSet.size());
//            alipayDomainClazzSet.forEach(clazz -> hints.reflection().registerType(clazz, MemberCategory.values()));
//            Set<Class<?>> alipayReqClazzSet = ClassUtil.scanPackage("com.alipay.api", AlipayRequest.class::isAssignableFrom);
//            log.info("Scan Alipay Request Class Count: " + alipayReqClazzSet.size());
//            alipayReqClazzSet.forEach(clazz -> hints.reflection().registerType(clazz, MemberCategory.values()));
//            Set<Class<?>> alipayRespClazzSet = ClassUtil.scanPackage("com.alipay.api", AlipayResponse.class::isAssignableFrom);
//            log.info("Scan Alipay Response Class Count: " + alipayRespClazzSet.size());
//            alipayRespClazzSet.forEach(clazz -> hints.reflection().registerType(clazz, MemberCategory.values()));
            hints.reflection().registerType(AlipayTradeCloseModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeCreateModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePayModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeRefundModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeFastpayRefundQueryModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePageRefundModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeWapPayModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePrecreateModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeCancelModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeOrderSettleModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeQueryModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeAppPayModel.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePagePayModel.class, MemberCategory.values());

            hints.reflection().registerType(AlipayTradeCloseRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeCreateRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePayRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeRefundRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeFastpayRefundQueryRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePageRefundRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeWapPayRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePrecreateRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeCancelRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeOrderSettleRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeQueryRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeAppPayRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePagePayRequest.class, MemberCategory.values());

            hints.reflection().registerType(AlipayTradeCloseResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeCreateResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePayResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeRefundResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeFastpayRefundQueryResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePageRefundResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeWapPayResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePrecreateResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeCancelResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeOrderSettleResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeQueryResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradeAppPayResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayTradePagePayResponse.class, MemberCategory.values());
            // 支付宝 OAuth
            hints.reflection().registerType(AlipaySystemOauthTokenRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipaySystemOauthTokenResponse.class, MemberCategory.values());
            hints.reflection().registerType(AlipayUserInfoShareRequest.class, MemberCategory.values());
            hints.reflection().registerType(AlipayUserInfoShareResponse.class, MemberCategory.values());

            // Spring Security
            hints.reflection().registerType(SystemManagerSecurityService.class, MemberCategory.values());
            hints.reflection().registerType(LoginSystemUser.class, MemberCategory.values());
            hints.reflection().registerType(LoginSystemManager.class, MemberCategory.values());

            // BouncyCastle 用到的 provider 都需要在此注册
            hints.reflection().registerType(BouncyCastleProvider.class, MemberCategory.values());
            hints.reflection().registerType(KeyFactorySpi.class, MemberCategory.values());
            hints.reflection().registerType(RSA.class, MemberCategory.values());
            hints.reflection().registerType(RSA.Mappings.class, MemberCategory.values());
            hints.reflection().registerType(X509.class, MemberCategory.values());
            hints.reflection().registerType(X509.Mappings.class, MemberCategory.values());

            // JWT
            hints.reflection().registerType(DefaultKeyOperationPolicyBuilder.class, MemberCategory.values());
            hints.reflection().registerType(StandardSecureDigestAlgorithms.class, MemberCategory.values());
            hints.reflection().registerType(StandardCompressionAlgorithms.class, MemberCategory.values());
            hints.reflection().registerType(StandardEncryptionAlgorithms.class, MemberCategory.values());
            hints.reflection().registerType(DefaultKeyOperationBuilder.class, MemberCategory.values());
            hints.reflection().registerType(StandardCurves.class, MemberCategory.values());
            hints.reflection().registerType(StandardHashAlgorithms.class, MemberCategory.values());
            hints.reflection().registerType(StandardKeyOperations.class, MemberCategory.values());
            hints.reflection().registerType(StandardKeyAlgorithms.class, MemberCategory.values());
            hints.reflection().registerType(DefaultJwtHeaderBuilder.class, MemberCategory.values());
            hints.reflection().registerType(DefaultClaimsBuilder.class, MemberCategory.values());
            hints.reflection().registerType(DefaultJwtBuilder.class, MemberCategory.values());
            hints.reflection().registerType(DefaultJwtParserBuilder.class, MemberCategory.values());
            hints.reflection().registerType(JwksBridge.class, MemberCategory.values());
            hints.reflection().registerType(KeysBridge.class, MemberCategory.values());
            hints.reflection().registerType(DefaultDynamicJwkBuilder.class, MemberCategory.values());
            hints.reflection().registerType(DefaultJwkParserBuilder.class, MemberCategory.values());
            hints.reflection().registerType(DefaultJwkSetBuilder.class, MemberCategory.values());
            hints.reflection().registerType(DefaultJwkSetParserBuilder.class, MemberCategory.values());

            // ALL MODEL 扫描
            Set<Class<?>> modelClazzSet = ClassUtil.scanPackage("cn.projectan.strix.model");
            Optional.ofNullable(packageProperties.getModel()).ifPresent(packages -> {
                for (String packageName : packages) {
                    Set<Class<?>> clazzSet = ClassUtil.scanPackage(packageName);
                    modelClazzSet.addAll(clazzSet);
                }
            });
            log.info("Scan Model Class Count: " + modelClazzSet.size());
            modelClazzSet.forEach(clazz -> hints.reflection().registerType(clazz, MemberCategory.values()));

        }
    }

}
