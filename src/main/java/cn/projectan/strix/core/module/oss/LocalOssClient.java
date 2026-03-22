package cn.projectan.strix.core.module.oss;

import cn.hutool.core.io.FileUtil;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.other.system.module.oss.StrixOssBucket;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.util.List;

/**
 * 本地 OSS 客户端
 *
 * @author ProjectAn
 * @since 2024/8/15 17:37
 */
@Slf4j
public class LocalOssClient implements StrixOssClient {

    private final DefaultOperations publicOperations;
    private final DefaultOperations privateOperations;

    public LocalOssClient(String publicDomain, String privateDomain) {
        this.publicOperations = new DefaultOperations();
        this.privateOperations = new DefaultOperations();
    }

    public DefaultOperations getPublic() {
        return publicOperations;
    }

    public DefaultOperations getPrivate() {
        return privateOperations;
    }

    @Override
    public void close() {
    }

    public static class DefaultOperations implements StrixOssClient.Operations {

        /**
         * 安全地解析文件路径, 防止路径穿越攻击.
         * 拒绝包含 ".." 的路径, 防止通过相对路径逃逸出预期目录.
         */
        private File safeResolve(String objectName) {
            if (objectName == null || objectName.isBlank()) {
                throw new StrixException("Strix OSS: 文件路径不能为空.");
            }
            // 将路径标准化并检查是否包含路径穿越序列
            String normalized = objectName.replace('\\', '/');
            for (String segment : normalized.split("/")) {
                if ("..".equals(segment)) {
                    throw new StrixException("Strix OSS: 非法文件路径.");
                }
            }
            try {
                File file = new File(objectName);
                File canonical = file.getCanonicalFile();
                File absoluteRef = file.getAbsoluteFile();
                // 确保规范化路径以原始绝对路径的父目录开头, 防止符号链接绕过
                if (!canonical.getPath().startsWith(absoluteRef.getParentFile().getCanonicalPath())) {
                    throw new StrixException("Strix OSS: 非法文件路径.");
                }
                return canonical;
            } catch (IOException e) {
                throw new StrixException("Strix OSS: 文件路径解析失败.");
            }
        }

        private File createFile(String objectName) throws IOException {
            File file = safeResolve(objectName);
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                Assert.isTrue(parentFile.mkdirs(), "创建文件夹失败");
            }
            if (!file.exists()) {
                Assert.isTrue(file.createNewFile(), "创建文件失败");
            }
            return file;
        }

        @Override
        public void upload(String bucketName, String objectName, byte[] buf) {
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf)) {
                File newFile = createFile(objectName);
                FileUtil.writeFromStream(byteArrayInputStream, newFile);
            } catch (IOException e) {
                log.error("本地存储操作异常: {}", e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public void upload(String bucketName, String objectName, InputStream inputStream, long contentLength) {
            try {
                File newFile = createFile(objectName);
                FileUtil.writeFromStream(inputStream, newFile);
            } catch (IOException e) {
                log.error("本地存储操作异常: {}", e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        @Deprecated
        public void upload(String bucketName, String objectName, InputStream inputStream) {
            upload(bucketName, objectName, inputStream, -1);
        }

        @Override
        public void upload(String bucketName, String objectName, File file) {
            try {
                File newFile = createFile(objectName);
                // 注意: 这里覆盖了原文件
                FileUtil.copy(file, newFile, true);
            } catch (IOException e) {
                log.error("本地存储操作异常: {}", e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public String signUploadUrl(String bucketName, String objectName, long expires) {
            return null;
        }

        @Override
        public File download(String bucketName, String objectName, String filePath) {
            File file = safeResolve(objectName);
            if (!file.exists()) {
                return null;
            }
            File saveFile = new File(filePath);
            FileUtil.copy(file, saveFile, true);
            return saveFile;
        }

        @Override
        public InputStream downloadAsStream(String bucketName, String objectName) {
            File file = safeResolve(objectName);
            if (!file.exists()) {
                throw new StrixException("Strix OSS: 文件不存在.");
            }
            try {
                return new FileInputStream(file);
            } catch (IOException e) {
                log.error("本地存储操作异常: {}", e.getMessage(), e);
                throw new StrixException("Strix OSS: 下载文件失败.");
            }
        }

        @Override
        public void downloadToStream(String bucketName, String objectName, OutputStream outputStream) {
            File file = safeResolve(objectName);
            if (!file.exists()) {
                throw new StrixException("Strix OSS: 文件不存在.");
            }
            try (InputStream inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                log.error("本地存储操作异常: {}", e.getMessage(), e);
                throw new StrixException("Strix OSS: 下载文件失败.");
            }
        }

        @Override
        @Deprecated
        public File downloadStream(String bucketName, String objectName, String filePath) {
            return download(bucketName, objectName, filePath);
        }

        @Override
        public StreamingResponseBody downloadStream(String bucketName, String objectName, HttpServletResponse response) {
            File file = safeResolve(objectName);
            if (!file.exists()) {
                throw new StrixException("Strix OSS: 文件不存在.");
            }

            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            response.setContentLengthLong(file.length());

            return outputStream -> {
                try (InputStream inputStream = FileUtil.getInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    log.error("Strix OSS: 流式下载文件失败: {}", e.getMessage(), e);
                    throw new StrixException("Strix OSS: 流式下载文件失败.");
                }
            };
        }

        @Override
        public String signDownloadUrl(String bucketName, String objectName, long expires) {
            return null;
        }

        @Override
        public boolean exist(String bucketName, String objectName) {
            File file = safeResolve(objectName);
            return file.exists();
        }

        @Override
        public void list(String bucketName, String prefix, int maxKeys) {
            File dir = new File(bucketName);
            if (!dir.exists() || !dir.isDirectory()) {
                throw new StrixException("Strix OSS: 目录不存在或不是目录.");
            }

            File[] files = dir.listFiles((d, name) -> {
                if (!StringUtils.hasText(prefix)) {
                    return true;
                }
                return name.startsWith(prefix);
            });
            if (files == null) {
                throw new StrixException("Strix OSS: 获取文件列表失败.");
            }

            int count = 0;
            for (File file : files) {
                if (count >= maxKeys) {
                    break;
                }
                log.debug("本地文件列表: {}", file.getName());
                count++;
            }
        }

        @Override
        public void delete(String bucketName, String objectName) {
            File file = safeResolve(objectName);
            if (file.exists()) {
                Assert.isTrue(file.delete(), "删除文件失败");
            }
        }

        @Override
        public List<StrixOssBucket> listBuckets() {
            return List.of();
        }

        @Override
        public void createBucket(String bucketName) {

        }

        @Override
        public void deleteBucket(String bucketName) {

        }
    }

}
