package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.core.module.oss.StrixOssConfig;
import cn.projectan.strix.mapper.OssFileMapper;
import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.model.db.OssFileGroup;
import cn.projectan.strix.model.dict.StrixOssFileGroupSecretType;
import cn.projectan.strix.service.DictService;
import cn.projectan.strix.service.OssConfigService;
import cn.projectan.strix.service.OssFileGroupService;
import cn.projectan.strix.service.OssFileService;
import cn.projectan.strix.utils.FileExtUtil;
import cn.projectan.strix.utils.RegexUtils;
import cn.projectan.strix.utils.SnowflakeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2022-03-09
 */
@Slf4j
@Service
public class OssFileServiceImpl extends ServiceImpl<OssFileMapper, OssFile> implements OssFileService {

    private final StrixOssConfig strixOssConfig;
    private final OssConfigService ossConfigService;
    private final OssFileGroupService ossFileGroupService;
    private final DictService dictService;

    @Autowired
    public OssFileServiceImpl(@Autowired(required = false) StrixOssConfig strixOssConfig, OssConfigService ossConfigService, OssFileGroupService ossFileGroupService, DictService dictService) {
        this.strixOssConfig = strixOssConfig;
        this.ossConfigService = ossConfigService;
        this.ossFileGroupService = ossFileGroupService;
        this.dictService = dictService;
    }

    @Override
    public String getUrl(String fileId, String defaultUrl) {
        try {
            OssFile ossFile = getById(fileId);
            Assert.notNull(ossFile, "下载文件失败, 文件不存在.");
            OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
            Assert.notNull(ossFileGroup, "下载文件失败, 文件组不存在.");

            return getUrl(ossFile, ossFileGroup, defaultUrl);
        } catch (Exception e) {
            Assert.hasText(defaultUrl, "获取文件URL失败. 文件不存在");
            return defaultUrl;
        }
    }

    @Override
    public String getUrl(OssFile ossFile, OssFileGroup ossFileGroup, String defaultUrl) {
        try {
            StrixOssClient client = strixOssConfig.getInstance(ossFileGroup.getConfigKey());
            Assert.notNull(client, "获取文件URL失败. OSS服务实例不存在");

            String url = client.getUrlPublic(ossFileGroup.getBucketName(), ossFile.getPath(), 300);
            // 处理自定义域名
            if (StringUtils.hasText(url) && StringUtils.hasText(ossFileGroup.getBucketDomain())) {
                Matcher matcher = RegexUtils.DOMAIN_PATTERN.matcher(url);
                if (matcher.find()) {
                    url = matcher.replaceAll(ossFileGroup.getBucketDomain());
                    return url;
                }
            }

            return StringUtils.hasText(url) ? url : defaultUrl;
        } catch (Exception e) {
            Assert.hasText(defaultUrl, "获取文件URL失败. 文件不存在");
            return defaultUrl;
        }
    }

    @Override
    public String getUrl(String fileId, Integer downloaderType, String downloaderId, String defaultUrl) {
        try {
            OssFile ossFile = getById(fileId);
            Assert.notNull(ossFile, "下载文件失败, 文件不存在.");
            OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
            Assert.notNull(ossFileGroup, "下载文件失败, 文件组不存在.");

            Assert.isTrue(checkPermission(ossFile, ossFileGroup, downloaderType, downloaderId), "下载文件失败, 文件不存在.");

            return getUrl(ossFile, ossFileGroup, defaultUrl);
        } catch (Exception e) {
            Assert.hasText(defaultUrl, "获取文件URL失败. 文件不存在");
            return defaultUrl;
        }
    }

    @Override
    public OssFile upload(String groupKey, File file, String uploaderId) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String encodedString = Base64.getEncoder().encodeToString(fileContent);
            String fileBase64 = "data:" + Files.probeContentType(file.toPath()) + ";base64," + encodedString;

