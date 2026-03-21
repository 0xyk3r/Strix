package cn.projectan.strix.util.crypto;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.SM2;

/**
 * 国密算法密钥对生成工具
 * <p>
 * 直接运行此类可生成 SM2 密钥对，用于替换 ApiSecurity 中的密钥常量。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
public class CryptoKeyGenerator {

    public static void main(String[] args) {
        System.out.println("=== SM2 密钥对生成器 ===\n");

        System.out.println("--- 服务端密钥对 (Server Key Pair) ---");
        SM2 serverSm2 = SmUtil.sm2();
        System.out.println("SERVER_SM2_PRIVATE_KEY = \"" + serverSm2.getPrivateKeyBase64() + "\";");
        System.out.println("SERVER_SM2_PUBLIC_KEY  = \"" + serverSm2.getPublicKeyBase64() + "\";");

        System.out.println("\n--- 客户端密钥对 (Client Key Pair) ---");
        SM2 clientSm2 = SmUtil.sm2();
        System.out.println("CLIENT_SM2_PRIVATE_KEY = \"" + clientSm2.getPrivateKeyBase64() + "\";");
        System.out.println("CLIENT_SM2_PUBLIC_KEY  = \"" + clientSm2.getPublicKeyBase64() + "\";");

        System.out.println("\n=== 密码哈希测试 ===");
        String testPassword = "Admin@123";
        String sm3Hash = StrixSM3Util.hashPassword(testPassword);
        System.out.println("密码 \"" + testPassword + "\" 的 SM3 哈希值: " + sm3Hash);
        System.out.println("哈希长度: " + sm3Hash.length() + " 字符 (SM3 = 64, MD5 = 32)");
        System.out.println("验证匹配: " + StrixSM3Util.matches(testPassword, sm3Hash));
    }

}
