package cn.projectan.strix.core.module.ai;

import cn.projectan.strix.model.request.system.module.ai.AiAttachment;
import cn.projectan.strix.service.system.OssFileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * AI 附件解析器
 * <p>将 fileId 引用解析为 DashScope API 可接受的 URL 或 base64 data URI。
 * <ul>
 *   <li>Cloud OSS → 签名 URL</li>
 *   <li>Local 存储 → 读取文件内容 → base64 data URI</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAttachmentResolver {

    private final OssFileService ossFileService;

    @Data
    public static class ResolvedAttachment {
        private String type;
        private String mimeType;
        private String dataUrl;
        private String format;
    }

    public List<ResolvedAttachment> resolve(List<AiAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        List<ResolvedAttachment> results = new ArrayList<>(attachments.size());
        for (AiAttachment att : attachments) {
            ResolvedAttachment resolved = resolveOne(att);
            if (resolved != null) {
                results.add(resolved);
            }
        }
        return results;
    }

    private ResolvedAttachment resolveOne(AiAttachment att) {
        ResolvedAttachment resolved = new ResolvedAttachment();
        resolved.setType(att.getType());
        resolved.setMimeType(att.getMimeType());

        try {
            String url = ossFileService.getUrl(att.getFileId(), null);
            if (url != null) {
                resolved.setDataUrl(url);
            } else {
                resolved.setDataUrl(readAsBase64DataUri(att.getFileId(), att.getMimeType()));
            }
        } catch (Exception e) {
            log.warn("AI: 附件解析失败(fileId={})，跳过: {}", att.getFileId(), e.getMessage());
            return null;
        }

        if ("audio".equals(att.getType()) && att.getMimeType() != null) {
            // 取分号前的主类型（如 "audio/webm;codecs=opus" → "webm"）
            String format = att.getMimeType().replace("audio/", "").split(";")[0].trim();
            if ("mpeg".equals(format)) format = "mp3";
            if ("x-wav".equals(format)) format = "wav";
            resolved.setFormat(format);
        }

        return resolved;
    }

    private static final int MAX_LOCAL_BASE64_BYTES = 10 * 1024 * 1024; // 10MB

    private String readAsBase64DataUri(String fileId, String mimeType) {
        try (InputStream is = ossFileService.downloadAsStream(fileId)) {
            byte[] bytes = is.readNBytes(MAX_LOCAL_BASE64_BYTES + 1);
            if (bytes.length > MAX_LOCAL_BASE64_BYTES) {
                throw new IllegalArgumentException(
                        "本地存储附件超过 10MB 无法转为 Base64，请配置云端 OSS 以使用公网 URL 方式传递大文件");
            }
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mime = mimeType != null ? mimeType : "application/octet-stream";
            return "data:" + mime + ";base64," + base64;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to read local file as base64: fileId={}", fileId, e);
            throw new RuntimeException("Failed to read local file: " + fileId, e);
        }
    }
}
