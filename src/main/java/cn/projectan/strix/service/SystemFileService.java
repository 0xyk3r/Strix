package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemFile;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.File;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2022-03-09
 */
public interface SystemFileService extends IService<SystemFile> {

    String uploadImage(String ossId, String imageGroup, String imageBase64, Integer uploaderType, String uploaderId);

    File downloadFile(String ossId, String imageGroup, SystemFile systemFile, Integer downloaderType, String downloaderId, File saveFile);
    File downloadFile(String ossId, String imageGroup, String fileId, Integer downloaderType, String downloaderId, File saveFile);

    String getImageUrl(String ossId, String imageGroup, String fileId, Integer viewerType);

    String getImageUrl(String ossId, String imageGroup, String fileId, Integer viewerType, String viewerId);

}
