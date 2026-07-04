package cn.projectan.strix.controller.system.tool;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.AiDocumentTask;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.tool.document.DocumentAiAnalyzeReq;
import cn.projectan.strix.model.response.system.tool.document.DocumentAiModelResp;
import cn.projectan.strix.model.response.system.tool.document.DocumentAiResultResp;
import cn.projectan.strix.model.response.system.tool.document.DocumentAiSubmitResp;
import cn.projectan.strix.service.system.DocumentAiAnalyzeService;
import cn.projectan.strix.util.common.I18nUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 文档 AI 分析控制器
 * <p>
 * 两步流程：POST 提交任务 → GET SSE 接收实时分析进度和结果；SSE 断开可重新挂接续播，
 * 结束后走结果兜底接口拉取最终落库结果。
 * <p>
 * <b>鉴权</b>：SSE / 图片 / 结果 / 重试接口均需登录且校验任务归属（managerId），
 * 不再对外匿名开放（原 {@code @Anonymous} 越权已修复）。EventSource 无法带 Authorization 头，
 * 故前端 SSE 改用 fetch + SM3 签名模式（与 AI 对话续播一致）。
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
    @Qualifier("mvcAsyncExecutor")
    private final Executor mvcAsyncExecutor;

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
     * 获取可用的文本模型列表（用于合并步骤 / 纯文本分析）
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
     * 支持文档（.doc .docx .pdf .ppt .pptx .xls .xlsx，转图片交视觉模型）
     * 与纯文本（.txt .md .csv，直接交文本模型）。返回 taskId 后，前端连接 SSE 端点接收进度。
     * </p>
     */
    @IgnoreEncryption
    @Operation(summary = "提交文档 AI 分析任务")
    @PostMapping(value = "analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasPermission('system:ai:document')")
    @StrixLog(operationGroup = "文档 AI 分析", operationName = "提交分析任务", operationType = SystemLogOperType.ADD)
    public RetResult<DocumentAiSubmitResp> submitAnalyze(
            @Parameter(description = "文档 / 文本文件") @RequestPart("file") MultipartFile file,
            @Validated DocumentAiAnalyzeReq req) throws Exception {
        return RetBuilder.success(documentAiAnalyzeService.submitAnalyze(file, req, loginManagerId()));
    }

    /**
     * 挂接文档 AI 分析进度（SSE 续播）
     * <p>
     * 命中进行中的分析：先下发 {@code snapshot}（当前阶段 + 全部批次已生成内容与状态 + 合并已生成内容），
     * 再继续推送 {@code stage/batch_chunk/batch_done/batch_error/merge_chunk} 增量，直至 {@code done}/{@code error}；
     * 无进行中的分析（已完成 / 出错 / 从未开始）：立即结束，客户端走结果兜底接口 {@code result}。
     * <p>GET 无请求体，故不加密；仅回放明文流。鉴权 + 归属校验在 Service 内完成。
     */
    @IgnoreEncryption
    @Operation(summary = "挂接文档 AI 分析进度（SSE 续播）")
    @GetMapping(value = "analyze/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@ss.hasPermission('system:ai:document')")
    public SseEmitter analyzeStream(@Parameter(description = "任务 ID") @PathVariable String taskId) {
        SseEmitter emitter = new SseEmitter(600_000L);
        String managerId = loginManagerId();
        mvcAsyncExecutor.execute(() -> documentAiAnalyzeService.attachStream(taskId, managerId, emitter));
        return emitter;
    }

    /**
     * 获取任务最终结果（兜底）
     * <p>分析已结束、SSE 无进行中生成时用于拉取落库的各批次结果 / 合并结果 / 状态。</p>
     */
    @Operation(summary = "获取文档 AI 分析结果（兜底）")
    @GetMapping("analyze/result/{taskId}")
    @PreAuthorize("@ss.hasPermission('system:ai:document')")
    public RetResult<DocumentAiResultResp> getResult(@Parameter(description = "任务 ID") @PathVariable String taskId) {
        AiDocumentTask task = documentAiAnalyzeService.getResult(taskId, loginManagerId());
        Assert.notNull(task, I18nUtil.notFound("field.originalData"));
        return RetBuilder.success(DocumentAiResultResp.from(task));
    }

    /**
     * 重试指定失败批次（SSE 流式，仅文档类任务）
     * <p>重新分析该批次并把增量推给当前连接，完成后更新落库结果。</p>
     */
    @IgnoreEncryption
    @Operation(summary = "重试文档 AI 分析批次（SSE）")
    @GetMapping(value = "analyze/{taskId}/batch/{batchIndex}/retry", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@ss.hasPermission('system:ai:document')")
    @StrixLog(operationGroup = "文档 AI 分析", operationName = "重试分析批次")
    public SseEmitter retryBatch(
            @Parameter(description = "任务 ID") @PathVariable String taskId,
            @Parameter(description = "批次索引（0-based）") @PathVariable int batchIndex) {
        SseEmitter emitter = new SseEmitter(600_000L);
        String managerId = loginManagerId();
        mvcAsyncExecutor.execute(() -> documentAiAnalyzeService.retryBatch(taskId, batchIndex, managerId, emitter));
        return emitter;
    }

    /**
     * 获取文档转换的指定页面图片（PNG）
     * <p>图片存磁盘临时目录，随任务过期清理。鉴权 + 归属校验在 Service 内完成。</p>
     */
    @IgnoreEncryption
    @Operation(summary = "获取文档转换的页面图片")
    @GetMapping("analyze/images/{taskId}/{pageIndex}")
    @PreAuthorize("@ss.hasPermission('system:ai:document')")
    public ResponseEntity<byte[]> getPageImage(
            @Parameter(description = "任务 ID") @PathVariable String taskId,
            @Parameter(description = "页面下标（0-based）") @PathVariable int pageIndex) {
        // 归属校验：非本人任务直接 404，避免越权拉取他人文档图片
        if (documentAiAnalyzeService.getResult(taskId, loginManagerId()) == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] imageData = documentAiAnalyzeService.getPageImage(taskId, pageIndex);
        if (imageData == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=1800, private")
                .body(imageData);
    }

}
