package cn.projectan.strix.utils.captcha;

import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Strix Captcha 随机工具类
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
@Slf4j
public class StrixCaptchaRandomUtils {

    /**
     * 生成UUID
     *
     * @return UUID
     */
    public static String getUUID() {
        String uuid = UUID.randomUUID().toString();
        uuid = uuid.replace("-", "");
        return uuid;
    }

    /**
     * 获取指定文字的随机中文
     *
     * @return 随机中文
     */
    public static String getRandomHan(String hanZi) {
        return hanZi.charAt(new Random().nextInt(hanZi.length())) + "";
    }

    public static int getRandomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    /**
     * 获取随机中文
     *
     * @return 随机中文
     */
    public static String getRandomHan() {
        String str = "";
        int highCode;
        int lowCode;

        Random random = new Random();

        highCode = (176 + Math.abs(random.nextInt(39))); // B0 + 0~39(16~55) 一级汉字所占区
        lowCode = (161 + Math.abs(random.nextInt(93))); // A1 + 0~93 每区有94个汉字

        byte[] b = new byte[2];
        b[0] = (Integer.valueOf(highCode)).byteValue();
        b[1] = (Integer.valueOf(lowCode)).byteValue();

        try {
            str = new String(b, "GBK");
        } catch (UnsupportedEncodingException e) {
            log.warn("获取随机中文异常", e);
        }
        return str;
    }

    /**
     * 随机范围内数字
     *
     * @param startNum 开始数字
     * @param endNum   结束数字
     * @return 随机数字
     */
    public static Integer getRandomInt(int startNum, int endNum) {
        return ThreadLocalRandom.current().nextInt(endNum - startNum) + startNum;
    }

    /**
     * 获取随机字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

}
