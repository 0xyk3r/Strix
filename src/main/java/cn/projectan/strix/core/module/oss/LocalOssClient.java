package cn.projectan.strix.core.module.oss;

import cn.hutool.core.io.FileUtil;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.other.module.oss.StrixOssBucket;
import cn.projectan.strix.util.tempurl.TempUrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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

        @Override
        public void upload(String bucketName, String objectName, byte[] buf) {
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf)) {
                File file = new File(objectName);
                File parentFile = file.getParentFile();
                if (parentFile != null && !parentFile.exists()) {
                    Assert.isTrue(parentFile.mkdirs(), "创建文件夹失败");
                }
                if (!file.exists()) {
                    Assert.isTrue(file.createNewFile(), "创建文件失败");
                }
                FileUtil.writeFromStream(byteArrayInputStream, file);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new StrixException("Strix OSS: 上传文件失败.");
            }
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
        public String getUrl(String bucketName, String objectName, long expires) {
            String ext = FileUtil.extName(objectName);
            File tempFile = FileUtil.createTempFile("strix-oss-", "." + ext, false);
            File downloadFile = download(bucketName, objectName, tempFile.getAbsolutePath());
            Assert.isTrue(downloadFile != null && downloadFile.exists(), "文件不存在");

            String keyData = tempFile.getAbsolutePath();
            String key = tempUrlUtil.create(keyData, expires);
            return publicDomain + "/system/common/url/file/" + key;
        }

        @Override
        public void delete(String bucketName, String objectName) {
            File file = new File(objectName);
            if (file.exists()) {
                Assert.isTrue(file.delete(), "删除文件失败");
            }
        }

        @Override
        public List<StrixOssBucket> getBucketList() {
            return List.of();
        }

        @Override
        public void createBucket(String bucketName, String storageClass) {

        }

    }

}
