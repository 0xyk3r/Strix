package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.model.dict.StrixOssFileGroupSecretType;
import cn.projectan.strix.service.OssFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

/**
 * @author 安炯奕
 * @date 2023/5/26 21:57
 */
@Slf4j
@RestController("SystemCommonFileController")
@RequestMapping("system/common/file")
@RequiredArgsConstructor
public class FileController extends BaseSystemController {

    private final OssFileService ossFileService;

    @GetMapping("{fileId}")
    public void getImage(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        response.setContentType("image/jpeg");
        response.sendRedirect(ossFileService.getUrl(fileId, StrixOssFileGroupSecretType.MANAGER, getLoginManagerId(), "https://oss.huiboche.cn/System/404.png"));
    }

    @PostMapping("{groupId}/upload")
    @IgnoreDataEncryption
    public RetResult<Object> uploadImage(@PathVariable String groupId, MultipartFile file) {
        Assert.hasText(groupId, "参数错误");
        Assert.notNull(file, "未选择文件");

        try {
            File tempFile = File.createTempFile("temp", file.getOriginalFilename());
            FileUtils.copyInputStreamToFile(file.getInputStream(), tempFile);

            OssFile ossFile = ossFileService.upload(groupId, tempFile, getLoginManagerId());

            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();

            return RetMarker.makeSuccessRsp(Map.of("fileId", ossFile.getId()));
        } catch (Exception e) {
            log.error("上传文件失败", e);
        }
        return RetMarker.makeErrRsp("上传文件失败");
    }

}
