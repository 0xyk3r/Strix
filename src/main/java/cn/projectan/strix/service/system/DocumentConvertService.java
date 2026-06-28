package cn.projectan.strix.service.system;

import cn.projectan.strix.model.enums.DocumentConvertStatus;
import cn.projectan.strix.model.enums.DocumentConvertType;
import cn.projectan.strix.model.response.system.tool.document.DocumentConvertSubmitResp;
import cn.projectan.strix.model.response.system.tool.document.DocumentConvertTypeResp;
import cn.projectan.strix.util.document.AsposeCellsUtil;
import cn.projectan.strix.util.document.AsposePdfUtil;
import cn.projectan.strix.util.document.AsposeSlidesUtil;
import cn.projectan.strix.util.document.AsposeWordsUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 文档格式转换服务
 * <p>
 * 基于 Aspose 系列库实现文档格式转换，支持异步任务管理和 SSE 实时进度推送。
 * 任务完成后结果保留 30 分钟，超时后自动清理。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentConvertService {

    private static final long TASK_TTL_MS = 30L * 60 * 1000; // 30分钟

    private final ObjectMapper objectMapper;

    /**
     * 任务存储（taskId → 任务信息）
     */
    private final ConcurrentHashMap<String, DocumentConvertTask> taskMap = new ConcurrentHashMap<>();

    /**
     * SSE 发射器存储（taskId → SseEmitter）
     */
    private final ConcurrentHashMap<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    // ==================== 任务提交 ====================

    /**
     * 提交文档转换任务
     *
     * @param file        上传的源文件
     * @param convertType 转换类型
     * @return 任务提交响应（含 taskId 和进度/下载 URL）
     */
    public DocumentConvertSubmitResp submitTask(MultipartFile file, DocumentConvertType convertType) throws Exception {
        cleanExpiredTasks();

        String taskId = UUID.randomUUID().toString().replace("-", "");
        DocumentConvertTask task = new DocumentConvertTask(taskId, file.getOriginalFilename(), convertType);
        taskMap.put(taskId, task);

        byte[] fileBytes = file.getBytes();

        Thread.ofVirtual().name("doc-convert-" + taskId).start(() -> executeConversion(task, fileBytes));

        return new DocumentConvertSubmitResp(taskId);
    }

    // ==================== SSE 进度 ====================

    /**
     * 为指定任务创建 SSE 发射器
     * <p>
     * 若任务已完成，将立即推送完成事件并关闭连接。
     * </p>
     *
     * @param taskId 任务 ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时
        emitterMap.put(taskId, emitter);

        emitter.onCompletion(() -> emitterMap.remove(taskId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitterMap.remove(taskId);
        });
        emitter.onError(e -> emitterMap.remove(taskId));

        DocumentConvertTask task = taskMap.get(taskId);
        if (task == null) {
            sendEvent(emitter, -1, DocumentConvertStatus.FAILED, "任务不存在", null);
            emitter.complete();
            return emitter;
        }

        // 任务已完成，立即推送当前状态
        if (DocumentConvertStatus.COMPLETED.equals(task.getStatus())) {
            sendEvent(emitter, 100, DocumentConvertStatus.COMPLETED, "转换完成", task.getResultFileName());
            emitter.complete();
        } else if (DocumentConvertStatus.FAILED.equals(task.getStatus())) {
            sendEvent(emitter, -1, DocumentConvertStatus.FAILED, task.getErrorMessage(), null);
            emitter.complete();
        }

        return emitter;
    }

    // ==================== 下载 ====================

    /**
     * 下载转换结果文件
     *
     * @param taskId 任务 ID
     * @return 文件响应（含 Content-Disposition 和文件字节）
     */
    public ResponseEntity<byte[]> downloadResult(String taskId) {
        DocumentConvertTask task = taskMap.get(taskId);
        if (task == null || !DocumentConvertStatus.COMPLETED.equals(task.getStatus())) {
            return ResponseEntity.notFound().build();
        }

        String filename = task.getResultFileName();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok().headers(headers).body(task.getResultBytes());
    }

    // ==================== 类型列表 ====================

    /**
     * 获取所有支持的转换类型列表
     */
    public List<DocumentConvertTypeResp> getConvertTypes() {
        return Arrays.stream(DocumentConvertType.values())
                .map(DocumentConvertTypeResp::new)
                .collect(Collectors.toList());
    }

    // ==================== 内部执行 ====================

    private void executeConversion(DocumentConvertTask task, byte[] fileBytes) {
        task.setStatus(DocumentConvertStatus.PROCESSING);
        sendTaskEvent(task.getTaskId(), 5, DocumentConvertStatus.PROCESSING, "正在读取文件...", null);

        try {
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            performConversion(task, fileBytes, resultStream);

            task.setResultBytes(resultStream.toByteArray());
            task.setResultFileName(buildResultFileName(task.getOriginalFileName(), task.getConvertType()));
            task.setStatus(DocumentConvertStatus.COMPLETED);
            task.setExpireAt(System.currentTimeMillis() + TASK_TTL_MS);

            sendTaskEvent(task.getTaskId(), 100, DocumentConvertStatus.COMPLETED, "转换完成", task.getResultFileName());

            SseEmitter emitter = emitterMap.remove(task.getTaskId());
            if (emitter != null) {
                emitter.complete();
            }
        } catch (Exception e) {
            log.error("[文档转换] 任务 {} 转换失败", task.getTaskId(), e);
            task.setStatus(DocumentConvertStatus.FAILED);
            task.setErrorMessage(e.getMessage());

            sendTaskEvent(task.getTaskId(), -1, DocumentConvertStatus.FAILED, "转换失败: " + e.getMessage(), null);

            SseEmitter emitter = emitterMap.remove(task.getTaskId());
            if (emitter != null) {
                emitter.completeWithError(e);
            }
        }
    }

    /**
     * 执行格式转换，对转图片类型（IMAGES）提供基于页数的实时进度回调。
     */
    private void performConversion(DocumentConvertTask task, byte[] sourceBytes, ByteArrayOutputStream out) throws Exception {
        DocumentConvertType type = task.getConvertType();
        String taskId = task.getTaskId();

        // ──── 对 *_TO_IMAGES 类型提前计算页数，实现逐页精准进度 ────
        if (isMultiPageImageType(type)) {
            performMultiPageImageConversion(type, taskId, sourceBytes, out);
            return;
        }

        // ──── 其他类型：阶段式固定进度 ────
        sendTaskEvent(taskId, 20, DocumentConvertStatus.PROCESSING, "正在解析文档结构...", null);
        ByteArrayInputStream input = new ByteArrayInputStream(sourceBytes);
        sendTaskEvent(taskId, 35, DocumentConvertStatus.PROCESSING, "正在执行格式转换...", null);
        switch (type) {
            case CELLS_TO_PDF -> AsposeCellsUtil.toPdf(input, out);
            case CELLS_TO_HTML -> AsposeCellsUtil.toHtml(input, out);
            case CELLS_TO_CSV -> AsposeCellsUtil.xlsToCsv(input, out);
            case CELLS_TO_IMAGE -> AsposeCellsUtil.toImage(input, 0, out);
            case PDF_TO_WORD -> AsposePdfUtil.toWord(input, out);
            case PDF_TO_EXCEL -> AsposePdfUtil.toExcel(input, out);
            case PDF_TO_HTML -> AsposePdfUtil.toHtml(input, out);
            case PDF_COMPRESS -> AsposePdfUtil.compress(input, out);
            case SLIDES_TO_PDF -> AsposeSlidesUtil.toPdf(input, out);
            case SLIDES_TO_HTML -> AsposeSlidesUtil.toHtml(input, out);
            case WORDS_TO_PDF -> AsposeWordsUtil.toPdf(input, out);
            case WORDS_TO_HTML -> AsposeWordsUtil.toHtml(input, out);
            case WORDS_TO_MARKDOWN -> AsposeWordsUtil.toMarkdown(input, out);
            case WORDS_TO_IMAGE -> AsposeWordsUtil.toImage(input, 0, out);
            default -> throw new UnsupportedOperationException("不支持的转换类型: " + type.getDisplayName());
        }
        sendTaskEvent(taskId, 90, DocumentConvertStatus.PROCESSING, "正在写入结果...", null);
    }

    private boolean isMultiPageImageType(DocumentConvertType type) {
        return switch (type) {
            case CELLS_TO_IMAGES, PDF_TO_IMAGES, SLIDES_TO_IMAGES, WORDS_TO_IMAGES -> true;
            default -> false;
        };
    }

    /**
     * 对多页转图片类型执行转换，按实际页/张数推送精确进度。
     */
    private void performMultiPageImageConversion(DocumentConvertType type, String taskId,
                                                 byte[] sourceBytes, ByteArrayOutputStream out) throws Exception {
        // Step 1: Count total pages/sheets
        sendTaskEvent(taskId, 10, DocumentConvertStatus.PROCESSING, "正在统计文档页数...", null);
        int total = countPages(type, sourceBytes);
        String unit = getPageUnit(type);
        sendTaskEvent(taskId, 15, DocumentConvertStatus.PROCESSING,
                String.format("正在转换（共 %d %s）...", total, unit), null);

        // Step 2: Convert with per-page progress callback
        // Progress maps page[1..total] to range [15..90]
        java.util.function.IntConsumer progressCallback = (currentPage) -> {
            if (total <= 0) return;
            int progress = 15 + (int) ((currentPage / (double) total) * 75);
            sendTaskEvent(taskId, progress, DocumentConvertStatus.PROCESSING,
                    String.format("正在转换第 %d %s / 共 %d %s", currentPage, unit, total, unit), null);
        };

        ByteArrayInputStream input = new ByteArrayInputStream(sourceBytes);
        switch (type) {
            case CELLS_TO_IMAGES -> AsposeCellsUtil.toImages(input, out, progressCallback);
            case PDF_TO_IMAGES -> AsposePdfUtil.toImages(input, 150, out, progressCallback);
            case SLIDES_TO_IMAGES -> AsposeSlidesUtil.toImages(input, 1280, 720, out, progressCallback);
            case WORDS_TO_IMAGES -> AsposeWordsUtil.toImages(input, out, progressCallback);
            default -> {
            }
        }
        sendTaskEvent(taskId, 92, DocumentConvertStatus.PROCESSING, "正在打包结果...", null);
    }

    /**
     * 预计算文档页数/张数
     */
    private int countPages(DocumentConvertType type, byte[] sourceBytes) {
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(sourceBytes);
            return switch (type) {
                case PDF_TO_IMAGES -> AsposePdfUtil.getPageCount(input);
                case SLIDES_TO_IMAGES -> AsposeSlidesUtil.getSlideCount(input);
                case WORDS_TO_IMAGES -> AsposeWordsUtil.getPageCount(input);
                case CELLS_TO_IMAGES -> {
                    List<String> names = AsposeCellsUtil.getSheetNames(input);
                    yield names.size();
                }
                default -> 0;
            };
        } catch (Exception e) {
            log.warn("[文档转换] 页数统计失败，将使用默认进度", e);
            return 0;
        }
    }

    private String getPageUnit(DocumentConvertType type) {
        return switch (type) {
            case SLIDES_TO_IMAGES -> "张";
            case CELLS_TO_IMAGES -> "个工作表";
            default -> "页";
        };
    }

    private String buildResultFileName(String originalFileName, DocumentConvertType type) {
        String baseName = originalFileName != null
                ? originalFileName.replaceAll("\\.[^.]+$", "")
                : "converted";
        return baseName + "_converted." + type.getTargetExtension();
    }

    private void sendTaskEvent(String taskId, int progress, String status, String message, String resultFileName) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter != null) {
            sendEvent(emitter, progress, status, message, resultFileName);
        }
    }

    private void sendEvent(SseEmitter emitter, int progress, String status, String message, String resultFileName) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("progress", progress);
            data.put("status", status);
            data.put("message", message);
            if (resultFileName != null) {
                data.put("filename", resultFileName);
            }
            emitter.send(SseEmitter.event().name("progress").data(objectMapper.writeValueAsString(data)));
        } catch (Exception e) {
            log.warn("[文档转换] SSE 推送失败: {}", e.getMessage());
        }
    }

    /**
     * 清理已过期任务
     */
    private void cleanExpiredTasks() {
        long now = System.currentTimeMillis();
        taskMap.entrySet().removeIf(entry -> {
            DocumentConvertTask task = entry.getValue();
            return task.getExpireAt() > 0 && task.getExpireAt() < now;
        });
    }

    // ==================== 任务信息 POJO ====================

    /**
     * 文档转换任务（纯内存，不持久化）
     */
    @Data
    public static class DocumentConvertTask {

        /**
         * 任务 ID
         */
        private final String taskId;

        /**
         * 原始文件名
         */
        private final String originalFileName;

        /**
         * 转换类型
         */
        private final DocumentConvertType convertType;

        /**
         * 任务状态
         */
        private String status;

        /**
         * 错误信息（仅失败时有值）
         */
        private String errorMessage;

        /**
         * 转换结果字节数组
         */
        private byte[] resultBytes;

        /**
         * 结果文件名
         */
        private String resultFileName;

        /**
         * 创建时间（毫秒时间戳）
         */
        private final long createdAt = System.currentTimeMillis();

        /**
         * 过期时间（毫秒时间戳，0 表示未设置）
         */
        private long expireAt;

        public DocumentConvertTask(String taskId, String originalFileName, DocumentConvertType convertType) {
            this.taskId = taskId;
            this.originalFileName = originalFileName;
            this.convertType = convertType;
            this.status = DocumentConvertStatus.PENDING;
        }

    }

}
