package cn.projectan.strix.util;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MIME 工具类
 *
 * @author ProjectAn
 * @since 2024/4/4 04:18
 */
public final class MimeUtil {

    private static final String MIME_TYPES_FILE_NAME = "/org/springframework/http/mime.types";

    private static final Map<String, String[]> mime2ExtMap = new HashMap<>(765);
    private static final Map<String, String> ext2MimeMap = new HashMap<>(982);

    static {
        init();
    }

    private MimeUtil() {
    }

    private static void init() {
        InputStream is = MimeUtil.class.getResourceAsStream(MIME_TYPES_FILE_NAME);
        Assert.notNull(is, MIME_TYPES_FILE_NAME + " not found in classpath");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] tokens = StringUtils.tokenizeToStringArray(line, " \t\n\r\f");
                String[] extArr = new String[tokens.length - 1];
                for (int i = 1; i < tokens.length; i++) {
                    String fileExtension = tokens[i].toLowerCase(Locale.ENGLISH);
                    extArr[i - 1] = fileExtension;
                    ext2MimeMap.put(fileExtension, tokens[0]);
                }
                mime2ExtMap.put(tokens[0], extArr);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + MIME_TYPES_FILE_NAME, ex);
        }
    }

    /**
     * 根据 文件扩展名 获取 MIME 类型
     *
     * @param ext 文件扩展名
     * @return MIME 类型
     */
    public static String ext2Mime(String ext) {
        if (StringUtils.hasText(ext) && ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        return ext2MimeMap.get(ext.toLowerCase(Locale.ENGLISH));
    }

    /**
     * 根据 MIME 类型 获取 文件扩展名
     *
     * @param mimeType MIME 类型
     * @return 文件扩展名
     */
    public static String[] mime2Exts(String mimeType) {
        return mime2ExtMap.get(mimeType);
    }

    /**
     * 根据 MIME 类型 获取 文件扩展名
     *
     * @param mimeType MIME 类型
     * @return 文件扩展名
     */
    public static String mime2Ext(String mimeType) {
        String[] extArr = mime2Exts(mimeType);
        return (extArr != null ? extArr[0] : null);
    }

}
