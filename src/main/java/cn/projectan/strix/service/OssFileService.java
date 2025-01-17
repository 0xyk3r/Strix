package cn.projectan.strix.service;

import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.model.db.OssFileGroup;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.File;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2022-03-09
 */
public interface OssFileService extends IService<OssFile> {

    /**
     * 获取文件访问URL (无权限校验)
     *
     * @param fileId     文件ID
     * @param defaultUrl 默认URL
     * @return 文件访问URL
     */
    String getUrl(String fileId, String defaultUrl);

    /**
     * 获取文件访问URL (无权限校验)
     * <p>不推荐直接使用 请使用 {@link #getUrl(String, String)} 或 {@link #getUrl(String, Integer, String, String)}
     *
     * @param ossFile      文件
     * @param ossFileGroup 文件组
     * @param defaultUrl   默认URL
     * @return 文件访问URL
     */
    String getUrl(OssFile ossFile, OssFileGroup ossFileGroup, String defaultUrl);

    /**
     * 获取文件访问URL
     *
     * @param fileId         文件ID
     * @param downloaderType 下载者类型 见{@link cn.projectan.strix.model.dict.StrixOssFileGroupSecretType StrixOssFileGroupSecretType}
     * @param downloaderId   下载者ID
     * @param defaultUrl     默认URL
     * @return 文件访问URL
     */
    String getUrl(String fileId, Integer downloaderType, String downloaderId, String defaultUrl);

    /**
     * 上传文件
     *
     * @param groupKey   文件组key
     * @param file       文件
     * @return 上传成功的文件信息
     */
    OssFile upload(String groupKey, File file);

    /**
     * 上传文件
     *
     * @param groupKey   文件组key
     * @param fileBase64 文件base64
     * @return 上传成功的文件信息
     */
    OssFile upload(String groupKey, String fileBase64);

    /**
     * 下载文件 (无权限校验)
     *
     * @param fileId   文件ID
     * @param saveFile 保存文件路径
     * @return 保存的文件
     */
    File download(String fileId, String saveFile);

    /**
     * 下载文件
     * <p>不推荐直接使用 请使用 {@link #download(String, String)} 或 {@link #download(String, String, Integer, String)}
     *
     * @param ossFile      文件
     * @param ossFileGroup 文件组
     * @param saveFile     保存文件路径
     * @return 保存的文件
     */
    File download(OssFile ossFile, OssFileGroup ossFileGroup, String saveFile);

    /**
     * 下载文件
     *
     * @param fileId         文件ID
     * @param saveFile       保存文件路径
     * @param downloaderType 下载者类型 见{@link cn.projectan.strix.model.dict.StrixOssFileGroupSecretType StrixOssFileGroupSecretType}
     * @param downloaderId   下载者ID
     * @return 保存的文件
     */
    File download(String fileId, String saveFile, Integer downloaderType, String downloaderId);

    /**
     * 删除文件 (无权限校验)
     *
     * @param fileId 文件ID
     */
    void delete(String fileId);

    /**
     * 删除文件
     *
     * @param fileId         文件ID
     * @param downloaderType 下载者类型 见{@link cn.projectan.strix.model.dict.StrixOssFileGroupSecretType StrixOssFileGroupSecretType}
     * @param downloaderId   下载者ID
     */
    void delete(String fileId, Integer downloaderType, String downloaderId);

    /**
     * 检查访问权限
     *
     * @param ossFile        文件
     * @param ossFileGroup   文件组
     * @param downloaderType 下载者类型 见{@link cn.projectan.strix.model.dict.StrixOssFileGroupSecretType StrixOssFileGroupSecretType}
     * @param downloaderId   下载者ID
     * @return 是否有权限
     */
    boolean checkPermission(OssFile ossFile, OssFileGroup ossFileGroup, Integer downloaderType, String downloaderId);

}
