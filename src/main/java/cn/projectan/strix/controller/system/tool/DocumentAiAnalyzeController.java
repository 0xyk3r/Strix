package cn.projectan.strix.controller.system.tool;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.tool.document.DocumentAiAnalyzeReq;
import cn.projectan.strix.model.response.system.tool.document.DocumentAiModelResp;
import cn.projectan.strix.model.response.system.tool.document.DocumentAiSubmitResp;
import cn.projectan.strix.service.system.DocumentAiAnalyzeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 文档 AI 分析控制器
 * <p>
 * 两步流程：POST 提交任务 → GET SSE 接收实时分析进度和结果。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/6/29
 */
@RestController
@RequestMapping("system/tool/document/ai")
@RequiredArgsConstructor
@Tag(name = "系统工具 - 文档 AI 分析")
public class DocumentAiAnalyzeController extends BaseSystemController {

    private final DocumentAiAnalyzeService documentAiAnalyzeService;

    /**
     * 获取可用的视觉模型列表
     */
    @Operation(summary = "获取可用视觉模型列表")
    @GetMapping("models/vision")
    @PreAuthorize("@ss.hasPermission('system:ai:document')")
    public RetResult<List<DocumentAiModelResp>> listVisionModels() {
        return RetBuilder.success(documentAiAnalyzeService.listVisionModels());
    }

    /**
     * 获取可用的文本模型列表（用于合并步骤）
     */
    @Operation(summary = "获取可用文本模型列表")
    @GetMapping("models/text")
    @PreAuthorize("@ss.hasPermission('system:ai:document')")
    public RetResult<List<DocumentAiModelResp>> listTextModels() {
        return RetBuilder.success(documentAiAnalyzeService.listTextModels());
    }

    /**
     * 提交文档 AI 分析任务
     * <p>
     * 上传文档文件和分析参数，后端将文档转换为图片并启动异步分析。
     * 返回 taskId 后，前端连接 SSE 端点接收实时分析进度和结果。
     * </p>
     *
     * @param file 文档文件（支持 .doc .docx .pdf .ppt .pptx .xls .xlsx）
     * @param req  分析请求参数
     * @return 任务信息（taskId、总页数、批次信息）
     */
    @IgnoreEncryption
    @Operation(summary = "提交文档 AI 分析任务")
    @PostMapping(value = "analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasPermission('system:ai:document')")
    @StrixLog(operationGroup = "文档 AI 分析", operationName = "提交分析任务", operationType = SystemLogOperType.ADD)
    public RetResult<DocumentAiSubmitResp> submitAnalyze(
            @Parameter(description = "文档文件") @RequestPart("file") MultipartFile file,
            @Validated DocumentAiAnalyzeReq req) throws Exception {
        return RetBuilder.success(documentAiAnalyzeService.submitAnalyze(file, req));
    }

    /**
     * 订阅文档 AI 分析进度（SSE）
     * <p>
     * SSE 事件类型：
     * <ul>
     *   <li>{@code stage} — 阶段变化 {stage, message, totalPages?, totalBatches?, batches?}</li>
     *   <li>{@code batch_chunk} — 批次流式 token {batchIndex, content}</li>
     *   <li>{@code batch_done} — 批次完成 {batchIndex}</li>
     *   <li>{@code batch_error} — 批次出错 {batchIndex, message}</li>
     *   <li>{@code merge_chunk} — 合并流式 token {content}</li>
     *   <li>{@code done} — 全部完成 {message}</li>
     *   <li>{@code error} — 全局错误 {message}</li>
     * </ul>
     * </p>
     *
     * @param taskId 任务 ID（由提交接口返回）
     * @return SSE 事件流
     */
    @IgnoreEncryption
    @Operation(summary = "订阅文档 AI 分析进度（SSE）")
    @GetMapping(value = "analyze/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Anonymous
    public SseEmitter analyzeStream(
            @Parameter(description = "任务 ID") @PathVariable String taskId) {
        return documentAiAnalyzeService.createEmitter(taskId);
    }

    /**
     * 获取文档转换的指定页面图片（PNG）
     * <p>
     * 图片在任务创建时生成，随任务一起保留 30 分钟。前端可通过此接口懒加载各页面缩略图和画廊。
     * </p>
     *
     * @param taskId    任务 ID
     * @param pageIndex 页面下标（0-based）
     * @return PNG 图片字节流
     */
    @IgnoreEncryption
    @Operation(summary = "获取文档转换的页面图片")
    @GetMapping("analyze/images/{taskId}/{pageIndex}")
    @Anonymous
    public ResponseEntity<byte[]> getPageImage(
            @Parameter(description = "任务 ID") @PathVariable String taskId,
            @Parameter(description = "页面下标（0-based）") @PathVariable int pageIndex) {
        byte[] imageData = documentAiAnalyzeService.getPageImage(taskId, pageIndex);
        if (imageData == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=1800, private")
                .body(imageData);
    }

}
