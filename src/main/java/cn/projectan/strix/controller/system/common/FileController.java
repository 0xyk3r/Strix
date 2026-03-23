package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
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
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Optional;

/**
 * 系统文件
 *
 * @author ProjectAn
 * @since 2023/5/26 21:57
 */
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

        OssFile ossFile = ossFileService.upload(groupId, file);
        return RetBuilder.success(new CommonFileIdResp(ossFile.getId()));
    }

}
