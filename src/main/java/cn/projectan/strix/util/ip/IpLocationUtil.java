package cn.projectan.strix.util.ip;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.Objects;

/**
 * IP 地理位置工具类
 *
 * @author ProjectAn
 * @since 2022/10/1 18:07
 */
@Slf4j
public class IpLocationUtil {

    private static volatile Ip2Region ip2Region;
    private static volatile boolean initialized = false;
    private static final Object LOCK = new Object();

    private IpLocationUtil() {
        // 私有构造函数,防止实例化
    }

    /**
     * 获取 Ip2Region 实例 (懒加载 + 双重检查锁定)
     */
    private static Ip2Region getInstance() {
        if (!initialized) {
            synchronized (LOCK) {
                if (!initialized) {
                    ip2Region = initializeIp2Region();
                    initialized = true;
                }
            }
        }
        return ip2Region;
    }

    /**
     * 初始化 Ip2Region 实例
     */
    private static Ip2Region initializeIp2Region() {
        Config v4Config;
        ClassPathResource resource = new ClassPathResource("ip2region/ip2region_v4.xdb");

        try (InputStream is = resource.getInputStream()) {
            v4Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setSearchers(15)
                    .setXdbInputStream(is)
                    .asV4();
        } catch (Exception e) {
            log.error("Strix IP-Region: IPv4 配置初始化失败.", e);
            return null;
        }

        try {
            return Ip2Region.create(v4Config, null);
        } catch (Exception e) {
            log.error("Strix IP-Region: Ip2Region 实例初始化失败.", e);
            return null;
        }
    }

    /**
     * 查询 IP 地址的地理位置
     *
     * @param ip IP 地址
     * @return 地理位置信息
     */
    public static String get(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "empty";
        }
        Ip2Region instance = getInstance();
        if (Objects.isNull(instance)) {
            log.warn("Strix IP-Region: 功能未初始化,无法查询 IP: {}", ip);
            return "unavailable";
        }

        try {
            String region = instance.search(ip);
            return StringUtils.hasText(region) ? region : "unknown";
        } catch (Exception e) {
            log.error("Strix IP-Region: IP 地址 [{}] 查询失败.", ip, e);
            return "unknown";
        }
    }

    /**
     * 检查服务是否可用
     *
     * @return true 如果服务已初始化且可用
     */
    public static boolean isAvailable() {
        return initialized && Objects.nonNull(ip2Region);
    }

}
