package cn.projectan.strix.core.module.oauth;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.util.OkHttpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 微信 OAuth 工具类
 *
 * @author ProjectAn
 * @since 2024/4/4 2:27
 */
@Slf4j
public class WechatOAuthTools {

    private static final String SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random RANDOM = new SecureRandom();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 获取全局AccessToken
     *
     * @param appId     公众号的AppID
     * @param appSecret 公众号的AppSecret
     */
    public static String getAccessToken(String appId, String appSecret) {
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
        try {
            String responseStr = OkHttpUtil.get(url);
            Assert.hasText(responseStr, "远程服务器返回数据为空");
            Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseStr, new TypeReference<>() {
            });
            String accessToken = MapUtil.getStr(responseMap, "access_token");
            Assert.hasText(accessToken, "远程服务器返回数据异常");
            return accessToken;
        } catch (Exception e) {
            log.error("Strix OAuth: 获取微信 AccessToken 失败", e);
            return null;
        }
    }

    /**
     * 获取JS_API_TICKET
     *
     * @param accessToken 全局AccessToken
     */
    public static String getJsApiTicket(String accessToken) {
        String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + accessToken + "&type=jsapi";
        try {
            String responseStr = OkHttpUtil.get(url);
            Assert.hasText(responseStr, "远程服务器返回数据为空");
            Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseStr, new TypeReference<>() {
            });
            String ticket = MapUtil.getStr(responseMap, "ticket");
            Assert.hasText(ticket, "远程服务器返回数据异常");
            return ticket;
        } catch (Exception e) {
            log.error("Strix OAuth: 获取微信 JsApiTicket 失败", e);
            return null;
        }
    }

    /**
     * 获取当前时间戳，单位秒
     *
     * @return 当前时间戳 单位秒
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 获取随机字符串 NonceStr
     *
     * @return 随机字符串
     */
    public static String generateNonceStr() {
        char[] nonceChars = new char[32];
        for (int index = 0; index < nonceChars.length; ++index) {
            nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
        }
        return new String(nonceChars);
    }


    /**
     * 用SHA1算法验证Token
     *
     * @param token     票据
     * @param timestamp 时间戳
     * @param nonce     随机字符串
     * @return 签名
     */
    public static String signBySha1(String token, String timestamp, String nonce) {
        Map<String, String> data = Map.of("token", token, "timestamp", timestamp, "nonce", nonce);
        return signBySha1(data);
    }

    /**
     * SHA1签名
     *
     * @param data 待签名数据
     * @return 签名
     */
    public static String signBySha1(Map<String, String> data) {
        try {
            Set<String> keySet = data.keySet();
            String[] array = keySet.toArray(new String[0]);
            StringBuilder sb = new StringBuilder();
            // 字符串排序
            Arrays.sort(array);
            for (String s : array) {
                if ("sign".equals(s)) {
                    continue;
                }
                // 参数值为空，则不参与签名
                if (!data.get(s).trim().isEmpty()) {
                    sb.append(s).append("=").append(data.get(s).trim()).append("&");
                }
            }
            String sortedKvStr = sb.toString();
            if (!sb.isEmpty()) {
                sortedKvStr = sb.substring(0, sb.length() - 1);
            }
            // SHA1签名生成
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(sortedKvStr.getBytes());
            byte[] digest = md.digest();

            StringBuilder hexStr = new StringBuilder();
            String shaHex;
            for (byte b : digest) {
                shaHex = Integer.toHexString(b & 0xFF);
                if (shaHex.length() < 2) {
                    hexStr.append(0);
                }
                hexStr.append(shaHex);
            }
            return hexStr.toString();
        } catch (Exception e) {
            log.error("生成JsAPI签名失败, {}", data.toString(), e);
            return null;
        }
    }

}
