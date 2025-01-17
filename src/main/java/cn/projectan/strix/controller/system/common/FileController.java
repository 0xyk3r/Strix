package cn.projectan.strix.controller.system.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.model.dict.StrixOssFileGroupSecretType;
import cn.projectan.strix.service.OssFileService;
import cn.projectan.strix.util.MimeUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

/**
 * 系统文件
 *
 * @author ProjectAn
 * @since 2023/5/26 21:57
 */
@Slf4j
@RestController("SystemCommonFileController")
@RequestMapping("system/common/file")
@RequiredArgsConstructor
public class FileController extends BaseSystemController {

    private final OssFileService ossFileService;

    /**
     * 获取文件
     */
    @GetMapping("{fileId}")
    public void getFile(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        OssFile ossFile = ossFileService.getById(fileId);
        Assert.notNull(ossFile, "下载文件失败, 文件不存在.");

        response.setContentType(MimeUtil.ext2Mime(ossFile.getExt()));
        response.sendRedirect(ossFileService.getUrl(fileId, StrixOssFileGroupSecretType.MANAGER, loginManagerId(), "https://oss.huiboche.cn/System/404.png"));
    }

    /**
     * 上传文件
     */
    @PostMapping("{groupId}/upload")
    @IgnoreDataEncryption
    public RetResult<Object> uploadImage(@PathVariable String groupId, MultipartFile file) {
        Assert.hasText(groupId, "参数错误");
        Assert.notNull(file, "未选择文件");

        try {
            File tempFile = File.createTempFile("temp", file.getOriginalFilename());
            IoUtil.copy(file.getInputStream(), FileUtil.getOutputStream(tempFile));

            OssFile ossFile = ossFileService.upload(groupId, tempFile);

            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();

            return RetBuilder.success(Map.of("fileId", ossFile.getId()));
        } catch (IllegalArgumentException e) {
            return RetBuilder.error("上传文件失败，" + e.getMessage());
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return RetBuilder.error("上传文件失败");
        }
    }

}
