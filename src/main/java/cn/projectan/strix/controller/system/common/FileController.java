package cn.projectan.strix.controller.system.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.db.system.OssFile;
import cn.projectan.strix.model.dict.system.OssFileGroupSecretType;
import cn.projectan.strix.model.response.common.CommonFileIdResp;
import cn.projectan.strix.service.system.OssFileService;
import cn.projectan.strix.util.common.I18nUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.util.Optional;

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
@Tag(name = "通用 - 文件")
public class FileController extends BaseSystemController {

    private final OssFileService ossFileService;

    /**
     * 获取文件
     */
    @GetMapping("{fileId}")
    @Operation(summary = "下载文件")
    @Parameter(name = "fileId", description = "文件 ID", required = true)
    public StreamingResponseBody download(@PathVariable String fileId, HttpServletResponse response) {
        OssFile ossFile = ossFileService.getById(fileId);
        Assert.notNull(ossFile, I18nUtil.get("assert.oss.download.fileNotFound"));

        Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(ossFile.getPath());
        mediaType.ifPresentOrElse(
                mt -> response.setContentType(mt.toString()),
                () -> response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        );

        return ossFileService.getStreamingDownload(fileId, OssFileGroupSecretType.MANAGER, loginManagerId());
    }

    /**
     * 上传文件
     */
    @IgnoreEncryption
    @PostMapping("{groupId}/upload")
    @Operation(summary = "上传文件")
    @Parameter(name = "groupId", description = "文件组 ID", required = true)
    public RetResult<CommonFileIdResp> upload(@PathVariable String groupId,
                                              @RequestPart MultipartFile file) {
        Assert.hasText(groupId, I18nUtil.get("error.param.invalid"));
        Assert.notNull(file, I18nUtil.get("assert.file.notSelected"));

        try {
            File tempFile = File.createTempFile("temp", file.getOriginalFilename());
            try {
                try (var in = file.getInputStream();
                     var out = FileUtil.getOutputStream(tempFile)) {
                    IoUtil.copy(in, out);
                }

                OssFile ossFile = ossFileService.upload(groupId, tempFile);

                return RetBuilder.success(
                        new CommonFileIdResp(ossFile.getId())
                );
            } finally {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        } catch (IllegalArgumentException e) {
            throw new StrixException(I18nUtil.get("error.file.uploadFailed") + ", " + e.getMessage());
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new StrixException(I18nUtil.get("error.file.uploadFailed"));
        }
    }

}
