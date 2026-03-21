package cn.projectan.strix.util.file;

import cn.hutool.core.io.FileTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * 文件 Magic Bytes 校验工具
 * <p>
 * 基于 Hutool {@link FileTypeUtil} 实现文件内容头（魔数）校验，
 * 防止攻击者通过修改文件扩展名绕过白名单限制。
 * <p>
 * 仅读取文件头 28 字节，对大文件无性能影响。
 * 对于 Hutool 无法识别的冷门类型，降级为仅扩展名校验。
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
@Slf4j
public final class FileMagicValidator {

    private FileMagicValidator() {
    }

    /**
     * 扩展名（不含点）→ Hutool FileTypeUtil 返回的类型名 映射。
     * 一个扩展名可能对应多个合法的 magic type（如 .xlsx 底层是 zip 格式）。
     */
    private static final Map<String, Set<String>> EXT_TO_MAGIC_TYPES = Map.ofEntries(
            // 图片
            Map.entry("jpg", Set.of("jpg")),
            Map.entry("jpeg", Set.of("jpg")),
            Map.entry("png", Set.of("png")),
            Map.entry("gif", Set.of("gif")),
            Map.entry("bmp", Set.of("bmp")),
            Map.entry("webp", Set.of("webp")),
            Map.entry("ico", Set.of("ico")),
            Map.entry("tif", Set.of("tif")),
            Map.entry("tiff", Set.of("tif")),
            // 文档 (Office Open XML 底层均为 zip)
            Map.entry("xlsx", Set.of("zip", "xlsx")),
            Map.entry("docx", Set.of("zip", "docx")),
            Map.entry("pptx", Set.of("zip", "pptx")),
            // 文档 (OLE2 / Compound Binary)
            Map.entry("doc", Set.of("doc", "xls", "ppt", "msi")),
            Map.entry("xls", Set.of("doc", "xls", "ppt", "msi")),
            Map.entry("ppt", Set.of("doc", "xls", "ppt", "msi")),
            // PDF
            Map.entry("pdf", Set.of("pdf")),
            // 压缩文件
            Map.entry("zip", Set.of("zip")),
            Map.entry("rar", Set.of("rar")),
            Map.entry("7z", Set.of("7z")),
            Map.entry("gz", Set.of("gz")),
            Map.entry("tar", Set.of("tar")),
            // 音视频
            Map.entry("mp3", Set.of("mp3")),
            Map.entry("mp4", Set.of("mp4")),
            Map.entry("avi", Set.of("avi")),
            Map.entry("mov", Set.of("mov")),
            Map.entry("flv", Set.of("flv")),
            Map.entry("wmv", Set.of("wmv")),
            // 可执行 / 危险文件（用于黑名单拦截）
            Map.entry("exe", Set.of("exe")),
            Map.entry("dll", Set.of("exe")),
            Map.entry("msi", Set.of("doc", "xls", "ppt", "msi"))
    );

    /**
     * 即使扩展名允许也要拒绝的危险文件内容类型
     */
    private static final Set<String> DANGEROUS_MAGIC_TYPES = Set.of("exe", "elf", "class");

    /**
     * 校验文件内容是否与声明的扩展名一致
     *
     * @param inputStream 文件输入流（仅读取头部，不影响后续读取）
     * @param extension   声明的扩展名（含点，如 ".jpg"）
     * @return true 校验通过，false 校验失败
     */
    public static boolean validate(InputStream inputStream, String extension) {
        if (inputStream == null || !StringUtils.hasText(extension)) {
            return false;
        }

        String ext = extension.startsWith(".") ? extension.substring(1).toLowerCase() : extension.toLowerCase();

        try {
            String detectedType = FileTypeUtil.getType(inputStream);
            if (detectedType == null) {
                // Hutool 无法识别 → 如果该扩展名有已知映射则拒绝，否则降级放行
                return !EXT_TO_MAGIC_TYPES.containsKey(ext);
            }

            detectedType = detectedType.toLowerCase();

            // 拦截危险文件内容
            if (DANGEROUS_MAGIC_TYPES.contains(detectedType)) {
                log.warn("Dangerous file content detected: type={}, declared extension={}", detectedType, extension);
                return false;
            }

            // 如果该扩展名有已知映射，验证内容类型是否匹配
            Set<String> allowedTypes = EXT_TO_MAGIC_TYPES.get(ext);
            if (allowedTypes != null) {
                boolean matched = allowedTypes.contains(detectedType);
                if (!matched) {
                    log.warn("File magic bytes mismatch: declared={}, detected={}", extension, detectedType);
                }
                return matched;
            }

            // 该扩展名无已知映射（冷门类型），降级为仅扩展名校验
            return true;
        } catch (Exception e) {
            log.error("File magic bytes validation error", e);
            return false;
        }
    }

    /**
     * 校验字节数组内容是否与声明的扩展名一致
     *
     * @param data      文件字节数组
     * @param extension 声明的扩展名（含点，如 ".jpg"）
     * @return true 校验通过
     */
    public static boolean validate(byte[] data, String extension) {
        if (data == null || data.length == 0) {
            return false;
        }
        // 只需要前 64 字节即可判断，避免构造大数组的 InputStream
        int headerLen = Math.min(data.length, 64);
        byte[] header = new byte[headerLen];
        System.arraycopy(data, 0, header, 0, headerLen);
        return validate(new ByteArrayInputStream(header), extension);
    }

}
