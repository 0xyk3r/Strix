package cn.projectan.strix.util.file;

import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.text.StrixBase64Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件工具类
 *
 * @author ProjectAn
 * @since 2025-01-27
 */
@Slf4j
public final class FileUtil {

    /**
     * Base64 文件数据正则表达式
     * 格式: data:mime/type;base64,xxxxx
     */
    public static final Pattern BASE64_DATA_PATTERN = Pattern.compile("^data:([^;]+);base64,(.+)$");

    /**
     * 默认缓冲区大小 (8KB)
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private FileUtil() {
    }

    /**
     * 将 MultipartFile 转换为临时 File
     *
     * @param multipartFile MultipartFile
     * @return 临时文件
     * @throws IOException IO异常
     */
    public static File toTempFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.get("error.fileUtil.emptyFile"));
        }

        String originalFilename = multipartFile.getOriginalFilename();
        Assert.hasText(originalFilename, "文件名不能为空");

        String ext = StringUtils.getFilenameExtension(originalFilename);
        String prefix = StringUtils.stripFilenameExtension(originalFilename);
        if (!StringUtils.hasText(prefix)) {
            prefix = "temp_" + UUID.randomUUID();
        }
        String suffix = StringUtils.hasText(ext) ? "." + ext : "";

        Path tempFile = Files.createTempFile(prefix + "_", suffix);
        multipartFile.transferTo(tempFile);
        return tempFile.toFile();
    }

    /**
     * 将 MultipartFile 写入到指定 File
     *
     * @param multipartFile MultipartFile
     * @param destFile      目标文件
     * @throws IOException IO异常
     */
    public static void transferTo(MultipartFile multipartFile, File destFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.get("error.fileUtil.emptyFile"));
        }
        multipartFile.transferTo(destFile.toPath());
    }

    /**
     * 从 Base64 数据字符串中解析文件内容
     *
     * @param base64Data Base64 数据字符串 (格式: data:mime/type;base64,xxxxx)
     * @return Base64ParseResult 包含 MIME 类型和解码后的字节数组
     */
    public static Base64ParseResult parseBase64Data(String base64Data) {
        if (!StringUtils.hasText(base64Data)) {
            throw new IllegalArgumentException(I18nUtil.get("error.fileUtil.emptyBase64"));
        }

        Matcher matcher = BASE64_DATA_PATTERN.matcher(base64Data);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(I18nUtil.get("error.fileUtil.invalidBase64Format"));
        }

        String mimeType = matcher.group(1);
        String base64Content = matcher.group(2);
        byte[] data = StrixBase64Util.decode(base64Content, StrixBase64Util.MAX_LENGTH_512MB);

        return new Base64ParseResult(mimeType, data);
    }

    /**
     * 将文件转换为 Base64 数据字符串
     *
     * @param file 文件
     * @return Base64 数据字符串 (格式: data:mime/type;base64,xxxxx)
     * @throws IOException IO异常
     */
    public static String toBase64DataString(File file) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        String mimeType = Files.probeContentType(file.toPath());
        return toBase64DataString(fileContent, mimeType);
    }

    /**
     * 将字节数组转换为 Base64 数据字符串
     *
     * @param data     字节数组
     * @param mimeType MIME 类型
     * @return Base64 数据字符串 (格式: data:mime/type;base64,xxxxx)
     */
    public static String toBase64DataString(byte[] data, String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            mimeType = "application/octet-stream";
        }
        String encodedString = Base64.getEncoder().encodeToString(data);
        return "data:" + mimeType + ";base64," + encodedString;
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名 (包含点号, 例如 ".jpg")
     */
    public static String getExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        String ext = StringUtils.getFilenameExtension(filename);
        if (StringUtils.hasText(ext)) {
            return "." + ext;
        }
        return "";
    }

    /**
     * 将输入流复制到输出流
     *
     * @param in         输入流
     * @param out        输出流
     * @param bufferSize 缓冲区大小
     * @return 复制的字节数
     * @throws IOException IO异常
     */
    public static long copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        long totalBytes = 0;
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            totalBytes += bytesRead;
        }
        return totalBytes;
    }

    /**
     * 将输入流复制到输出流 (使用默认缓冲区大小)
     *
     * @param in  输入流
     * @param out 输出流
     * @return 复制的字节数
     * @throws IOException IO异常
     */
    public static long copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 将输入流读取为字节数组
     *
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO异常
     */
    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            copy(inputStream, baos);
            return baos.toByteArray();
        }
    }

    /**
     * 将字节数组转换为输入流
     *
     * @param data 字节数组
     * @return 输入流
     */
    public static InputStream toInputStream(byte[] data) {
        return new ByteArrayInputStream(data);
    }

    /**
     * 将文件转换为输入流
     */
    public static InputStream open(Path path) throws IOException {
        return Files.newInputStream(path);
    }

    /**
     * 安全关闭流
     *
     * @param closeable 可关闭对象
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.debug("关闭流时出现异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 删除文件 (安全删除, 不抛异常)
     *
     * @param file 文件
     * @return 是否删除成功
     */
    public static boolean deleteQuietly(File file) {
        if (file == null || !file.exists()) {
            return true;
        }
        try {
            return file.delete();
        } catch (Exception e) {
            log.debug("删除文件时出现异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Base64 解析结果
     *
     * @param mimeType MIME 类型
     * @param data     解码后的字节数组
     */
    public record Base64ParseResult(String mimeType, byte[] data) {
    }
}
