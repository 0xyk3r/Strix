package cn.projectan.strix.core.captcha.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strix Captcha 图像工具类
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
@Slf4j
public class StrixCaptchaImageUtils {

    private static final String ORIGINAL_KEY = "original";
    private static final String SLIDING_BLOCK_KEY = "slidingBlock";

    private static final Map<String, String> originalCacheMap = new ConcurrentHashMap<>();
    private static final Map<String, String> slidingBlockCacheMap = new ConcurrentHashMap<>();
    private static final Map<String, String[]> fileNameMap = new ConcurrentHashMap<>();

    /**
     * 缓存图片到内存
     * 支持三种路径格式:
     * 1. null 或空字符串: 使用默认的 classpath:captchaImages/jigsaw
     * 2. classpath:路径: 从 classpath 加载资源
     * 3. 文件系统路径: 从文件系统加载
     *
     * @param captchaOriginalPathJigsaw 图片路径
     */
    public static void cacheImage(String captchaOriginalPathJigsaw) {
        if (!StringUtils.hasText(captchaOriginalPathJigsaw)) {
            // 场景1: 使用默认的 classpath 资源
            log.debug("Strix Captcha: 使用默认验证码图片路径: classpath:captchaImages/jigsaw");
            originalCacheMap.putAll(getResourcesImagesFile("captchaImages/jigsaw/original"));
            slidingBlockCacheMap.putAll(getResourcesImagesFile("captchaImages/jigsaw/slidingBlock"));
        } else if (captchaOriginalPathJigsaw.startsWith("classpath:")) {
            // 场景2: 自定义 classpath 资源路径
            String classPath = captchaOriginalPathJigsaw.substring("classpath:".length());
            log.debug("Strix Captcha: 使用自定义 classpath 验证码图片路径: {}", captchaOriginalPathJigsaw);
            originalCacheMap.putAll(getClasspathImagesFile(classPath + "/original/*.png"));
            slidingBlockCacheMap.putAll(getClasspathImagesFile(classPath + "/slidingBlock/*.png"));
        } else {
            // 场景3: 文件系统路径
            log.debug("Strix Captcha: 使用文件系统验证码图片路径: {}", captchaOriginalPathJigsaw);
            originalCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + File.separator + "original"));
            slidingBlockCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + File.separator + "slidingBlock"));
        }
        fileNameMap.put(ORIGINAL_KEY, originalCacheMap.keySet().toArray(new String[0]));
        fileNameMap.put(SLIDING_BLOCK_KEY, slidingBlockCacheMap.keySet().toArray(new String[0]));
        log.debug("Strix Captcha: 验证码图片缓存完成 - 底图: {} 张, 滑块: {} 张",
                originalCacheMap.size(), slidingBlockCacheMap.size());
    }

    public static BufferedImage getOriginal() {
        String[] strings = fileNameMap.get(ORIGINAL_KEY);
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = StrixCaptchaRandomUtils.getRandomInt(0, strings.length);
        String s = originalCacheMap.get(strings[randomInt]);
        return getBase64StrToImage(s);
    }

    public static String getSlidingBlock() {
        String[] strings = fileNameMap.get(SLIDING_BLOCK_KEY);
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = StrixCaptchaRandomUtils.getRandomInt(0, strings.length);
        return slidingBlockCacheMap.get(strings[randomInt]);
    }

    /**
     * 图片转base64 字符串
     *
     * @param templateImage 模板图片
     * @return base64 字符串
     */
    public static String getImageToBase64Str(BufferedImage templateImage) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(templateImage, "png", os);
        } catch (IOException e) {
            log.warn("图片转base64异常", e);
        }
        byte[] bytes = os.toByteArray();
        return Base64.getEncoder().encodeToString(bytes).trim();
    }

    /**
     * base64 字符串转图片
     *
     * @param base64String base64 字符串
     * @return 图片
     */
    public static BufferedImage getBase64StrToImage(String base64String) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64String);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            log.warn("base64转图片异常", e);
        }
        return null;
    }

    /**
     * 从 classpath 加载图片文件（使用通配符模式）
     *
     * @param pattern 资源路径模式，例如: "captchaImages/jigsaw/original/*.png"
     * @return 图片名称与 Base64 编码的映射
     */
    private static Map<String, String> getClasspathImagesFile(String pattern) {
        Map<String, String> imgMap = new HashMap<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:" + pattern);
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
                    String string = Base64.getEncoder().encodeToString(bytes);
                    String filename = resource.getFilename();
                    if (filename != null) {
                        imgMap.put(filename, string);
                    }
                } catch (IOException e) {
                    log.warn("读取 classpath 图片资源异常: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.warn("扫描 classpath 图片资源异常: {}", pattern, e);
        }
        return imgMap;
    }

    /**
     * 从 classpath 加载默认图片（固定数量1-6.png）
     *
     * @param path 资源路径，例如: "captchaImages/jigsaw/original"
     * @return 图片名称与 Base64 编码的映射
     */
    private static Map<String, String> getResourcesImagesFile(String path) {
        // 默认提供六张底图
        Map<String, String> imgMap = new HashMap<>();
        ClassLoader classLoader = StrixCaptchaImageUtils.class.getClassLoader();
        for (int i = 1; i <= 6; i++) {
            try (InputStream resourceAsStream = classLoader.getResourceAsStream(path.concat("/").concat(String.valueOf(i).concat(".png")))) {
                if (resourceAsStream != null) {
                    byte[] bytes = FileCopyUtils.copyToByteArray(resourceAsStream);
                    String string = Base64.getEncoder().encodeToString(bytes);
                    String filename = String.valueOf(i).concat(".png");
                    imgMap.put(filename, string);
                }
            } catch (IOException e) {
                log.warn("读取资源图片异常", e);
            }
        }
        return imgMap;
    }

    /**
     * 从文件系统加载图片文件
     *
     * @param path 文件系统路径
     * @return 图片名称与 Base64 编码的映射
     */
    private static Map<String, String> getImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>();
        File file = new File(path);
        if (!file.exists()) {
            return new HashMap<>();
        }
        File[] files = file.listFiles();
        if (files != null) {
            Arrays.stream(files).forEach(item -> {
                try (FileInputStream fileInputStream = new FileInputStream(item)) {
                    byte[] bytes = FileCopyUtils.copyToByteArray(fileInputStream);
                    String string = Base64.getEncoder().encodeToString(bytes);
                    imgMap.put(item.getName(), string);
                } catch (IOException e) {
                    log.warn("读取自定义图片异常", e);
                }
            });
        }
        return imgMap;
    }

}