            return upload(groupKey, fileBase64, uploaderId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new StrixException("上传文件失败. 解析文件失败");
        }
    }

    @Override
    public OssFile upload(String groupKey, String fileBase64, String uploaderId) {
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(groupKey);
        Assert.notNull(ossFileGroup, "上传文件失败. 文件组不存在");
        StrixOssClient client = strixOssConfig.getInstance(ossFileGroup.getConfigKey());
        Assert.notNull(client, "上传文件失败. OSS服务实例不存在");

        List<String> allowExtSet = Arrays.asList(ossFileGroup.getAllowExtension().split(","));

        Matcher matcher = RegexUtils.BASE64_FILE_PATTERN.matcher(fileBase64);
        if (matcher.matches()) {
            String mimeType = matcher.group(1);
            String data = matcher.group(2);

            String ext = FileExtUtil.mime2ext(mimeType);
            Assert.isTrue(allowExtSet.contains(ext), "上传文件失败, 不支持的文件格式.");

            byte[] imageByte = Base64.getDecoder().decode(data);
            for (int i = 0; i < imageByte.length; ++i) {
                if (imageByte[i] < 0) {
                    imageByte[i] += 256;
                }
            }

            // 构建图片在OSS中的路径
            StringBuilder filePath = new StringBuilder();
            if (StringUtils.hasText(ossFileGroup.getBaseDir())) {
                filePath.append(ossFileGroup.getBaseDir()).append("/");
            }
            filePath.append(SnowflakeUtil.nextOssFileName()).append(ext);

            client.uploadPrivate(ossFileGroup.getBucketName(), filePath.toString(), imageByte);

            OssFile ossFile = new OssFile();
            ossFile.setConfigKey(ossFileGroup.getConfigKey());
            ossFile.setGroupKey(ossFileGroup.getKey());
            ossFile.setPath(filePath.toString());
            ossFile.setSize((long) imageByte.length);
            ossFile.setExt(ext);
            ossFile.setUploaderId(uploaderId);
            ossFile.setCreateBy(uploaderId);
            ossFile.setUpdateBy(uploaderId);
            save(ossFile);

            return ossFile;
        } else {
            throw new IllegalArgumentException("上传文件失败, 不支持的文件数据.");
        }
    }

    @Override
    public File download(String fileId, String saveFile) {
        OssFile ossFile = getById(fileId);
        Assert.notNull(ossFile, "下载文件失败, 文件不存在.");
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
        Assert.notNull(ossFileGroup, "下载文件失败, 文件组不存在.");

        return download(ossFile, ossFileGroup, saveFile);
    }

    @Override
    public File download(OssFile ossFile, OssFileGroup ossFileGroup, String saveFile) {
        StrixOssClient client = strixOssConfig.getInstance(ossFileGroup.getConfigKey());
        Assert.notNull(client, "上传文件失败. OSS服务实例不存在");

        return client.downloadPrivate(ossFileGroup.getBucketName(), ossFile.getPath(), saveFile);
    }

    @Override
    public File download(String fileId, String saveFile, Integer downloaderType, String downloaderId) {
        OssFile ossFile = getById(fileId);
        Assert.notNull(ossFile, "下载文件失败, 文件不存在.");
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
        Assert.notNull(ossFileGroup, "下载文件失败, 文件组不存在.");

        Assert.isTrue(checkPermission(ossFile, ossFileGroup, downloaderType, downloaderId), "下载文件失败, 文件不存在.");

        return download(ossFile, ossFileGroup, saveFile);
    }

    @Override
    public void delete(String fileId) {
        delete(fileId, null, null);
    }

    @Override
    public void delete(String fileId, Integer downloaderType, String downloaderId) {
        try {
            OssFile ossFile = getById(fileId);
            Assert.notNull(ossFile, "删除文件失败, 文件不存在.");
            OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
            Assert.notNull(ossFileGroup, "删除文件失败, 文件组不存在.");
            if (downloaderType != null && downloaderId != null) {
                Assert.isTrue(checkPermission(ossFile, ossFileGroup, downloaderType, downloaderId), "删除文件失败, 文件不存在.");
            }
            StrixOssClient client = strixOssConfig.getInstance(ossFileGroup.getConfigKey());
            Assert.notNull(client, "上传文件失败. OSS服务实例不存在");
            client.deletePrivate(ossFileGroup.getBucketName(), ossFile.getPath());
        } catch (Exception e) {
            log.error("删除文件失败", e);
        }

        removeById(fileId);
    }

    @Override
    public boolean checkPermission(OssFile ossFile, OssFileGroup ossFileGroup, Integer downloaderType, String downloaderId) {
        if (StrixOssFileGroupSecretType.MANAGER == ossFileGroup.getSecretType() && StrixOssFileGroupSecretType.MANAGER == downloaderType) {
            // 文件要求管理员权限 且下载用户为管理员 ACCEPT
            return true;
        } else if (StrixOssFileGroupSecretType.USER == ossFileGroup.getSecretType() && StrixOssFileGroupSecretType.MANAGER == downloaderType) {
            // 文件要求用户权限 且下载用户为管理员 ACCEPT
            return true;
        } else if (StrixOssFileGroupSecretType.USER == ossFileGroup.getSecretType() && StrixOssFileGroupSecretType.USER == downloaderType) {
            // 文件要求用户权限 且下载用户为用户 ACCEPT
            // 要求下载用户为上传用户
            return Objects.equals(downloaderId, ossFile.getUploaderId());
        } else {
            return false;
        }
    }

}
