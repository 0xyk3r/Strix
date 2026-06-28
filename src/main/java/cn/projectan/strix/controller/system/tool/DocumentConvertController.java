package cn.projectan.strix.controller.system.tool;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.enums.DocumentConvertType;
import cn.projectan.strix.model.response.system.tool.document.DocumentConvertSubmitResp;
import cn.projectan.strix.model.response.system.tool.document.DocumentConvertTypeResp;
import cn.projectan.strix.service.system.DocumentConvertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 文档格式转换控制器
 * <p>
 * 提供文档格式转换工具接口，支持提交异步转换任务、SSE 实时进度推送和结果文件下载。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
@RestController
@RequestMapping("system/tool/document")
@RequiredArgsConstructor
@Tag(name = "系统工具 - 文档格式转换")
public class DocumentConvertController extends BaseSystemController {

    private final DocumentConvertService documentConvertService;

    /**
     * 获取支持的转换类型列表
     */
    @Operation(summary = "获取支持的转换类型列表")
    @GetMapping("convert/types")
    @PreAuthorize("@ss.hasPermission('system:tool:document:convert')")
    public RetResult<List<DocumentConvertTypeResp>> getConvertTypes() {
        return RetBuilder.success(documentConvertService.getConvertTypes());
    }

    /**
     * 提交文档转换任务
     * <p>
     * 上传文件后立即返回任务 ID，转换在后台异步执行。
     * 客户端通过 {@code /convert/progress/{taskId}} SSE 接口获取实时进度，
     * 进度达到 100% 后通过 {@code /convert/download/{taskId}} 下载结果。
     * </p>
     *
     * @param file 待转换的源文件
     * @param type 转换类型枚举名称（见 {@link DocumentConvertType}，例如 {@code WORDS_TO_PDF}）
     * @return 任务信息（taskId、进度 URL、下载 URL）
     */
    @IgnoreEncryption
    @Operation(summary = "提交文档转换任务")
    @PostMapping(value = "convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasPermission('system:tool:document:convert')")
    @StrixLog(operationGroup = "文档转换", operationName = "提交转换任务", operationType = SystemLogOperType.ADD)
    public RetResult<DocumentConvertSubmitResp> submitConvert(
            @Parameter(description = "待转换的源文件") @RequestPart("file") MultipartFile file,
            @Parameter(description = "转换类型，如 WORDS_TO_PDF") @RequestParam String type) throws Exception {
        DocumentConvertType convertType = DocumentConvertType.valueOf(type);
        return RetBuilder.success(documentConvertService.submitTask(file, convertType));
    }

    /**
     * 获取转换任务实时进度（SSE）
     * <p>
     * 建立 SSE 连接后服务器持续推送进度事件，事件名称为 {@code progress}，数据为 JSON 格式：
     * <pre>{@code
     * {
     *   "progress": 50,          // 0-100，-1 表示失败
     *   "status": "PROCESSING",  // PENDING | PROCESSING | COMPLETED | FAILED
     *   "message": "正在执行格式转换...",
     *   "filename": "result.pdf" // 仅在 COMPLETED 时存在
     * }
     * }</pre>
     * </p>
     *
     * @param taskId 任务 ID（由提交接口返回）
     * @return SSE 事件流
     */
    @IgnoreEncryption
    @Operation(summary = "获取转换任务实时进度（SSE）")
    @GetMapping(value = "convert/progress/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Anonymous
    public SseEmitter convertProgress(
            @Parameter(description = "任务 ID") @PathVariable String taskId) {
        return documentConvertService.createEmitter(taskId);
    }

    /**
     * 下载转换结果文件
     * <p>
     * 仅在任务状态为 {@code COMPLETED} 时可下载，任务结果保留 30 分钟。
     * </p>
     *
     * @param taskId 任务 ID
     * @return 文件流（Content-Disposition: attachment）
     */
    @IgnoreEncryption
    @Operation(summary = "下载转换结果文件")
    @GetMapping("convert/download/{taskId}")
    public ResponseEntity<byte[]> downloadResult(
            @Parameter(description = "任务 ID") @PathVariable String taskId) {
        return documentConvertService.downloadResult(taskId);
    }

}
