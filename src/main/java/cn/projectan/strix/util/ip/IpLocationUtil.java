package cn.projectan.strix.util.ip;

import cn.hutool.core.io.file.FileWriter;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

/**
 * IP 地理位置工具类
 *
 * @author ProjectAn
 * @since 2022/10/1 18:07
 */
@Slf4j
public class IpLocationUtil {

    private static Searcher searcher;

    static {
        ClassPathResource resource = new ClassPathResource("ip2region/ip2region.xdb");
        File file = null;
        try {
            file = Files.createTempFile("ip2region", ".xdb").toFile();
            FileWriter writer = new FileWriter(file);
            writer.writeFromStream(resource.getInputStream(), true);
        } catch (IOException e) {
            log.error("Strix IP-Region: 数据库文件读取失败.", e);
        }
        if (file != null) {
            byte[] cBuff;
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                cBuff = Searcher.loadContent(raf);
                searcher = Searcher.newWithBuffer(cBuff);
                log.info("Strix IP-Region: 初始化完成.");
            } catch (Exception e) {
                log.error("Strix IP-Region: 初始化失败.", e);
            }
        }
    }

    public static String getLocation(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "empty";
        }
        if (searcher == null) {
            throw new IllegalArgumentException("Strix IP-Region: 功能未初始化.");
        }
        try {
            return searcher.search(ip).replaceAll("\\|", " ").replaceAll("0", "").replaceAll(" +", " ");
        } catch (Exception e) {
            log.error("Strix IP-Region: 获取数据失败.", e);
            return "unknown";
        }
    }

}
