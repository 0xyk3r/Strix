package cn.projectan.strix.service.system;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.model.db.system.OssFile;
import cn.projectan.strix.model.db.system.OssFileGroup;
import cn.projectan.strix.model.request.system.module.oss.*;
import cn.projectan.strix.model.response.system.module.oss.OssFileArchiveResp;
import cn.projectan.strix.model.response.system.module.oss.OssFileBrowseResp;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.SnowflakeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssFileBrowseService {

    private final OssFileService ossFileService;
    private final OssFileGroupService ossFileGroupService;
    private final Optional<StrixOssStore> strixOssStore;

    private static final String KEEP_FILE = ".keep";
    private static final long ARCHIVE_MAX_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long PREVIEW_URL_EXPIRES = TimeUnit.HOURS.toMillis(1);
    private static final int ARCHIVE_MAX_ENTRIES = 10_000;

    /**
     * 浏览指定前缀下的文件和目录
     */
    public OssFileBrowseResp browse(OssFileBrowseReq req) {
        OssFileGroup group = ossFileGroupService.getGroupByKey(req.getGroupKey());
        Assert.notNull(group, I18nUtil.get("assert.oss.fileGroupNotExist"));

        String baseDir = StringUtils.hasText(group.getBaseDir()) ? group.getBaseDir() + "/" : "";
        String fullPrefix = baseDir + (StringUtils.hasText(req.getPrefix()) ? req.getPrefix() : "");

        // Query all files under this group with the given prefix
        List<OssFile> allFiles;
        if (StringUtils.hasText(req.getKeyword())) {
            // Global search mode: search across all files in the group
            allFiles = ossFileService.lambdaQuery()
                    .eq(OssFile::getGroupKey, req.getGroupKey())
                    .and(w -> w
                            .like(OssFile::getOriginalName, req.getKeyword())
                            .or()
                            .like(OssFile::getPath, req.getKeyword()))
                    .list();
        } else {
            allFiles = ossFileService.lambdaQuery()
                    .eq(OssFile::getGroupKey, req.getGroupKey())
                    .likeRight(OssFile::getPath, escapeLike(fullPrefix))
                    .list();
        }

        // Parse into directories and files at the current level
        Set<String> dirNames = new LinkedHashSet<>();
        List<OssFile> currentLevelFiles = new ArrayList<>();
        Map<String, Long> dirFileCounts = new HashMap<>();

        for (OssFile file : allFiles) {
            if (!file.getPath().startsWith(fullPrefix)) continue;
            String relativePath = file.getPath().substring(fullPrefix.length());

            int slashIndex = relativePath.indexOf('/');
            if (slashIndex >= 0) {
                // This file is in a subdirectory
                String dirName = relativePath.substring(0, slashIndex);
                dirNames.add(dirName);
                dirFileCounts.merge(dirName, 1L, Long::sum);
            } else if (!relativePath.equals(KEEP_FILE) && !relativePath.isEmpty()) {
                // This file is at the current level (exclude .keep files)
                currentLevelFiles.add(file);
            }
        }

        // Also detect directories from .keep files
        for (OssFile file : allFiles) {
            String path = file.getPath();
            if (!path.startsWith(fullPrefix) || !path.endsWith("/" + KEEP_FILE)) continue;
            String relDir = path.substring(fullPrefix.length());
            int slashIdx = relDir.indexOf('/');
            if (slashIdx >= 0) {
                dirNames.add(relDir.substring(0, slashIdx));
            }
        }

        // Sort current-level files
        sortFiles(currentLevelFiles, req.getSortBy(), req.getSortOrder());

        // Build directory items
        String prefix = StringUtils.hasText(req.getPrefix()) ? req.getPrefix() : "";
        List<OssFileBrowseResp.DirectoryItem> directories = dirNames.stream()
                .sorted()
                .map(name -> new OssFileBrowseResp.DirectoryItem(
                        name,
                        prefix + name + "/",
                        dirFileCounts.getOrDefault(name, 0L)
                ))
                .collect(Collectors.toList());

        // Build file items
        List<OssFileBrowseResp.FileItem> files = currentLevelFiles.stream()
                .map(f -> new OssFileBrowseResp.FileItem(
                        f.getId(), f.getOriginalName(), f.getPath(),
                        f.getSize(), f.getExt(), f.getContentType(),
                        f.getCreatedTime(), f.getCreatedBy()
                ))
                .collect(Collectors.toList());

        // Build breadcrumb from prefix
        List<String> breadcrumb = new ArrayList<>();
        if (StringUtils.hasText(req.getPrefix())) {
            String trimmed = req.getPrefix().endsWith("/")
                    ? req.getPrefix().substring(0, req.getPrefix().length() - 1)
                    : req.getPrefix();
            breadcrumb.addAll(Arrays.asList(trimmed.split("/")));
        }

        return new OssFileBrowseResp(req.getGroupKey(), prefix, directories, files, breadcrumb);
    }

    /**
     * 创建空目录 (通过 .keep 占位文件)
     */
    @Transactional(rollbackFor = Exception.class)
    public void mkdir(OssFileMkdirReq req) {
        OssFileGroup group = ossFileGroupService.getGroupByKey(req.getGroupKey());
        Assert.notNull(group, I18nUtil.get("assert.oss.fileGroupNotExist"));

        String baseDir = StringUtils.hasText(group.getBaseDir()) ? group.getBaseDir() + "/" : "";
        String parentPrefix = StringUtils.hasText(req.getParentPrefix()) ? req.getParentPrefix() : "";

        // Path traversal guard
        Assert.isTrue(!req.getDirName().contains("/") && !req.getDirName().contains(".."),
                I18nUtil.get("assert.oss.invalidDirName"));

        String keepPath = baseDir + parentPrefix + req.getDirName() + "/" + KEEP_FILE;

        // Check if directory already exists
        boolean exists = ossFileService.lambdaQuery()
                .eq(OssFile::getGroupKey, req.getGroupKey())
                .likeRight(OssFile::getPath, escapeLike(baseDir + parentPrefix + req.getDirName() + "/"))
                .exists();
        Assert.isTrue(!exists, I18nUtil.get("assert.oss.dirAlreadyExists"));

        // Upload .keep placeholder to OSS
        StrixOssClient client = getOssClient(group.getConfigKey());
        StrixOssClient.Operations ops = client.getPublic();
        ops.upload(group.getBucketName(), keepPath, new byte[0]);

        // Save .keep file record
        OssFile keepFile = new OssFile()
                .setConfigKey(group.getConfigKey())
                .setGroupKey(group.getKey())
                .setPath(keepPath)
                .setSize(0L)
                .setExt(".keep")
                .setOriginalName(KEEP_FILE)
                .setContentType("application/octet-stream");
        ossFileService.save(keepFile);
    }

    /**
     * 重命名文件 (OSS copy + delete + DB update)
     */
    @Transactional(rollbackFor = Exception.class)
    public void rename(OssFileRenameReq req) {
        OssFile file = ossFileService.getById(req.getFileId());
        Assert.notNull(file, I18nUtil.get("assert.oss.fileNotExist"));

        OssFileGroup group = ossFileGroupService.getGroupByKey(file.getGroupKey());
        Assert.notNull(group, I18nUtil.get("assert.oss.fileGroupNotExist"));

        // Build new path: replace file name portion in the path
        String oldPath = file.getPath();
        String directory = oldPath.contains("/") ? oldPath.substring(0, oldPath.lastIndexOf('/') + 1) : "";
        String newFileName = SnowflakeUtil.nextOssFileName() + "_" + req.getNewName();
        String newPath = directory + newFileName;

        // Determine new extension
        String newExt = req.getNewName().contains(".")
                ? req.getNewName().substring(req.getNewName().lastIndexOf('.'))
                : file.getExt();

        // OSS: copy to new path, delete old
        StrixOssClient client = getOssClient(file.getConfigKey());
        StrixOssClient.Operations ops = client.getPublic();
        ops.copy(group.getBucketName(), oldPath, newPath);
        ops.delete(group.getBucketName(), oldPath);

        // DB: update path and originalName
        ossFileService.lambdaUpdate()
                .eq(OssFile::getId, file.getId())
                .set(OssFile::getPath, newPath)
                .set(OssFile::getOriginalName, req.getNewName())
                .set(OssFile::getExt, newExt)
                .update();
    }

    /**
     * 移动文件到目标前缀 (OSS copy + delete + DB update)
     */
    @Transactional(rollbackFor = Exception.class)
    public void move(OssFileMoveReq req) {
        OssFileGroup targetGroup = ossFileGroupService.getGroupByKey(req.getTargetGroupKey());
        Assert.notNull(targetGroup, I18nUtil.get("assert.oss.fileGroupNotExist"));

        String targetBaseDir = StringUtils.hasText(targetGroup.getBaseDir()) ? targetGroup.getBaseDir() + "/" : "";
        String targetPrefix = StringUtils.hasText(req.getTargetPrefix()) ? req.getTargetPrefix() : "";

        // Pre-fetch all files and unique groups to avoid N+1
        List<OssFile> files = ossFileService.lambdaQuery()
                .in(OssFile::getId, req.getFileIds())
                .list();
        Map<String, OssFileGroup> groupCache = files.stream()
                .map(OssFile::getGroupKey).distinct()
                .collect(Collectors.toMap(k -> k, k -> ossFileGroupService.getGroupByKey(k)));

        for (OssFile file : files) {
            OssFileGroup sourceGroup = groupCache.get(file.getGroupKey());
            if (sourceGroup == null) continue;

            // Build new path
            String fileName = file.getPath().contains("/")
                    ? file.getPath().substring(file.getPath().lastIndexOf('/') + 1)
                    : file.getPath();
            String newPath = targetBaseDir + targetPrefix + fileName;

            // OSS: copy (handle cross-config) + delete
            if (file.getConfigKey().equals(targetGroup.getConfigKey())) {
                StrixOssClient client = getOssClient(file.getConfigKey());
                client.getPublic().copy(sourceGroup.getBucketName(), file.getPath(), newPath);
                client.getPublic().delete(sourceGroup.getBucketName(), file.getPath());
            } else {
                StrixOssClient sourceClient = getOssClient(file.getConfigKey());
                StrixOssClient targetClient = getOssClient(targetGroup.getConfigKey());
                try (InputStream is = sourceClient.getPublic().downloadAsStream(sourceGroup.getBucketName(), file.getPath())) {
                    targetClient.getPublic().upload(targetGroup.getBucketName(), newPath, is.readAllBytes());
                } catch (Exception e) {
                    throw new StrixException(I18nUtil.get("error.oss.crossConfigMoveFailed"));
                }
                sourceClient.getPublic().delete(sourceGroup.getBucketName(), file.getPath());
            }

            // DB update
            ossFileService.lambdaUpdate()
                    .eq(OssFile::getId, file.getId())
                    .set(OssFile::getPath, newPath)
                    .set(OssFile::getGroupKey, req.getTargetGroupKey())
                    .update();
        }
    }

    /**
     * 复制文件到目标前缀 (OSS copy + new DB record)
     */
    @Transactional(rollbackFor = Exception.class)
    public void copy(OssFileCopyReq req) {
        OssFileGroup targetGroup = ossFileGroupService.getGroupByKey(req.getTargetGroupKey());
        Assert.notNull(targetGroup, I18nUtil.get("assert.oss.fileGroupNotExist"));

        String targetBaseDir = StringUtils.hasText(targetGroup.getBaseDir()) ? targetGroup.getBaseDir() + "/" : "";
        String targetPrefix = StringUtils.hasText(req.getTargetPrefix()) ? req.getTargetPrefix() : "";

        // Pre-fetch all files and unique groups to avoid N+1
        List<OssFile> files = ossFileService.lambdaQuery()
                .in(OssFile::getId, req.getFileIds())
                .list();
        Map<String, OssFileGroup> groupCache = files.stream()
                .map(OssFile::getGroupKey).distinct()
                .collect(Collectors.toMap(k -> k, k -> ossFileGroupService.getGroupByKey(k)));

        for (OssFile file : files) {
            OssFileGroup sourceGroup = groupCache.get(file.getGroupKey());
            if (sourceGroup == null) continue;

            // Build new path with new snowflake ID
            String originalName = StringUtils.hasText(file.getOriginalName())
                    ? file.getOriginalName() : "file" + file.getExt();
            String newFileName = SnowflakeUtil.nextOssFileName() + "_" + originalName;
            String newPath = targetBaseDir + targetPrefix + newFileName;

            // OSS: copy (handle cross-config)
            if (file.getConfigKey().equals(targetGroup.getConfigKey())) {
                getOssClient(file.getConfigKey()).getPublic()
                        .copy(sourceGroup.getBucketName(), file.getPath(), newPath);
            } else {
                StrixOssClient sourceClient = getOssClient(file.getConfigKey());
                StrixOssClient targetClient = getOssClient(targetGroup.getConfigKey());
                try (InputStream is = sourceClient.getPublic().downloadAsStream(sourceGroup.getBucketName(), file.getPath())) {
                    targetClient.getPublic().upload(targetGroup.getBucketName(), newPath, is.readAllBytes());
                } catch (Exception e) {
                    throw new StrixException(I18nUtil.get("error.oss.crossConfigMoveFailed"));
                }
            }

            // DB: create new record
            OssFile newFile = new OssFile()
                    .setConfigKey(targetGroup.getConfigKey())
                    .setGroupKey(req.getTargetGroupKey())
                    .setPath(newPath)
                    .setSize(file.getSize())
                    .setExt(file.getExt())
                    .setOriginalName(file.getOriginalName())
                    .setContentType(file.getContentType());
            ossFileService.save(newFile);
        }
    }

    /**
     * 批量删除文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchRemove(OssFileBatchRemoveReq req) {
        // Pre-fetch all files and unique groups to avoid N+1
        List<OssFile> files = ossFileService.lambdaQuery()
                .in(OssFile::getId, req.getFileIds())
                .list();
        Map<String, OssFileGroup> groupCache = files.stream()
                .map(OssFile::getGroupKey).distinct()
                .collect(Collectors.toMap(k -> k, k -> ossFileGroupService.getGroupByKey(k)));

        List<String> deletedIds = new ArrayList<>();
        for (OssFile file : files) {
            OssFileGroup group = groupCache.get(file.getGroupKey());
            if (group == null) continue;

            // OSS: delete
            StrixOssClient client = getOssClient(file.getConfigKey());
            try {
                client.getPublic().delete(group.getBucketName(), file.getPath());
            } catch (Exception e) {
                log.warn("OSS 文件删除失败, 继续删除数据库记录: fileId={}, path={}", file.getId(), file.getPath(), e);
            }
            deletedIds.add(file.getId());
        }

        // DB: batch soft delete
        if (!deletedIds.isEmpty()) {
            ossFileService.removeByIds(deletedIds);
        }
    }

    /**
     * 获取文件预览签名 URL
     */
    public String getPreviewUrl(String fileId) {
        OssFile file = ossFileService.getById(fileId);
        Assert.notNull(file, I18nUtil.get("assert.oss.fileNotExist"));

        OssFileGroup group = ossFileGroupService.getGroupByKey(file.getGroupKey());
        Assert.notNull(group, I18nUtil.get("assert.oss.fileGroupNotExist"));

        StrixOssClient client = getOssClient(file.getConfigKey());
        StrixOssClient.Operations ops = client.getPublic();
        return ops.signDownloadUrl(group.getBucketName(), file.getPath(), PREVIEW_URL_EXPIRES);
    }

    /**
     * 列出压缩包内容
     */
    public OssFileArchiveResp listArchiveContents(String fileId) {
        OssFile file = ossFileService.getById(fileId);
        Assert.notNull(file, I18nUtil.get("assert.oss.fileNotExist"));

        // Size check
        if (file.getSize() != null && file.getSize() > ARCHIVE_MAX_SIZE) {
            throw new StrixException(I18nUtil.get("error.oss.archiveTooLarge"));
        }

        OssFileGroup group = ossFileGroupService.getGroupByKey(file.getGroupKey());
        Assert.notNull(group, I18nUtil.get("assert.oss.fileGroupNotExist"));

        StrixOssClient client = getOssClient(file.getConfigKey());
        StrixOssClient.Operations ops = client.getPublic();

        List<OssFileArchiveResp.ArchiveEntry> entries = new ArrayList<>();
        int totalFiles = 0;
        long totalSize = 0;

        // For ZIP files, use java.util.zip
        String ext = file.getExt() != null ? file.getExt().toLowerCase() : "";
        if (".zip".equals(ext)) {
            try (InputStream is = ops.downloadAsStream(group.getBucketName(), file.getPath());
                 java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entries.size() >= ARCHIVE_MAX_ENTRIES) {
                        throw new StrixException(I18nUtil.get("error.oss.archiveTooManyEntries"));
                    }
                    entries.add(new OssFileArchiveResp.ArchiveEntry(
                            entry.getName(),
                            entry.getSize() >= 0 ? entry.getSize() : 0,
                            entry.getCompressedSize() >= 0 ? entry.getCompressedSize() : 0,
                            entry.isDirectory()
                    ));
                    if (!entry.isDirectory()) {
                        totalFiles++;
                        totalSize += Math.max(entry.getSize(), 0);
                    }
                    zis.closeEntry();
                }
            } catch (Exception e) {
                log.error("解析 ZIP 压缩包失败: {}", e.getMessage(), e);
                throw new StrixException(I18nUtil.get("error.oss.archiveParseFailed"));
            }
        } else {
            // For RAR, 7z, tar, tar.gz — use Commons Compress
            try (InputStream is = ops.downloadAsStream(group.getBucketName(), file.getPath());
                 BufferedInputStream bis = new BufferedInputStream(is);
                 ArchiveInputStream<?> ais = new ArchiveStreamFactory().createArchiveInputStream(bis)) {
                ArchiveEntry entry;
                while ((entry = ais.getNextEntry()) != null) {
                    if (entries.size() >= ARCHIVE_MAX_ENTRIES) {
                        throw new StrixException(I18nUtil.get("error.oss.archiveTooManyEntries"));
                    }
                    entries.add(new OssFileArchiveResp.ArchiveEntry(
                            entry.getName(),
                            entry.getSize() >= 0 ? entry.getSize() : 0,
                            0, // compressed size not always available
                            entry.isDirectory()
                    ));
                    if (!entry.isDirectory()) {
                        totalFiles++;
                        totalSize += Math.max(entry.getSize(), 0);
                    }
                }
            } catch (Exception e) {
                log.error("解析压缩包失败: {}", e.getMessage(), e);
                throw new StrixException(I18nUtil.get("error.oss.archiveParseFailed"));
            }
        }

        return new OssFileArchiveResp(entries, totalFiles, totalSize);
    }

    // ======================== 辅助方法 ========================

    private StrixOssClient getOssClient(String configKey) {
        StrixOssStore store = strixOssStore.orElseThrow(
                () -> new StrixException(I18nUtil.get("error.oss.moduleDisabled"))
        );
        StrixOssClient client = store.getInstance(configKey);
        Assert.notNull(client, I18nUtil.get("assert.oss.instanceNotFound", configKey));
        return client;
    }

    private void sortFiles(List<OssFile> files, String sortBy, String sortOrder) {
        String effectiveSortBy = StringUtils.hasText(sortBy) ? sortBy : "name";
        boolean asc = !"desc".equalsIgnoreCase(sortOrder);

        Comparator<OssFile> comparator = switch (effectiveSortBy) {
            case "size" -> Comparator.comparing(OssFile::getSize, Comparator.nullsLast(Comparator.naturalOrder()));
            case "time" -> Comparator.comparing(OssFile::getCreatedTime, Comparator.nullsLast(Comparator.naturalOrder()));
            case "type" -> Comparator.comparing(OssFile::getExt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(
                    f -> f.getOriginalName() != null ? f.getOriginalName().toLowerCase() : "",
                    Comparator.naturalOrder()
            );
        };

        if (!asc) comparator = comparator.reversed();
        files.sort(comparator);
    }

    /**
     * Escape backslashes in LIKE pattern — MySQL treats '\' as an escape character in LIKE.
     */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\");
    }

}
