package cn.projectan.strix.core.captcha.util;

import cn.projectan.strix.model.enums.CaptchaBaseMapEnum;
import lombok.extern.slf4j.Slf4j;
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

    private static final Map<String, String> originalCacheMap = new ConcurrentHashMap<>();  //滑块底图
    private static final Map<String, String> slidingBlockCacheMap = new ConcurrentHashMap<>(); //滑块
    private static final Map<String, String> picClickCacheMap = new ConcurrentHashMap<>(); //点选文字
    private static final Map<String, String[]> fileNameMap = new ConcurrentHashMap<>();

    public static void cacheImage(String captchaOriginalPathJigsaw, String captchaOriginalPathClick) {
        // 滑动拼图
        if (!StringUtils.hasText(captchaOriginalPathJigsaw)) {
            originalCacheMap.putAll(getResourcesImagesFile("captchaImages/jigsaw/original"));
            slidingBlockCacheMap.putAll(getResourcesImagesFile("captchaImages/jigsaw/slidingBlock"));
        } else {
            originalCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + File.separator + "original"));
            slidingBlockCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + File.separator + "slidingBlock"));
        }
        // 点选文字
        if (!StringUtils.hasText(captchaOriginalPathClick)) {
            picClickCacheMap.putAll(getResourcesImagesFile("captchaImages/pic-click"));
        } else {
            picClickCacheMap.putAll(getImagesFile(captchaOriginalPathClick));
        }
        fileNameMap.put(CaptchaBaseMapEnum.ORIGINAL.getCodeValue(), originalCacheMap.keySet().toArray(new String[0]));
        fileNameMap.put(CaptchaBaseMapEnum.SLIDING_BLOCK.getCodeValue(), slidingBlockCacheMap.keySet().toArray(new String[0]));
        fileNameMap.put(CaptchaBaseMapEnum.PIC_CLICK.getCodeValue(), picClickCacheMap.keySet().toArray(new String[0]));
//        logger.info("初始化底图:{}", JsonUtil.toJSONString(fileNameMap));
    }

    public static void cacheBootImage(Map<String, String> originalMap, Map<String, String> slidingBlockMap, Map<String, String> picClickMap) {
        originalCacheMap.putAll(originalMap);
        slidingBlockCacheMap.putAll(slidingBlockMap);
        picClickCacheMap.putAll(picClickMap);
        fileNameMap.put(CaptchaBaseMapEnum.ORIGINAL.getCodeValue(), originalCacheMap.keySet().toArray(new String[0]));
        fileNameMap.put(CaptchaBaseMapEnum.SLIDING_BLOCK.getCodeValue(), slidingBlockCacheMap.keySet().toArray(new String[0]));
        fileNameMap.put(CaptchaBaseMapEnum.PIC_CLICK.getCodeValue(), picClickCacheMap.keySet().toArray(new String[0]));
//        logger.info("自定义resource底图:{}", JsonUtil.toJSONString(fileNameMap));
    }

    public static BufferedImage getOriginal() {
        String[] strings = fileNameMap.get(CaptchaBaseMapEnum.ORIGINAL.getCodeValue());
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = StrixCaptchaRandomUtils.getRandomInt(0, strings.length);
        String s = originalCacheMap.get(strings[randomInt]);
        return getBase64StrToImage(s);
    }

    public static String getSlidingBlock() {
        String[] strings = fileNameMap.get(CaptchaBaseMapEnum.SLIDING_BLOCK.getCodeValue());
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = StrixCaptchaRandomUtils.getRandomInt(0, strings.length);
        return slidingBlockCacheMap.get(strings[randomInt]);
    }

    public static BufferedImage getPicClick() {
        String[] strings = fileNameMap.get(CaptchaBaseMapEnum.PIC_CLICK.getCodeValue());
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = StrixCaptchaRandomUtils.getRandomInt(0, strings.length);
        String s = picClickCacheMap.get(strings[randomInt]);
        return getBase64StrToImage(s);
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

        Base64.Encoder encoder = Base64.getEncoder();

        return encoder.encodeToString(bytes).trim();
    }

    /**
     * base64 字符串转图片
     *
     * @param base64String base64 字符串
     * @return 图片
     */
    public static BufferedImage getBase64StrToImage(String base64String) {
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] bytes = decoder.decode(base64String);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            log.warn("base64转图片异常", e);
        }
        return null;
    }


    private static Map<String, String> getResourcesImagesFile(String path) {
        // 默认提供六张底图
        Map<String, String> imgMap = new HashMap<>();
        ClassLoader classLoader = StrixCaptchaImageUtils.class.getClassLoader();
        for (int i = 1; i <= 6; i++) {
            InputStream resourceAsStream = classLoader.getResourceAsStream(path.concat("/").concat(String.valueOf(i).concat(".png")));
            byte[] bytes = new byte[0];
            try {
                bytes = StrixCaptchaFileCopyUtils.copyToByteArray(resourceAsStream);
            } catch (IOException e) {
                log.warn("读取资源图片异常", e);
            }
            String string = StrixCaptchaBase64Utils.encodeToString(bytes);
            String filename = String.valueOf(i).concat(".png");
            imgMap.put(filename, string);
        }
        return imgMap;
    }

    private static Map<String, String> getImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>();
        File file = new File(path);
        if (!file.exists()) {
            return new HashMap<>();
        }
        File[] files = file.listFiles();
        if (files != null) {
            Arrays.stream(files).forEach(item -> {
                try {
                    FileInputStream fileInputStream = new FileInputStream(item);
                    byte[] bytes = StrixCaptchaFileCopyUtils.copyToByteArray(fileInputStream);
                    String string = StrixCaptchaBase64Utils.encodeToString(bytes);
                    imgMap.put(item.getName(), string);
                } catch (IOException e) {
                    log.warn("读取自定义图片异常", e);
                }
            });
        }
        return imgMap;
    }

}
