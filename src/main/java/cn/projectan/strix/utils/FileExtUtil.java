package cn.projectan.strix.utils;

import cn.projectan.strix.core.exception.StrixException;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import java.util.Optional;

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


    /**
     * 将文件拓展名转换为 MimeType
     *
     * @param ext 文件拓展名
     * @return MimeType
     */
    public static String ext2mime(String ext) {
        return ext2mime(ext, MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * 将文件拓展名转换为 MimeType
     *
     * @param ext 文件拓展名
     * @return MimeType
     */
    public static String ext2mime(String ext, MediaType defaultMediaType) {
        try {
            Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(ext);
            return mediaType.orElse(defaultMediaType).toString();
        } catch (Exception e) {
            throw new StrixException("获取文件 MimeType 失败");
        }

    }
}
