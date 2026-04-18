package cn.projectan.strix.controller.system.module.oss;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.module.oss.*;
import cn.projectan.strix.model.response.system.module.oss.OssFileArchiveResp;
import cn.projectan.strix.model.response.system.module.oss.OssFileBrowseResp;
import cn.projectan.strix.service.system.OssFileBrowseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("system/oss/file/browse")
@ConditionalOnBean(StrixOssStore.class)
@RequiredArgsConstructor
@Tag(name = "系统模块 - OSS 文件浏览器")
public class OssFileBrowseController extends BaseSystemController {

    private final OssFileBrowseService ossFileBrowseService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:oss:file:browse')")
    @StrixLog(operationGroup = "文件浏览器", operationName = "浏览文件")
    @Operation(summary = "浏览文件目录")
    public RetResult<OssFileBrowseResp> browse(@Validated OssFileBrowseReq req) {
        return RetBuilder.success(ossFileBrowseService.browse(req));
    }

    @PostMapping("mkdir")
    @PreAuthorize("@ss.hasPermission('system:oss:file:upload')")
    @StrixLog(operationGroup = "文件浏览器", operationName = "创建目录", operationType = SystemLogOperType.ADD)
    @Operation(summary = "创建目录")
    public RetResult<Object> mkdir(@RequestBody @Validated OssFileMkdirReq req) {
        ossFileBrowseService.mkdir(req);
        return RetBuilder.success();
    }

    @PostMapping("rename")
    @PreAuthorize("@ss.hasPermission('system:oss:file:rename')")
    @StrixLog(operationGroup = "文件浏览器", operationName = "重命名文件", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "重命名文件")
    public RetResult<Object> rename(@RequestBody @Validated OssFileRenameReq req) {
        ossFileBrowseService.rename(req);
        return RetBuilder.success();
    }

    @PostMapping("move")
    @PreAuthorize("@ss.hasPermission('system:oss:file:move')")
    @StrixLog(operationGroup = "文件浏览器", operationName = "移动文件", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "移动文件")
    public RetResult<Object> move(@RequestBody @Validated OssFileMoveReq req) {
        ossFileBrowseService.move(req);
        return RetBuilder.success();
    }

    @PostMapping("copy")
    @PreAuthorize("@ss.hasPermission('system:oss:file:copy')")
    @StrixLog(operationGroup = "文件浏览器", operationName = "复制文件", operationType = SystemLogOperType.ADD)
    @Operation(summary = "复制文件")
    public RetResult<Object> copy(@RequestBody @Validated OssFileCopyReq req) {
        ossFileBrowseService.copy(req);
        return RetBuilder.success();
    }

    @PostMapping("batch/remove")
    @PreAuthorize("@ss.hasPermission('system:oss:file:remove')")
    @StrixLog(operationGroup = "文件浏览器", operationName = "批量删除文件", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "批量删除文件")
    public RetResult<Object> batchRemove(@RequestBody @Validated OssFileBatchRemoveReq req) {
        ossFileBrowseService.batchRemove(req);
        return RetBuilder.success();
    }

    @PostMapping("batch/move")
    @PreAuthorize("@ss.hasPermission('system:oss:file:move')")
    @StrixLog(operationGroup = "文件浏览器", operationName = "批量移动文件", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "批量移动文件")
    public RetResult<Object> batchMove(@RequestBody @Validated OssFileMoveReq req) {
        ossFileBrowseService.move(req);
        return RetBuilder.success();
    }

    @GetMapping("preview/{fileId}")
    @PreAuthorize("@ss.hasPermission('system:oss:file:browse')")
    @StrixLog(operationGroup = "文件浏览器", operationName = "获取预览URL")
    @Operation(summary = "获取文件预览签名URL")
    public RetResult<String> getPreviewUrl(@Parameter(description = "文件ID") @PathVariable String fileId) {
        return RetBuilder.success(ossFileBrowseService.getPreviewUrl(fileId));
    }

    @GetMapping("archive/{fileId}/list")
    @PreAuthorize("@ss.hasPermission('system:oss:file:browse')")
    @StrixLog(operationGroup = "文件浏览器", operationName = "查看压缩包内容")
    @Operation(summary = "列出压缩包内容")
    public RetResult<OssFileArchiveResp> listArchiveContents(@Parameter(description = "文件ID") @PathVariable String fileId) {
        return RetBuilder.success(ossFileBrowseService.listArchiveContents(fileId));
    }

}
