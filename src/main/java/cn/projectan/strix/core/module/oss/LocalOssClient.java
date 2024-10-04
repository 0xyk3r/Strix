package cn.projectan.strix.core.module.oss;

import cn.hutool.core.io.FileUtil;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.other.module.oss.StrixOssBucket;
import cn.projectan.strix.utils.tempurl.TempUrlUtil;
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
public class LocalOssClient extends StrixOssClient {

    private final String publicDomain;
    private final String privateDomain;
    private final TempUrlUtil tempUrlUtil;

    public LocalOssClient(String publicDomain, String privateDomain, TempUrlUtil tempUrlUtil) {
        this.publicDomain = publicDomain;
        this.privateDomain = privateDomain;
        this.tempUrlUtil = tempUrlUtil;
    }

    @Override
    public Object getPublic() {
        return null;
    }

    @Override
    public Object getPrivate() {
        return null;
    }

    @Override
    public void uploadPublic(String bucketName, String objectName, byte[] buf) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf)) {
            File file = new File(objectName);
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            FileUtil.writeFromStream(byteArrayInputStream, file);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new StrixException("Strix OSS: 上传文件失败.");
        }
    }

    @Override
    public void uploadPrivate(String bucketName, String objectName, byte[] buf) {
        uploadPublic(bucketName, objectName, buf);
    }

    @Override
    public File downloadPublic(String bucketName, String objectName, String filePath) {
        File file = new File(objectName);
        if (!file.exists()) {
            return null;
        }
        File saveFile = new File(filePath);
        FileUtil.copy(file, saveFile, true);
        return saveFile;
    }

    @Override
    public File downloadPrivate(String bucketName, String objectName, String filePath) {
        return downloadPublic(bucketName, objectName, filePath);
    }

    @Override
    public String getUrlPublic(String bucketName, String objectName, long expires) {
        String ext = FileUtil.extName(objectName);
        File tempFile = FileUtil.createTempFile("strix-oss-", "." + ext, false);
        File downloadFile = downloadPublic(bucketName, objectName, tempFile.getAbsolutePath());
        Assert.isTrue(downloadFile != null && downloadFile.exists(), "文件不存在");

        String keyData = tempFile.getAbsolutePath();
        String key = tempUrlUtil.create(keyData, expires);
        return publicDomain + "/system/common/url/file/" + key;
    }

    @Override
    public String getUrlPrivate(String bucketName, String objectName, long expires) {
        String ext = FileUtil.extName(objectName);
        File tempFile = FileUtil.createTempFile("strix-oss-", "." + ext, false);
        File downloadFile = downloadPrivate(bucketName, objectName, tempFile.getAbsolutePath());
        Assert.isTrue(downloadFile != null && downloadFile.exists(), "文件不存在");

        String keyData = tempFile.getAbsolutePath();
        String key = tempUrlUtil.create(keyData, expires);
        return privateDomain + "/system/common/url/file/" + key;
    }

    @Override
    public void deletePublic(String bucketName, String objectName) {
        File file = new File(objectName);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void deletePrivate(String bucketName, String objectName) {
        deletePublic(bucketName, objectName);
    }

    @Override
    public List<StrixOssBucket> getBucketList() {
        return List.of();
    }

    @Override
    public void createBucket(String bucketName, String storageClass) {

    }

    @Override
    public void close() {

    }
}
