package cn.projectan.strix.util.module.oauth;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.core.exception.StrixOAuthException;
import cn.projectan.strix.util.http.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 微信公众号 OAuth 工具类
 *
 * @author ProjectAn
 * @since 2024/4/4 2:27
 */
@Slf4j
public class WechatOAOAuthUtil {

    private static final String SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random RANDOM = new SecureRandom();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String JS_API_TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=%s&type=jsapi";

    /**
     * 获取JS_API_TICKET
     *
     * @param accessToken 全局AccessToken
     * @return JsApiTicket
     * @throws StrixOAuthException 获取失败时抛出异常
     */
    public static String getJsApiTicket(String accessToken) {
        String url = String.format(JS_API_TICKET_URL, accessToken);
        try {
            String responseStr = OkHttpUtil.get(url);
            Assert.hasText(responseStr, "Strix OAuth: 获取微信 JsApiTicket 时远程服务器返回数据为空.");

            Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseStr, new TypeReference<>() {
            });
            String ticket = MapUtil.getStr(responseMap, "ticket");
            Assert.hasText(ticket, "Strix OAuth: 获取微信 JsApiTicket 时远程服务器返回数据异常.");

            log.debug("Strix OAuth: 获取微信 JsApiTicket 成功.");
            return ticket;
        } catch (Exception e) {
            log.error("Strix OAuth: 获取微信 JsApiTicket 失败", e);
            throw new StrixOAuthException("Strix OAuth: 获取微信 JsApiTicket 失败", e);
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
     * @throws StrixOAuthException 签名失败时抛出异常
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
     * @throws StrixOAuthException 签名失败时抛出异常
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
                String value = data.get(s);
                if (value != null && !value.trim().isEmpty()) {
                    sb.append(s).append("=").append(value.trim()).append("&");
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
            log.error("Strix OAuth: 生成JsAPI签名失败, data: {}", data, e);
            throw new StrixOAuthException("Strix OAuth: 生成JsAPI签名失败", e);
        }
    }

}
