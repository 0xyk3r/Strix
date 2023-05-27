package cn.projectan.strix.utils;

import cn.projectan.strix.core.exception.StrixException;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;

/**
 * 文件拓展名工具类
 *
 * @author 安炯奕
 * @date 2023/5/22 18:21
 */
public class FileExtUtil {

    /**
     * 将 MimeType 转换为文件拓展名
     *
     * @param mimeType MimeType
     * @return 文件拓展名
     */
    public static String mime2ext(String mimeType) {
        try {
            MimeType type = MimeTypes.getDefaultMimeTypes().forName(mimeType);
            return type.getExtension();
        } catch (Exception e) {
            throw new StrixException("获取文件扩展名失败");
        }
    }

}
