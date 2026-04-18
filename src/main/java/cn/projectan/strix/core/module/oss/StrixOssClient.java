package cn.projectan.strix.core.module.oss;

import cn.projectan.strix.model.other.system.module.oss.StrixOssBucket;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
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
         * 上传文件 (适用于已知内容长度的流)
         *
         * @param bucketName    桶名称
         * @param objectName    对象名称
         * @param inputStream   输入流
         * @param contentLength 内容长度
         */
        void upload(String bucketName, String objectName, InputStream inputStream, long contentLength);

        /**
         * 上传文件 (不推荐用于大文件, 会将流读入内存)
         *
         * @param bucketName  桶名称
         * @param objectName  对象名称
         * @param inputStream 输入流
         * @deprecated 请使用 {@link #upload(String, String, InputStream, long)} 方法
         */
        @Deprecated
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
         * 下载文件到本地路径
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @param filePath   文件路径
         * @return 文件
         */
        File download(String bucketName, String objectName, String filePath);

        /**
         * 下载文件为输入流
         * <p>注意: 调用者需要负责关闭返回的输入流
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @return 文件输入流
         */
        InputStream downloadAsStream(String bucketName, String objectName);

        /**
         * 下载文件到输出流
         *
         * @param bucketName   桶名称
         * @param objectName   对象名称
         * @param outputStream 输出流
         */
        void downloadToStream(String bucketName, String objectName, OutputStream outputStream);

        /**
         * 流式下载文件到本地路径 (适用于大文件)
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @param filePath   文件路径
         * @return 文件
         * @deprecated 使用 {@link #download(String, String, String)} 即可，S3 SDK 默认支持流式下载
         */
        @Deprecated
        File downloadStream(String bucketName, String objectName, String filePath);

        /**
         * 获取用于客户端流式下载的 StreamingResponseBody
         * <p>适用于 Spring MVC 控制器返回流式响应
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @param response   HTTP响应对象 (用于设置Content-Length等头信息)
         * @return StreamingResponseBody
         */
        StreamingResponseBody downloadStream(String bucketName, String objectName, HttpServletResponse response);

        /**
         * 获取下载文件的签名 URL
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @param expires    过期时间 (ms)
         * @return 下载文件的签名 URL
         */
        String signDownloadUrl(String bucketName, String objectName, long expires);

        /**
         * 判断文件是否存在
         *
         * @param bucketName 桶名称
         * @param objectName 对象名称
         * @return 是否存在
         */
        boolean exist(String bucketName, String objectName);

        /**
         * 列举文件
         *
         * @param bucketName 桶名称
         * @param prefix     前缀
         * @param maxKeys    最大数量
         */
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
         * @param bucketName 桶名称
         */
        void createBucket(String bucketName);

        /**
         * 删除桶
         *
         * @param bucketName 桶名称
         */
        void deleteBucket(String bucketName);

        /**
         * 复制对象
         *
         * @param bucketName       源桶名称
         * @param sourceObjectName 源对象名称
         * @param targetObjectName 目标对象名称
         */
        void copy(String bucketName, String sourceObjectName, String targetObjectName);

    }

}
