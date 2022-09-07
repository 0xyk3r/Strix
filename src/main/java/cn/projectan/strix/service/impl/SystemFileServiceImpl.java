package cn.projectan.strix.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.config.AliyunOssConfig;
import cn.projectan.strix.mapper.SystemFileMapper;
import cn.projectan.strix.model.db.SystemFile;
import cn.projectan.strix.model.system.AliyunOssInstance;
import cn.projectan.strix.service.SystemDictService;
import cn.projectan.strix.service.SystemFileService;
import com.aliyun.oss.model.GetObjectRequest;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
public class SystemFileServiceImpl extends ServiceImpl<SystemFileMapper, SystemFile> implements SystemFileService {

    @Lazy
    @Autowired
    private AliyunOssConfig aliyunOssConfig;
    @Autowired
    private SystemDictService systemDictService;

    @Override
    public String uploadImage(String ossId, String imageGroup, String imageBase64, Integer uploaderType, String uploaderId) {
        // base64文件头
        String dataPrefix;
        // base64实体部分
        String data;
        // 图片文件后缀名
        String suffix;
        String[] d = imageBase64.split("base64,");
        if (d.length == 2) {
            dataPrefix = d[0];
            data = d[1];
        } else {
            throw new IllegalArgumentException("参数错误");
        }

        // 获取oss对象
        AliyunOssInstance ossInstance = aliyunOssConfig.getInstance(ossId);
        if (ossInstance == null) {
            throw new IllegalArgumentException("上传服务未初始化");
        }

        // 验证上传者类型是否正确
        if (uploaderType < 1 || uploaderType > 3) {
            throw new IllegalArgumentException("参数错误");
        }
        String uploaderTypeStr = "";
        if (uploaderType == 3) {
            uploaderTypeStr = "user";
        } else if (uploaderType == 1) {
            uploaderTypeStr = "manager";
        } else if (uploaderType == 2) {
            uploaderTypeStr = "endpoint";
        }

        // 验证图片分组是否可用
        String imageGroupUser = systemDictService.getDict("ImageGroupUser");
        if (StringUtils.hasText(imageGroupUser)) {
            List<String> groups = Arrays.asList(imageGroupUser.split(","));
            if (!groups.contains(imageGroup)) {
                throw new IllegalArgumentException("参数错误");
            }
        }

        // 验证文件是否是图片
        if ("data:image/jpeg;".equalsIgnoreCase(dataPrefix)) {
            suffix = ".jpg";
        } else if ("data:image/png;".equalsIgnoreCase(dataPrefix)) {
            suffix = ".png";
        } else {
            throw new IllegalArgumentException("不支持的图片类型");
        }

        // 调整base64异常数据
        byte[] imageByte = Base64.decode(data);
        for (int i = 0; i < imageByte.length; ++i) {
            if (imageByte[i] < 0) {
                imageByte[i] += 256;
            }
        }

        // 图片在OSS中的路径
        String imageUrl = String.format("strix/%s/%s/%s%s", uploaderTypeStr, imageGroup, IdUtil.simpleUUID(), suffix);

        // 构建存储至数据库的实体对象
        SystemFile systemFile = new SystemFile();
        systemFile.setOssId(ossId);
        systemFile.setImageGroup(imageGroup);
        systemFile.setImageUrl(imageUrl);
        systemFile.setImageSize(imageByte.length);
        systemFile.setImageType(suffix);
        systemFile.setImageUploaderType(uploaderType);
        systemFile.setImageUploaderId(uploaderId);
        systemFile.setCreateBy(uploaderId);
        systemFile.setUpdateBy(uploaderId);
        int insert = getBaseMapper().insert(systemFile);
        if (insert > 0) {
            // 上传图片至OSS
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageByte)) {
                // TODO 改为从配置文件获取
                ossInstance.getPrivateInstance().putObject("huiboche-core", imageUrl, byteArrayInputStream);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new IllegalArgumentException("远程图片服务异常");
            }
        }

        return systemFile.getId();
    }

    @Override
    public File downloadFile(String ossId, String imageGroup, SystemFile systemFile, Integer downloaderType, String downloaderId, File saveFile) {
        if (!StringUtils.hasText(imageGroup) || systemFile == null) {
            return null;
        }
        // 访问者等级需要高于上传者
        if (!systemFile.getImageGroup().equals(imageGroup) || downloaderType >= systemFile.getImageUploaderType()) {
            return null;
        }
        // 访问者等级相同时，需判断访问者是否为上传者
        if (downloaderType != 1 && !downloaderType.equals(systemFile.getImageUploaderType()) && !Objects.equals(downloaderId, systemFile.getImageUploaderId())) {
            return null;
        }
        // 获取oss对象
        AliyunOssInstance ossInstance = aliyunOssConfig.getInstance(ossId);
        if (ossInstance == null) {
            return null;
        }
        try {
            ossInstance.getPrivateInstance().getObject(new GetObjectRequest("huiboche-core", systemFile.getImageUrl()), saveFile);
            return saveFile;
        } catch (Exception e) {
            log.error("下载OSS文件时出错", e);
        }
        return null;
    }

    @Override
    public File downloadFile(String ossId, String imageGroup, String fileId, Integer downloaderType, String downloaderId, File saveFile) {
        SystemFile systemFile = getBaseMapper().selectById(fileId);
        if (systemFile == null) {
            return null;
        }
        return downloadFile(ossId, imageGroup, systemFile, downloaderType, downloaderId, saveFile);
    }

    @Override
    public String getImageUrl(String ossId, String imageGroup, String fileId, Integer viewerType) {
        String imageUrl = "https://oss.huiboche.cn/System/404.png";
        if (!StringUtils.hasText(imageGroup) || !StringUtils.hasText(fileId)) {
            return imageUrl;
        }
        SystemFile systemFile = getBaseMapper().selectById(fileId);
        // 访问者等级需要高于上传者
        if (systemFile == null || !systemFile.getImageGroup().equals(imageGroup) || viewerType > systemFile.getImageUploaderType()) {
            return imageUrl;
        }
        // 获取oss对象
        AliyunOssInstance ossInstance = aliyunOssConfig.getInstance(ossId);
        if (ossInstance == null) {
            return imageUrl;
        }
        // 设置URL过期时间  5min
        Date expiration = new Date(System.currentTimeMillis() + 300 * 1000);
        // 生成以GET方法访问的签名URL，访客可以直接通过浏览器访问相关内容。
        URL url = ossInstance.getPublicInstance().generatePresignedUrl("huiboche-core", systemFile.getImageUrl(), expiration);

        return url.toString();
    }

    @Override
    public String getImageUrl(String ossId, String imageGroup, String fileId, Integer viewerType, String viewerId) {
        String image404 = "https://oss.huiboche.cn/System/404.png";
        if (!StringUtils.hasText(imageGroup) || !StringUtils.hasText(fileId)) {
            return image404;
        }
        SystemFile systemFile = getBaseMapper().selectById(fileId);
        // 访问者等级需要高于上传者
        if (systemFile == null || !systemFile.getImageGroup().equals(imageGroup) || viewerType >= systemFile.getImageUploaderType()) {
            return image404;
        }
        // 访问者等级相同时，需判断访问者是否为上传者
        if (!viewerType.equals(systemFile.getImageUploaderType()) && !Objects.equals(viewerId, systemFile.getImageUploaderId())) {
            return image404;
        }
        // 获取oss对象
        AliyunOssInstance ossInstance = aliyunOssConfig.getInstance(ossId);
        if (ossInstance == null) {
            return image404;
        }
        // 设置URL过期时间  5min
        Date expiration = new Date(System.currentTimeMillis() + 300 * 1000);
        // 生成以GET方法访问的签名URL，访客可以直接通过浏览器访问相关内容。
        URL url = ossInstance.getPublicInstance().generatePresignedUrl("huiboche-core", systemFile.getImageUrl(), expiration);

        return url.toString();
    }
}
