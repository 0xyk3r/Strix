package cn.projectan.strix.util.crypto;

import cn.hutool.crypto.SmUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * SM3 国密哈希工具类
 * <p>
 * 用于替代 MD5 进行密码哈希等操作
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
public final class StrixSM3Util {

    private static final String PASSWORD_SALT = "ProjectAn Strix";

    private StrixSM3Util() {
    }

    /**
     * 使用 SM3 对密码进行哈希
     *
     * @param password 明文密码
     * @return SM3 哈希值（hex 格式）
     */
    public static String hashPassword(String password) {
        return SmUtil.sm3(password + PASSWORD_SALT);
    }

    /**
     * 校验密码是否匹配（常量时间比较，防止时序攻击）
     *
     * @param rawPassword    明文密码
     * @param hashedPassword 数据库中存储的哈希值
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        String computed = hashPassword(rawPassword);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                hashedPassword.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 检查存储的密码是否为旧版 MD5 格式（32 位 hex）
     * SM3 产出 64 位 hex，MD5 产出 32 位 hex
     */
    public static boolean isLegacyMd5Hash(String hashedPassword) {
        return hashedPassword != null && hashedPassword.length() == 32;
    }

}
