package cn.projectan.strix.aot;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

import java.security.Security;

/**
 * 处理 Native 构建时 BouncyCastle 加载问题
 *
 * @author ProjectAn
 * @date 2024/3/25 2:37
 */
public class BouncyCastleFeature implements Feature {

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        RuntimeClassInitialization.initializeAtBuildTime("org.bouncycastle");
        Security.addProvider(new BouncyCastleProvider());
    }

}
