package cn.projectan.strix.utils;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author 安炯奕
 * @date 2022/10/1 18:07
 */
@Slf4j
public class Ip2RegionUtil {

    private static Searcher searcher;

    static {
        File resourcesDir = new File("src/main/resources");
        byte[] cBuff;
        try (RandomAccessFile raf = new RandomAccessFile(resourcesDir.getCanonicalPath() + "/ip2region/ip2region.xdb", "r")) {
            cBuff = Searcher.loadContent(raf);
            searcher = Searcher.newWithBuffer(cBuff);
            log.info("Strix IP-Region: 初始化成功");
        } catch (Exception e) {
            log.error("Strix IP-Region: 初始化IP-Region功能失败", e);
        }
    }

    public static String getRegion(String ip) {
        if (searcher == null) {
            throw new IllegalArgumentException("Strix IP-Region: 功能未初始化");
        }
        try {
            String result = searcher.search(ip);
            return result.replaceAll("\\|", " ").replaceAll("0", "").replaceAll(" +", " ");
        } catch (Exception e) {
            log.error("Strix IP-Region: 获取IP-Region失败", e);
            return "unknown";
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(getRegion("1.1.1.1"));
        System.out.println(getRegion("114.114.114.114"));
        System.out.println(getRegion("119.29.29.29"));
        System.out.println(getRegion("223.5.5.5"));
        System.out.println(getRegion("8.8.8.8"));
        System.out.println(getRegion("114.244.69.97"));
    }

}
