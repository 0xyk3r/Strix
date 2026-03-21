package cn.projectan.strix.util.ip;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;

/**
 * IP 地址相关工具类
 *
 * @author ProjectAn
 */
public final class IpUtils {

    private IpUtils() {
    }

    private static final String UNKNOWN = "unknown";
    private static final String IPV6_LOOPBACK = "0:0:0:0:0:0:0:1";
    private static final String IPV4_LOOPBACK = "127.0.0.1";

    private static final String[] PROXY_HEADERS = {
            "x-forwarded-for", "Proxy-Client-IP", "X-Forwarded-For", "WL-Proxy-Client-IP", "X-Real-IP"
    };

    /**
     * 获取客户端IP
     *
     * @param request request对象
     * @return IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = Stream.of(PROXY_HEADERS)
                .map(request::getHeader)
                .filter(StringUtils::hasText)
                .filter(s -> !UNKNOWN.equalsIgnoreCase(s))
                .findFirst()
                .orElse(request.getRemoteAddr());
        return IPV6_LOOPBACK.equals(ip) ? IPV4_LOOPBACK : getMultistageReverseProxyIp(ip);
    }

    /**
     * 检查是否为内部IP地址
     *
     * @param ip IP地址
     * @return 结果
     */
    public static boolean internalIp(String ip) {
        if (IPV4_LOOPBACK.equals(ip)) {
            return true;
        }
        byte[] addr = textToNumericFormatV4(ip);
        return internalIp(addr);
    }

    /**
     * 检查是否为内部IP地址
     * <ul>
     *   <li>10.0.0.0/8</li>
     *   <li>172.16.0.0/12</li>
     *   <li>192.168.0.0/16</li>
     * </ul>
     *
     * @param addr byte地址
     * @return 结果
     */
    private static boolean internalIp(byte[] addr) {
        if (addr == null || addr.length < 2) {
            return true;
        }
        int b0 = addr[0] & 0xFF;
        int b1 = addr[1] & 0xFF;

        // 10.x.x.x/8
        if (b0 == 10) {
            return true;
        }
        // 172.16.0.0 ~ 172.31.255.255 (172.16.0.0/12)
        if (b0 == 172 && b1 >= 16 && b1 <= 31) {
            return true;
        }
        // 192.168.x.x/16
        return b0 == 192 && b1 == 168;
    }

    /**
     * 将IPv4地址转换成字节
     *
     * @param text IPv4地址
     * @return byte 字节
     */
    public static byte[] textToNumericFormatV4(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String[] elements = text.split("\\.", -1);
        if (elements.length != 4) {
            return null;
        }

        byte[] bytes = new byte[4];
        try {
            for (int i = 0; i < 4; i++) {
                int val = Integer.parseInt(elements[i]);
                if (val < 0 || val > 255) {
                    return null;
                }
                bytes[i] = (byte) val;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return bytes;
    }

    /**
     * 获取IP地址
     *
     * @return 本地IP地址
     */
    public static String getHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
        }
        return IPV4_LOOPBACK;
    }

    /**
     * 获取主机名
     *
     * @return 本地主机名
     */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
        }
        return "未知";
    }

    /**
     * 从多级反向代理中获得最后一个非 unknown 的 IP 地址
     * <p>
     * 取最后一个可同时兼容 Nginx 覆盖模式和追加模式：
     * <ul>
     *   <li>覆盖模式：仅一个 IP，直接返回</li>
     *   <li>追加模式：客户端伪造的 IP 在前，真实 IP 被 Nginx 追加在最后</li>
     * </ul>
     *
     * @param ip 获得的IP地址（可能包含逗号分隔的多个IP）
     * @return 最后一个非 unknown 的 IP 地址
     */
    public static String getMultistageReverseProxyIp(String ip) {
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.trim().split(",");
            for (int i = ips.length - 1; i >= 0; i--) {
                String candidate = ips[i].trim();
                if (!isUnknown(candidate)) {
                    return candidate;
                }
            }
        }
        return ip;
    }

    /**
     * 检测给定字符串是否为未知，多用于检测HTTP请求相关
     *
     * @param checkString 被检测的字符串
     * @return 是否未知
     */
    public static boolean isUnknown(String checkString) {
        return !StringUtils.hasText(checkString) || UNKNOWN.equalsIgnoreCase(checkString);
    }

}
