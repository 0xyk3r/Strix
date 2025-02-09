package cn.projectan.strix.core.module.oss;

import cn.projectan.strix.model.other.module.oss.StrixOssBucket;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * Strix OSS 客户端
 *
 * @author ProjectAn
 * @since 2023/5/22 15:21
 */
public interface StrixOssClient {

    /**
     * 获取对象存储服务公网操作集
     *
     * @return 公网操作集
     */
    Operations getPublic();

    /**
     * 获取对象存储服务私网操作集
     *
     * @return 私网操作集
     */
    Operations getPrivate();

    /**
     * 关闭客户端
     */
    void close();

    /**
     * Strix OSS 操作集
     */
    interface Operations {

        /**
         * 上传文件
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @param buf        文件字节数组
         */
        void upload(String bucketName, String objectName, byte[] buf);

        /**
         * 上传文件
         *
         * @param bucketName  桶名称
         * @param objectName  对象名称
         * @param inputStream 输入流
         */
        void upload(String bucketName, String objectName, InputStream inputStream);

        /**
         * 上传文件
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @param file       文件
         */
        void upload(String bucketName, String objectName, File file);

        /**
         * 获取上传文件的签名 URL
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @param expires    过期时间
         * @return 上传文件的签名 URL
         */
        String signUploadUrl(String bucketName, String objectName, long expires);

        /**
         * 下载文件
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @param filePath   文件路径
         * @return 文件
         */
        File download(String bucketName, String objectName, String filePath);

        File downloadStream(String bucketName, String objectName, String filePath);

        /**
         * 获取下载文件的签名 URL
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @param expires    过期时间 (ms)
         * @return 下载文件的签名 URL
         */
        String signDownloadUrl(String bucketName, String objectName, long expires);

        boolean exist(String bucketName, String objectName);

        void list(String bucketName, String prefix, int maxKeys);

        /**
         * 删除文件
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         */
        void delete(String bucketName, String objectName);

        /**
         * 获取桶列表
         *
         * @return 桶列表
         */
        List<StrixOssBucket> listBuckets();

        /**
         * 创建桶
         *
         * @param bucketName   桶名称
         * @param storageClass 存储类型
         */
        void createBucket(String bucketName, String storageClass);

        /**
         * 删除桶
         *
         * @param bucketName 桶名称
         */
        void deleteBucket(String bucketName);

    }

}
