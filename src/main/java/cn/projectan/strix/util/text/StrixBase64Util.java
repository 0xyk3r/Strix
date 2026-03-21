package cn.projectan.strix.util.text;

import java.util.Base64;

/**
 * Base64 编解码工具类（带安全大小校验）
 *
 * @author ProjectAn
 * @since 2026/3/21
 */
public final class StrixBase64Util {

    /**
     * 默认最大 Base64 输入长度：1MB（适用于加密签名、IV 等短数据）
     */
    public static final int MAX_LENGTH_1MB = 1_048_576;

    /**
     * 最大 Base64 输入长度：10MB（适用于图片、验证码等中等数据）
     */
    public static final int MAX_LENGTH_10MB = 10_485_760;

    /**
     * 最大 Base64 输入长度：约 512MB（适用于文件上传等大数据）
     */
    public static final int MAX_LENGTH_512MB = 716_800_000;

    private StrixBase64Util() {
    }

    /**
     * 安全解码 Base64 字符串（使用指定最大长度限制）
     *
     * @param base64    Base64 编码字符串
     * @param maxLength 允许的最大字符长度
     * @return 解码后的字节数组
     * @throws IllegalArgumentException 如果输入为空或超出长度限制
     */
    public static byte[] decode(String base64, int maxLength) {
        if (base64 == null || base64.isEmpty()) {
            throw new IllegalArgumentException("Base64 数据不能为空");
        }
        if (base64.length() > maxLength) {
            throw new IllegalArgumentException("Base64 数据过大，最大允许 " + maxLength + " 字符，实际 " + base64.length() + " 字符");
        }
        return Base64.getDecoder().decode(base64);
    }

    /**
     * 安全解码 Base64 字符串（使用指定最大长度限制），允许 null 输入
     *
     * @param base64    Base64 编码字符串，可为 null
     * @param maxLength 允许的最大字符长度
     * @return 解码后的字节数组，输入为 null 或空时返回 null
     * @throws IllegalArgumentException 如果超出长度限制
     */
    public static byte[] decodeOrNull(String base64, int maxLength) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        if (base64.length() > maxLength) {
            throw new IllegalArgumentException("Base64 数据过大，最大允许 " + maxLength + " 字符，实际 " + base64.length() + " 字符");
        }
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Base64 编码
     *
     * @param bytes 待编码的字节数组
     * @return 编码后的 Base64 字符串
     */
    public static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

}
