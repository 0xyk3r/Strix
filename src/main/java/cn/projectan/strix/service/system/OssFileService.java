package cn.projectan.strix.service.system;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.mapper.system.OssFileMapper;
import cn.projectan.strix.model.db.system.OssFile;
import cn.projectan.strix.model.db.system.OssFileGroup;
import cn.projectan.strix.model.dict.system.OssFileGroupSecretType;
import cn.projectan.strix.model.request.system.module.oss.OssFileListReq;
import cn.projectan.strix.util.common.SnowflakeUtil;
import cn.projectan.strix.util.file.FileMagicValidator;
import cn.projectan.strix.util.file.FileUtil;
import cn.projectan.strix.util.http.ServletUtil;
import cn.projectan.strix.util.text.RegexPatterns;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * <p>
 * Strix OSS 文件 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2022-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssFileService extends ServiceImpl<OssFileMapper, OssFile> {

    private final Optional<StrixOssStore> strixOssStore;
    private final OssFileGroupService ossFileGroupService;

    /**
     * 分页查询存储文件列表
     *
     * @param req 查询请求
     * @return 分页数据
     */
    public Page<OssFile> listPage(OssFileListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), OssFile::getPath, req.getKeyword())
                .eq(StringUtils.hasText(req.getConfigKey()), OssFile::getConfigKey, req.getConfigKey())
                .eq(StringUtils.hasText(req.getGroupKey()), OssFile::getGroupKey, req.getGroupKey())
                .page(req.getPage());
    }


    /**
     * 获取文件访问URL (无权限校验)
     *
     * @param fileId     文件ID
     * @param defaultUrl 默认URL
     * @return 文件访问URL
     */
    public String getUrl(String fileId, String defaultUrl) {
        try {
            OssFile ossFile = getById(fileId);
            Assert.notNull(ossFile, "下载文件失败, 文件不存在.");
            OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
            Assert.notNull(ossFileGroup, "下载文件失败, 文件组不存在.");

            return getUrl(ossFile, ossFileGroup, defaultUrl);
        } catch (Exception e) {
            log.warn("获取文件URL失败, 使用默认URL. fileId={}", fileId, e);
            return defaultUrl;
        }
    }

    /**
     * 获取文件访问URL
     *
     * @param fileId         文件ID
     * @param downloaderType 下载者类型 见{@link OssFileGroupSecretType OssFileGroupSecretType}
     * @param downloaderId   下载者ID
     * @param defaultUrl     默认URL
     * @return 文件访问URL
     */
    public String getUrl(String fileId, Short downloaderType, String downloaderId, String defaultUrl) {
        try {
            OssFile ossFile = getById(fileId);
            Assert.notNull(ossFile, "下载文件失败, 文件不存在.");
            OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
            Assert.notNull(ossFileGroup, "下载文件失败, 文件组不存在.");

            Assert.isTrue(checkPermission(ossFile, ossFileGroup, downloaderType, downloaderId), "下载文件失败, 文件不存在.");

            return getUrl(ossFile, ossFileGroup, defaultUrl);
        } catch (Exception e) {
            log.warn("获取文件URL失败, 使用默认URL. fileId={}", fileId, e);
            return defaultUrl;
        }
    }

    /**
     * 获取文件访问URL (无权限校验)
     *
     * @param ossFile      文件
     * @param ossFileGroup 文件组
     * @param defaultUrl   默认URL
     * @return 文件访问URL
     */
    public String getUrl(OssFile ossFile, OssFileGroup ossFileGroup, String defaultUrl) {
        try {
            StrixOssClient client = getOssClient(ossFileGroup.getConfigKey());

            String url = client.getPublic().signDownloadUrl(ossFileGroup.getBucketName(), ossFile.getPath(), 300 * 1000);
            // 处理自定义域名
            if (StringUtils.hasText(url) && StringUtils.hasText(ossFileGroup.getBucketDomain())) {
                Matcher matcher = RegexPatterns.DOMAIN_PATTERN.matcher(url);
                if (matcher.find()) {
                    url = matcher.replaceAll(ossFileGroup.getBucketDomain());
                    return url;
                }
            }

            return StringUtils.hasText(url) ? url : defaultUrl;
        } catch (Exception e) {
            log.warn("获取文件URL失败, 使用默认URL. fileId={}", ossFile.getId(), e);
            return defaultUrl;
        }
    }

    /**
     * 上传文件
     *
     * @param groupKey 文件组 key
     * @param file     文件
     * @return 上传成功的文件信息
     */
    public OssFile upload(String groupKey, File file) {
        return upload(groupKey, file, file.getName());
    }

    /**
     * 上传文件
     *
     * @param groupKey     文件组 key
     * @param file         文件
     * @param originalName 原始文件名
     * @return 上传成功的文件信息
     */
    public OssFile upload(String groupKey, File file, String originalName) {
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(groupKey);
        Assert.notNull(ossFileGroup, "上传文件失败. 文件组不存在");
        StrixOssClient client = getOssClient(ossFileGroup.getConfigKey());

        String ext = FileUtil.getExtension(originalName);
        validateExtension(ossFileGroup, ext);
        try (InputStream is = new FileInputStream(file)) {
            validateFileMagic(is, ext);
        } catch (IOException e) {
            throw new StrixException("上传文件失败. 文件读取异常", e);
        }

        String filePath = buildFilePath(ossFileGroup, originalName);

        try {
            client.getPrivate().upload(ossFileGroup.getBucketName(), filePath, file);
        } catch (Exception e) {
            throw new StrixException("上传文件失败. 文件上传异常", e);
        }

        return saveOssFile(ossFileGroup, filePath, file.length(), ext);
    }

    /**
     * 上传文件
     *
     * @param groupKey 文件组 key
     * @param file     文件
     * @return 上传成功的文件信息
     */
    public OssFile upload(String groupKey, MultipartFile file) {
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(groupKey);
        Assert.notNull(ossFileGroup, "上传文件失败. 文件组不存在");
        StrixOssClient client = getOssClient(ossFileGroup.getConfigKey());

        String ext = FileUtil.getExtension(file.getOriginalFilename());
        validateExtension(ossFileGroup, ext);

        String filePath = buildFilePath(ossFileGroup, file.getOriginalFilename());

        try (InputStream is = file.getInputStream()) {
            validateFileMagic(is, ext);
            client.getPrivate().upload(ossFileGroup.getBucketName(), filePath, is, file.getSize());
        } catch (IOException e) {
            throw new StrixException("上传文件失败. 文件上传异常", e);
        }

        return saveOssFile(ossFileGroup, filePath, file.getSize(), ext);
    }

    /**
     * 上传文件 (输入流方式)
     *
     * @param groupKey      文件组 key
     * @param inputStream   输入流
     * @param contentLength 内容长度
     * @param originalName  原始文件名
     * @return 上传成功的文件信息
     */
    public OssFile upload(String groupKey, InputStream inputStream, long contentLength, String originalName) {
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(groupKey);
        Assert.notNull(ossFileGroup, "上传文件失败. 文件组不存在");
        StrixOssClient client = getOssClient(ossFileGroup.getConfigKey());

        String ext = FileUtil.getExtension(originalName);
        validateExtension(ossFileGroup, ext);
        validateFileMagic(inputStream, ext);

        String filePath = buildFilePath(ossFileGroup, originalName);

        try {
            client.getPrivate().upload(ossFileGroup.getBucketName(), filePath, inputStream, contentLength);
        } catch (Exception e) {
            throw new StrixException("上传文件失败. 文件上传异常", e);
        }

        return saveOssFile(ossFileGroup, filePath, contentLength, ext);
    }

    /**
     * 上传文件 (字节数组方式)
     *
     * @param groupKey     文件组 key
     * @param data         文件字节数组
     * @param originalName 原始文件名
     * @return 上传成功的文件信息
     */
    public OssFile upload(String groupKey, byte[] data, String originalName) {
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(groupKey);
        Assert.notNull(ossFileGroup, "上传文件失败. 文件组不存在");
        StrixOssClient client = getOssClient(ossFileGroup.getConfigKey());

        String ext = FileUtil.getExtension(originalName);
        validateExtension(ossFileGroup, ext);
        try (InputStream is = new ByteArrayInputStream(data)) {
            validateFileMagic(is, ext);
        } catch (IOException e) {
            throw new StrixException("上传文件失败. 文件读取异常", e);
        }

        String filePath = buildFilePath(ossFileGroup, originalName);

        try {
            client.getPrivate().upload(ossFileGroup.getBucketName(), filePath, data);
        } catch (Exception e) {
            throw new StrixException("上传文件失败. 文件上传异常", e);
        }

        return saveOssFile(ossFileGroup, filePath, (long) data.length, ext);
    }

    /**
     * 下载文件 (无权限校验)
     *
     * @param fileId   文件ID
     * @param saveFile 保存文件路径
     * @return 保存的文件
     */
    public File download(String fileId, String saveFile) {
        OssFile ossFile = getById(fileId);
        Assert.notNull(ossFile, "下载文件失败, 文件不存在.");
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
        Assert.notNull(ossFileGroup, "下载文件失败, 文件组不存在.");

        return download(ossFile, ossFileGroup, saveFile);
    }

    /**
     * 下载文件
     * <p>不推荐直接使用 请使用 {@link #download(String, String)} 或 {@link #download(String, String, Short, String)}
     *
     * @param ossFile      文件
     * @param ossFileGroup 文件组
     * @param saveFile     保存文件路径
     * @return 保存的文件
     */
    public File download(OssFile ossFile, OssFileGroup ossFileGroup, String saveFile) {
        StrixOssClient client = getOssClient(ossFileGroup.getConfigKey());

        return client.getPrivate().download(ossFileGroup.getBucketName(), ossFile.getPath(), saveFile);
    }

    /**
     * 下载文件
     *
     * @param fileId         文件ID
     * @param saveFile       保存文件路径
     * @param downloaderType 下载者类型 见{@link OssFileGroupSecretType OssFileGroupSecretType}
     * @param downloaderId   下载者ID
     * @return 保存的文件
     */
    public File download(String fileId, String saveFile, Short downloaderType, String downloaderId) {
        OssFile ossFile = getById(fileId);
        Assert.notNull(ossFile, "下载文件失败, 文件不存在.");
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
        Assert.notNull(ossFileGroup, "下载文件失败, 文件组不存在.");

        Assert.isTrue(checkPermission(ossFile, ossFileGroup, downloaderType, downloaderId), "下载文件失败, 文件不存在.");

        return download(ossFile, ossFileGroup, saveFile);
    }

    /**
     * 下载文件为输入流 (无权限校验)
     * <p>注意: 调用者需要负责关闭返回的输入流
     *
     * @param fileId 文件ID
     * @return 文件输入流
     */
    public InputStream downloadAsStream(String fileId) {
        FileAndGroup fileAndGroup = getFileAndGroup(fileId, "下载文件失败");
        StrixOssClient client = getOssClient(fileAndGroup.ossFileGroup().getConfigKey());
        return client.getPrivate().downloadAsStream(fileAndGroup.ossFileGroup().getBucketName(), fileAndGroup.ossFile().getPath());
    }

    /**
     * 下载文件为输入流
     * <p>注意: 调用者需要负责关闭返回的输入流
     *
     * @param fileId         文件ID
     * @param downloaderType 下载者类型 见{@link OssFileGroupSecretType OssFileGroupSecretType}
     * @param downloaderId   下载者ID
     * @return 文件输入流
     */
    public InputStream downloadAsStream(String fileId, Short downloaderType, String downloaderId) {
        FileAndGroup fileAndGroup = getFileAndGroup(fileId, "下载文件失败");
        Assert.isTrue(checkPermission(fileAndGroup.ossFile(), fileAndGroup.ossFileGroup(), downloaderType, downloaderId), "下载文件失败, 文件不存在.");

        StrixOssClient client = getOssClient(fileAndGroup.ossFileGroup().getConfigKey());
        return client.getPrivate().downloadAsStream(fileAndGroup.ossFileGroup().getBucketName(), fileAndGroup.ossFile().getPath());
    }

    /**
     * 下载文件到输出流 (无权限校验)
     *
     * @param fileId       文件ID
     * @param outputStream 输出流
     */
    public void downloadToStream(String fileId, OutputStream outputStream) {
        FileAndGroup fileAndGroup = getFileAndGroup(fileId, "下载文件失败");
        StrixOssClient client = getOssClient(fileAndGroup.ossFileGroup().getConfigKey());
        client.getPrivate().downloadToStream(fileAndGroup.ossFileGroup().getBucketName(), fileAndGroup.ossFile().getPath(), outputStream);
    }

    /**
     * 下载文件到输出流
     *
     * @param fileId         文件ID
     * @param outputStream   输出流
     * @param downloaderType 下载者类型 见{@link OssFileGroupSecretType OssFileGroupSecretType}
     * @param downloaderId   下载者ID
     */
    public void downloadToStream(String fileId, OutputStream outputStream, Short downloaderType, String downloaderId) {
        FileAndGroup fileAndGroup = getFileAndGroup(fileId, "下载文件失败");
        Assert.isTrue(checkPermission(fileAndGroup.ossFile(), fileAndGroup.ossFileGroup(), downloaderType, downloaderId), "下载文件失败, 文件不存在.");

        StrixOssClient client = getOssClient(fileAndGroup.ossFileGroup().getConfigKey());
        client.getPrivate().downloadToStream(fileAndGroup.ossFileGroup().getBucketName(), fileAndGroup.ossFile().getPath(), outputStream);
    }

    /**
     * 获取客户端流式下载响应体 (无权限校验)
     * <p>适用于 Spring MVC 控制器返回流式响应
     *
     * @param fileId 文件ID
     * @return StreamingResponseBody
     */
    public StreamingResponseBody getStreamingDownload(String fileId) {
        FileAndGroup fileAndGroup = getFileAndGroup(fileId, "下载文件失败");
        StrixOssClient client = getOssClient(fileAndGroup.ossFileGroup().getConfigKey());

        HttpServletResponse response = ServletUtil.getResponse();

        return client.getPrivate().downloadStream(fileAndGroup.ossFileGroup().getBucketName(), fileAndGroup.ossFile().getPath(), response);
    }

    /**
     * 获取客户端流式下载响应体
     * <p>适用于 Spring MVC 控制器返回流式响应
     *
     * @param fileId         文件ID
     * @param downloaderType 下载者类型 见{@link OssFileGroupSecretType OssFileGroupSecretType}
     * @param downloaderId   下载者ID
     * @return StreamingResponseBody
     */
    public StreamingResponseBody getStreamingDownload(String fileId, Short downloaderType, String downloaderId) {
        FileAndGroup fileAndGroup = getFileAndGroup(fileId, "下载文件失败");
        Assert.isTrue(checkPermission(fileAndGroup.ossFile(), fileAndGroup.ossFileGroup(), downloaderType, downloaderId), "下载文件失败, 文件不存在.");

        StrixOssClient client = getOssClient(fileAndGroup.ossFileGroup().getConfigKey());

        HttpServletResponse response = ServletUtil.getResponse();

        return client.getPrivate().downloadStream(fileAndGroup.ossFileGroup().getBucketName(), fileAndGroup.ossFile().getPath(), response);
    }

    /**
     * 删除文件 (无权限校验)
     *
     * @param fileId 文件ID
     */
    public void delete(String fileId) {
        delete(fileId, null, null);
    }

    /**
     * 删除文件
     *
     * @param fileId         文件ID
     * @param downloaderType 下载者类型 见{@link OssFileGroupSecretType OssFileGroupSecretType}
     * @param downloaderId   下载者ID
     */
    public void delete(String fileId, Short downloaderType, String downloaderId) {
        try {
            OssFile ossFile = getById(fileId);
            Assert.notNull(ossFile, "删除文件失败, 文件不存在.");
            OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
            Assert.notNull(ossFileGroup, "删除文件失败, 文件组不存在.");

            if (downloaderType != null && downloaderId != null) {
                Assert.isTrue(checkPermission(ossFile, ossFileGroup, downloaderType, downloaderId), "删除文件失败, 文件不存在.");
            }

            StrixOssClient client = getOssClient(ossFileGroup.getConfigKey());
            client.getPrivate().delete(ossFileGroup.getBucketName(), ossFile.getPath());
        } catch (Exception e) {
            log.error("删除文件失败", e);
        }

        removeById(fileId);
    }

    /**
     * 检查访问权限
     *
     * @param ossFile        文件
     * @param ossFileGroup   文件组
     * @param downloaderType 下载者类型 见{@link OssFileGroupSecretType OssFileGroupSecretType}
     * @param downloaderId   下载者ID
     * @return 是否有权限
     */
    public boolean checkPermission(OssFile ossFile, OssFileGroup ossFileGroup, Short downloaderType, String downloaderId) {
        if (OssFileGroupSecretType.MANAGER == ossFileGroup.getSecretType() && OssFileGroupSecretType.MANAGER == downloaderType) {
            // 文件要求管理员权限 且下载用户为管理员 ACCEPT
            return true;
        } else if (OssFileGroupSecretType.USER == ossFileGroup.getSecretType() && OssFileGroupSecretType.MANAGER == downloaderType) {
            // 文件要求用户权限 且下载用户为管理员 ACCEPT
            return true;
        } else if (OssFileGroupSecretType.USER == ossFileGroup.getSecretType() && OssFileGroupSecretType.USER == downloaderType) {
            // 文件要求用户权限 且下载用户为用户 ACCEPT
            // 要求下载用户为上传用户
            return Objects.equals(downloaderId, ossFile.getCreatedBy());
        } else {
            return false;
        }
    }

    /**
     * 获取 OSS 客户端，若 OSS 模块未启用则抛出异常
     */
    private StrixOssClient getOssClient(String configKey) {
        StrixOssStore store = strixOssStore.orElseThrow(
                () -> new StrixException("Strix OSS 模块未启用，请检查配置.")
        );
        StrixOssClient client = store.getInstance(configKey);
        Assert.notNull(client, "Strix OSS 服务实例不存在: " + configKey);
        return client;
    }

    // ======================== 辅助方法 ========================

    /**
     * 验证文件扩展名是否允许
     *
     * @param ossFileGroup 文件组
     * @param ext          扩展名
     */
    private void validateExtension(OssFileGroup ossFileGroup, String ext) {
        List<String> allowExtSet = Arrays.asList(ossFileGroup.getAllowExtension().split(","));
        Assert.isTrue(allowExtSet.contains(ext), "上传文件失败, 不支持的文件格式.");
    }

    /**
     * 验证文件内容头是否与扩展名一致
     */
    private void validateFileMagic(InputStream inputStream, String ext) {
        boolean valid = FileMagicValidator.validate(inputStream, ext);
        Assert.isTrue(valid, "上传文件失败, 文件内容与扩展名不匹配.");
    }

    /**
     * 构建文件在 OSS 中的存储路径
     *
     * @param ossFileGroup 文件组
     * @param originalName 原始文件名
     * @return 文件存储路径
     */
    private String buildFilePath(OssFileGroup ossFileGroup, String originalName) {
        StringBuilder filePath = new StringBuilder();
        if (StringUtils.hasText(ossFileGroup.getBaseDir())) {
            filePath.append(ossFileGroup.getBaseDir()).append("/");
        }
        filePath.append(SnowflakeUtil.nextOssFileName());
        if (StringUtils.hasText(originalName)) {
            filePath.append("_").append(originalName);
        }
        return filePath.toString();
    }

    /**
     * 保存文件信息到数据库
     *
     * @param ossFileGroup 文件组
     * @param filePath     文件路径
     * @param size         文件大小
     * @param ext          扩展名
     * @return 保存的文件信息
     */
    private OssFile saveOssFile(OssFileGroup ossFileGroup, String filePath, Long size, String ext) {
        OssFile ossFile = new OssFile()
                .setConfigKey(ossFileGroup.getConfigKey())
                .setGroupKey(ossFileGroup.getKey())
                .setPath(filePath)
                .setSize(size)
                .setExt(ext);
        Assert.isTrue(save(ossFile), "上传文件失败, 保存文件信息失败.");
        return ossFile;
    }

    /**
     * 获取文件信息和文件组信息
     *
     * @param fileId      文件ID
     * @param errorPrefix 错误消息前缀
     * @return 文件信息和文件组信息
     */
    private FileAndGroup getFileAndGroup(String fileId, String errorPrefix) {
        OssFile ossFile = getById(fileId);
        Assert.notNull(ossFile, errorPrefix + ", 文件不存在.");
        OssFileGroup ossFileGroup = ossFileGroupService.getGroupByKey(ossFile.getGroupKey());
        Assert.notNull(ossFileGroup, errorPrefix + ", 文件组不存在.");
        return new FileAndGroup(ossFile, ossFileGroup);
    }

    /**
     * 文件与文件组信息
     */
    private record FileAndGroup(OssFile ossFile, OssFileGroup ossFileGroup) {
    }

}
