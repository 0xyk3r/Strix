package cn.projectan.strix.core.module.oss;

import cn.hutool.core.io.FileUtil;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.other.module.oss.StrixOssBucket;
import cn.projectan.strix.util.tempurl.TempUrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    public LocalOssClient(String publicDomain, String privateDomain, TempUrlUtil tempUrlUtil) {
        this.publicOperations = new DefaultOperations(publicDomain, tempUrlUtil);
        this.privateOperations = new DefaultOperations(privateDomain, tempUrlUtil);
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

        private final String publicDomain;
        private final TempUrlUtil tempUrlUtil;

        public DefaultOperations(String publicDomain, TempUrlUtil tempUrlUtil) {
            this.publicDomain = publicDomain;
            this.tempUrlUtil = tempUrlUtil;
        }

        private File createFile(String objectName) throws IOException {
            File file = new File(objectName);
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
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public void upload(String bucketName, String objectName, InputStream inputStream) {
            try {
                File newFile = createFile(objectName);
                FileUtil.writeFromStream(inputStream, newFile);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public void upload(String bucketName, String objectName, File file) {
            try {
                File newFile = createFile(objectName);
                // 注意: 这里覆盖了原文件
                FileUtil.copy(file, newFile, true);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
        }

        @Override
        public String signUploadUrl(String bucketName, String objectName, long expires) {
            return null;
        }

        @Override
        public File download(String bucketName, String objectName, String filePath) {
            File file = new File(objectName);
            if (!file.exists()) {
                return null;
            }
            File saveFile = new File(filePath);
            FileUtil.copy(file, saveFile, true);
            return saveFile;
        }

        @Override
        public File downloadStream(String bucketName, String objectName, String filePath) {
            return download(bucketName, objectName, filePath);
        }

        @Override
        public String signDownloadUrl(String bucketName, String objectName, long expires) {
            String ext = FileUtil.extName(objectName);
            File tempFile = FileUtil.createTempFile("strix-oss-", "." + ext, false);
            File downloadFile = download(bucketName, objectName, tempFile.getAbsolutePath());
            Assert.isTrue(downloadFile != null && downloadFile.exists(), "文件不存在");

            String keyData = tempFile.getAbsolutePath();
            String key = tempUrlUtil.create(keyData, expires);
            return publicDomain + "/system/common/url/file/" + key;
        }

        @Override
        public boolean exist(String bucketName, String objectName) {
            File file = new File(objectName);
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
                System.out.println(file.getName());
                count++;
            }
        }

        @Override
        public void delete(String bucketName, String objectName) {
            File file = new File(objectName);
            if (file.exists()) {
                Assert.isTrue(file.delete(), "删除文件失败");
            }
        }

        @Override
        public List<StrixOssBucket> listBuckets() {
            return List.of();
        }

        @Override
        public void createBucket(String bucketName, String storageClass) {

        }

        @Override
        public void deleteBucket(String bucketName) {

        }
    }

}
