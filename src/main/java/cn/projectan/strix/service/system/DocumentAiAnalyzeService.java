package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.ai.AiChatClient;
import cn.projectan.strix.core.module.ai.AiJson;
import cn.projectan.strix.core.module.ai.DocumentAiStreamRegistry;
import cn.projectan.strix.core.module.ai.provider.AiProviderAdapter;
import cn.projectan.strix.core.module.ai.provider.AiProviderRegistry;
import cn.projectan.strix.mapper.system.AiDocumentTaskMapper;
import cn.projectan.strix.model.db.system.AiDocumentTask;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import cn.projectan.strix.model.request.system.tool.document.DocumentAiAnalyzeReq;
import cn.projectan.strix.model.response.system.tool.document.DocumentAiModelResp;
import cn.projectan.strix.model.response.system.tool.document.DocumentAiSubmitResp;
import cn.projectan.strix.util.document.AsposeCellsUtil;
import cn.projectan.strix.util.document.AsposePdfUtil;
import cn.projectan.strix.util.document.AsposeSlidesUtil;
import cn.projectan.strix.util.document.AsposeWordsUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文档 AI 分析服务
 * <p>
 * 将文档页面转换为图片，分批并行调用视觉模型分析，可选使用文本模型合并结果；
 * 或对纯文本文件（txt/md/csv）直接喂文本模型分析。
 * <p>
 * 相较早期版本的关键改造：
 * <ul>
 *   <li><b>任务持久化</b>：任务元数据与各批次 / 合并结果落库（{@code sys_ai_document_task}），
 *       服务重启、客户端刷新后仍可查询最终结果，不再仅存内存单实例。</li>
 *   <li><b>页面图片转存磁盘临时目录</b>（存路径而非字节），不再让整份文档 PNG 字节常驻堆，避免 OOM；
 *       任务完成 / 失败 / 过期时清理目录。</li>
 *   <li><b>SSE 断线续播</b>：借助 {@link DocumentAiStreamRegistry} 实现「后台分析 + 多订阅 + 快照续播 +
 *       终态广播」，刷新页面可重新挂接进行中的分析；结束后走结果兜底接口。</li>
 *   <li><b>批次失败可单独重试</b>；<b>纯文本文件</b>直接走文本模型分支。</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026/6/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAiAnalyzeService extends ServiceImpl<AiDocumentTaskMapper, AiDocumentTask> {

    private static final ObjectMapper MAPPER = AiJson.mapper();

    /**
     * 任务保留时长：完成 / 失败后 30 分钟过期，到期清理记录与磁盘图片
     */
    private static final long TASK_TTL_MINUTES = 30L;

    /**
     * 图片临时根目录（系统临时目录下）
     */
    private static final Path IMAGE_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "strix-doc-ai");

    /**
     * 纯文本文件支持的扩展名
     */
    private static final Set<String> TEXT_EXTS = Set.of(".txt", ".md", ".markdown", ".csv");

    /**
     * 文档（转图片）文件支持的扩展名
     */
    private static final Set<String> DOC_EXTS = Set.of(".doc", ".docx", ".pdf", ".ppt", ".pptx", ".xls", ".xlsx");

    private final AiModelConfigService aiModelConfigService;
    private final AiChatClient aiChatClient;
    private final AiProviderRegistry providerRegistry;
    private final DocumentAiStreamRegistry streamRegistry;

    // ==================== 模型列表 ====================

    /**
     * 列出已启用的视觉模型
     * <p>包括 type=VISION 的模型，以及 type=TEXT 且 supportedModalities 含 "image" 的模型</p>
     */
    public List<DocumentAiModelResp> listVisionModels() {
        return aiModelConfigService.lambdaQuery()
                .eq(AiModelConfig::getStatus, 1)
                .and(w -> w
                        .eq(AiModelConfig::getType, AiModelType.VISION)
                        .or(o -> o
                                .eq(AiModelConfig::getType, AiModelType.TEXT)
                                .like(AiModelConfig::getSupportedModalities, "image")))
                .list()
                .stream()
                .map(DocumentAiModelResp::new)
                .toList();
    }

    /**
     * 列出已启用的文本模型（用于合并步骤 / 纯文本分析）
     */
    public List<DocumentAiModelResp> listTextModels() {
        return aiModelConfigService.lambdaQuery()
                .eq(AiModelConfig::getStatus, 1)
                .eq(AiModelConfig::getType, AiModelType.TEXT)
                .list()
                .stream()
                .map(DocumentAiModelResp::new)
                .toList();
    }

    // ==================== 任务提交 ====================

    /**
     * 提交文档 AI 分析任务
     * <p>
     * 根据文件扩展名走两条路径：
     * <ul>
     *   <li>文档类（doc/pdf/ppt/xls 等）：转图片 → 分批 → 视觉模型分析（DOC 类型）；</li>
     *   <li>纯文本类（txt/md/csv）：读文本 → 单批 → 文本模型分析（TEXT 类型，不转图、不需视觉模型）。</li>
     * </ul>
     *
     * @param file      文档 / 文本文件
     * @param req       分析请求参数
     * @param managerId 当前登录管理员 ID（用于归属校验；异步线程内无 SecurityContext，故提交时传入）
     * @return 任务提交响应（含 taskId、批次信息和图片索引范围）
     */
    public DocumentAiSubmitResp submitAnalyze(MultipartFile file, DocumentAiAnalyzeReq req, String managerId)
            throws Exception {
        cleanExpiredTasks();

        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String lower = originalFilename.toLowerCase();
        boolean isText = TEXT_EXTS.stream().anyMatch(lower::endsWith);
        boolean isDoc = DOC_EXTS.stream().anyMatch(lower::endsWith);
        Assert.isTrue(isText || isDoc, "不支持的文件格式，支持文档：" + String.join(" ", DOC_EXTS)
                + "；纯文本：" + String.join(" ", TEXT_EXTS));

        if (isText) {
            return submitTextAnalyze(fileBytes, originalFilename, req, managerId);
        }
        return submitDocAnalyze(fileBytes, originalFilename, req, managerId);
    }

    /**
     * 文档类提交：转图片 → 分批 → 落盘 → 落库 → 异步视觉分析。
     */
    private DocumentAiSubmitResp submitDocAnalyze(byte[] fileBytes, String filename,
                                                  DocumentAiAnalyzeReq req, String managerId) throws Exception {
        if (req.isMerge()) {
            Assert.hasText(req.getTextModelKey(), "合并模式下，文本模型不能为空");
        }
        Assert.hasText(req.getVisionModelKey(), "视觉模型不能为空");

        List<byte[]> images = convertToImages(fileBytes, filename.toLowerCase());
        Assert.notEmpty(images, "文档转图片失败，请检查文件格式和内容");

        List<List<byte[]>> batches = partition(images, req.getBatchSize());
        List<String> batchDescriptions = buildBatchDescriptions(batches, images.size());

        // 计算每批次页面索引范围（0-based，含首尾）
        List<List<Integer>> batchPageRanges = new ArrayList<>();
        int pageStart = 0;
        for (List<byte[]> batch : batches) {
            batchPageRanges.add(List.of(pageStart, pageStart + batch.size() - 1));
            pageStart += batch.size();
        }

        // 落库任务记录（主键雪花 id 自动生成）
        AiDocumentTask task = new AiDocumentTask()
                .setManagerId(managerId)
                .setStatus("PROCESSING")
                .setPrompt(req.getPrompt())
                .setVisionModelKey(req.getVisionModelKey())
                .setTextModelKey(req.isMerge() ? req.getTextModelKey() : null)
                .setMergeEnabled(req.isMerge() ? 1 : 0)
                .setFileName(filename)
                .setInputType("DOC")
                .setTotalPages(images.size())
                .setTotalBatches(batches.size())
                .setBatchDescriptions(toJson(batchDescriptions))
                .setExpireAt(LocalDateTime.now().plusMinutes(TASK_TTL_MINUTES));
        save(task);
        String taskId = task.getId();

        // 页面图片落盘临时目录（存路径，不占堆、不入库）
        try {
            persistImages(taskId, images);
        } catch (Exception e) {
            // 落盘失败：标记任务失败并抛出，避免残留 PROCESSING 空任务
            markFailed(taskId, "页面图片暂存失败: " + e.getMessage());
            cleanupImages(taskId);
            throw e;
        }

        // 异步执行分析（虚拟线程）
        int totalBatches = batches.size();
        Thread.ofVirtual().name("doc-ai-" + taskId).start(() ->
                analyzeDocAsync(taskId, totalBatches, batchDescriptions, req));

        return new DocumentAiSubmitResp(taskId, images.size(), batches.size(),
                batchDescriptions, batchPageRanges);
    }

    /**
     * 纯文本提交：读文本 → 单批 → 落库 → 异步文本分析（N12）。
     * <p>文本文件无图片，totalPages=0，batchPageRanges 为空，视觉模型不参与。</p>
     */
    private DocumentAiSubmitResp submitTextAnalyze(byte[] fileBytes, String filename,
                                                   DocumentAiAnalyzeReq req, String managerId) {
        String textModelKey = StringUtils.hasText(req.getTextModelKey())
                ? req.getTextModelKey() : req.getVisionModelKey();
        Assert.hasText(textModelKey, "纯文本分析需指定文本模型");

        String text = new String(fileBytes, StandardCharsets.UTF_8);
        Assert.hasText(text, "文本文件内容为空");

        List<String> batchDescriptions = List.of("全文");

        AiDocumentTask task = new AiDocumentTask()
                .setManagerId(managerId)
                .setStatus("PROCESSING")
                .setPrompt(req.getPrompt())
                .setTextModelKey(textModelKey)
                .setMergeEnabled(0)
                .setFileName(filename)
                .setInputType("TEXT")
                .setTotalPages(0)
                .setTotalBatches(1)
                .setBatchDescriptions(toJson(batchDescriptions))
                .setExpireAt(LocalDateTime.now().plusMinutes(TASK_TTL_MINUTES));
        save(task);
        String taskId = task.getId();

        Thread.ofVirtual().name("doc-ai-" + taskId).start(() ->
                analyzeTextAsync(taskId, textModelKey, req.getPrompt(), text));

        return new DocumentAiSubmitResp(taskId, 0, 1, batchDescriptions, List.of());
    }

    // ==================== 图片存取（磁盘临时目录）====================

    /**
     * 将各页图片落盘到 {@code {tmpdir}/strix-doc-ai/{taskId}/{index}.png}。
     */
    private void persistImages(String taskId, List<byte[]> images) throws IOException {
        Path dir = IMAGE_ROOT.resolve(taskId);
        Files.createDirectories(dir);
        for (int i = 0; i < images.size(); i++) {
            Files.write(dir.resolve(i + ".png"), images.get(i));
        }
    }

    /**
     * 获取指定任务的页面图片字节。任务不存在 / 越界 / 已清理时返回 null。
     */
    public byte[] getPageImage(String taskId, int pageIndex) {
        if (pageIndex < 0) return null;
        Path img = IMAGE_ROOT.resolve(taskId).resolve(pageIndex + ".png");
        try {
            if (!Files.exists(img)) return null;
            return Files.readAllBytes(img);
        } catch (IOException e) {
            log.debug("doc-ai: 读取页面图片失败 taskId={}, page={}", taskId, pageIndex);
            return null;
        }
    }

    /**
     * 从磁盘临时目录读取某批次的全部页面图片（重试时用）。
     */
    private List<byte[]> loadBatchImages(String taskId, int startPage, int endPage) {
        List<byte[]> images = new ArrayList<>();
        for (int p = startPage; p <= endPage; p++) {
            byte[] img = getPageImage(taskId, p);
            if (img != null) images.add(img);
        }
        return images;
    }

    /**
     * 删除任务的磁盘图片目录。
     */
    private void cleanupImages(String taskId) {
        Path dir = IMAGE_ROOT.resolve(taskId);
        if (!Files.exists(dir)) return;
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException e) {
            log.debug("doc-ai: 清理图片目录失败 taskId={}", taskId);
        }
    }

    // ==================== SSE 续播 ====================

    /**
     * 挂接到任务进行中的分析（重连续播）。应在虚拟线程中调用。
     * <p>命中进行中的分析：回放 snapshot 全量快照后继续接收增量；未命中（已结束 / 不存在）：直接结束，
     * 客户端走结果兜底接口 {@link #getResult}。</p>
     *
     * @param taskId    任务 ID
     * @param managerId 当前管理员 ID（归属校验）
     * @param emitter   新的 SSE 连接
     */
    public void attachStream(String taskId, String managerId, SseEmitter emitter) {
        AiDocumentTask task = getById(taskId);
        if (task == null || !task.getManagerId().equals(managerId)) {
            sendSseError(emitter, "任务不存在或无权限");
            return;
        }
        DocumentAiStreamRegistry.ActiveAnalysis analysis = streamRegistry.get(taskId);
        if (analysis == null) {
            // 无进行中的分析：直接结束，客户端走结果兜底接口
            emitter.complete();
            return;
        }
        emitter.onError(e -> analysis.unsubscribe(emitter));
        emitter.onTimeout(() -> analysis.unsubscribe(emitter));
        emitter.onCompletion(() -> analysis.unsubscribe(emitter));
        if (!analysis.subscribe(emitter)) {
            emitter.complete();
        }
    }

    /**
     * 获取任务的最终结果（兜底接口，用于分析已结束、SSE 无进行中生成时拉取落库结果）。
     *
     * @param taskId    任务 ID
     * @param managerId 当前管理员 ID（归属校验）
     * @return 任务（含状态、各批次结果、合并结果、错误信息）；不存在 / 无权限返回 null
     */
    public AiDocumentTask getResult(String taskId, String managerId) {
        AiDocumentTask task = getById(taskId);
        if (task == null || !task.getManagerId().equals(managerId)) {
            return null;
        }
        return task;
    }

    // ==================== 核心分析逻辑：文档（视觉）====================

    private void analyzeDocAsync(String taskId, int totalBatches, List<String> batchDescriptions,
                                 DocumentAiAnalyzeReq req) {
        DocumentAiStreamRegistry.ActiveAnalysis analysis = streamRegistry.start(taskId, totalBatches);
        if (analysis == null) {
            log.warn("doc-ai: 文档任务 {} 已有进行中的分析，跳过重复启动", taskId);
            return;
        }

        Map<Integer, String> batchResults = new ConcurrentHashMap<>();
        try {
            analysis.setStage("CONVERTING", Map.of("message", "文档已转换为图片，准备分析..."));

            AiModelConfig visionConfig = aiModelConfigService.requireEnabledByKey(req.getVisionModelKey());
            AiProviderAdapter visionAdapter = providerRegistry.getAdapter(visionConfig);

            List<Map<String, Object>> batchInfoList = new ArrayList<>();
            for (int i = 0; i < totalBatches; i++) {
                batchInfoList.add(Map.of("index", i, "pageRange", batchDescriptions.get(i)));
            }
            analysis.setStage("ANALYZING", Map.of(
                    "message", "开始并行分析各批次...",
                    "totalBatches", totalBatches,
                    "batches", batchInfoList));

            // 各批次的页面范围（0-based，含首尾），从描述反推批次页数
            List<int[]> batchPageBounds = resolveBatchBounds(taskId, totalBatches);

            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < totalBatches; i++) {
                final int batchIndex = i;
                final int[] bounds = batchPageBounds.get(i);
                futures.add(CompletableFuture.runAsync(() ->
                        runVisionBatch(taskId, batchIndex, bounds[0], bounds[1],
                                visionConfig, visionAdapter, req.getPrompt(),
                                batchDescriptions.get(batchIndex), analysis, batchResults), executor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();

            // 各批次结果落库
            persistBatchResults(taskId, batchResults);

            // 合并步骤
            String mergeResult = null;
            if (req.isMerge() && StringUtils.hasText(req.getTextModelKey())) {
                analysis.setStage("MERGING", Map.of("message", "正在合并分析结果..."));
                mergeResult = runMerge(req.getTextModelKey(), batchDescriptions, batchResults,
                        req.getPrompt(), analysis);
            }

            markDone(taskId, mergeResult);
            analysis.finish("done", Map.of("message", "分析完成"));
        } catch (Exception e) {
            log.error("doc-ai: 分析任务出错, taskId={}", taskId, e);
            markFailed(taskId, e.getMessage());
            analysis.finish("error", Map.of("message", "分析失败: " +
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
        } finally {
            streamRegistry.remove(taskId);
        }
    }

    /**
     * 运行单个视觉批次：加载该批次图片 → 构建消息 → 流式分析 → 累加并广播。
     * 失败时标记批次出错并记空串（不中断整体），供后续合并 / 重试。
     */
    private void runVisionBatch(String taskId, int batchIndex, int startPage, int endPage,
                                AiModelConfig visionConfig, AiProviderAdapter visionAdapter,
                                String prompt, String pageRange,
                                DocumentAiStreamRegistry.ActiveAnalysis analysis,
                                Map<Integer, String> batchResults) {
        try {
            List<byte[]> batchImages = loadBatchImages(taskId, startPage, endPage);
            Assert.notEmpty(batchImages, "批次图片缺失");

            StringBuilder content = new StringBuilder();
            List<Map<String, Object>> messages = buildVisionMessages(visionConfig, prompt, batchImages, pageRange);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", visionConfig.getModelName());
            body.put("messages", messages);
            visionAdapter.applyStreamingParams(body, visionConfig);

            aiChatClient.streamChat(visionConfig.getBaseUrl(), visionConfig.getApiKey(), body, chunk -> {
                String token = extractContentDelta(chunk);
                if (!token.isEmpty()) {
                    content.append(token);
                    analysis.appendBatchChunk(batchIndex, token);
                }
            });

            batchResults.put(batchIndex, content.toString());
            analysis.markBatchDone(batchIndex);
        } catch (Exception e) {
            log.error("doc-ai: 批次 {} 分析失败, taskId={}", batchIndex, taskId, e);
            batchResults.put(batchIndex, "");
            analysis.markBatchError(batchIndex, e.getMessage() != null ? e.getMessage() : "分析失败");
        }
    }

    /**
     * 重试单个失败批次（N11）。同步执行（供 Controller 在异步线程中调用），完成后更新落库结果。
     * <p>重试不经 {@link DocumentAiStreamRegistry}（原分析多半已结束），而是直接分析并把结果落库，
     * 通过 SSE 把该批次的增量推给当前连接；返回最终内容供 Controller 决定后续。</p>
     */
    public void retryBatch(String taskId, int batchIndex, String managerId, SseEmitter emitter) {
        AiDocumentTask task = getById(taskId);
        if (task == null || !task.getManagerId().equals(managerId)) {
            sendSseError(emitter, "任务不存在或无权限");
            return;
        }
        if (!"DOC".equals(task.getInputType())) {
            sendSseError(emitter, "该任务不支持批次重试");
            return;
        }
        if (batchIndex < 0 || batchIndex >= (task.getTotalBatches() == null ? 0 : task.getTotalBatches())) {
            sendSseError(emitter, "批次索引越界");
            return;
        }

        try {
            List<String> descriptions = fromJsonList(task.getBatchDescriptions());
            String pageRange = batchIndex < descriptions.size() ? descriptions.get(batchIndex) : ("批次 " + (batchIndex + 1));
            int[] bounds = resolveBatchBounds(taskId, task.getTotalBatches()).get(batchIndex);
            List<byte[]> batchImages = loadBatchImages(taskId, bounds[0], bounds[1]);
            if (batchImages.isEmpty()) {
                sendSseError(emitter, "批次图片已过期，无法重试，请重新提交任务");
                return;
            }

            AiModelConfig visionConfig = aiModelConfigService.requireEnabledByKey(task.getVisionModelKey());
            AiProviderAdapter visionAdapter = providerRegistry.getAdapter(visionConfig);

            StringBuilder content = new StringBuilder();
            List<Map<String, Object>> messages = buildVisionMessages(visionConfig, task.getPrompt(), batchImages, pageRange);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", visionConfig.getModelName());
            body.put("messages", messages);
            visionAdapter.applyStreamingParams(body, visionConfig);

            aiChatClient.streamChat(visionConfig.getBaseUrl(), visionConfig.getApiKey(), body, chunk -> {
                String token = extractContentDelta(chunk);
                if (!token.isEmpty()) {
                    content.append(token);
                    sendSseEvent(emitter, "batch_chunk", Map.of("batchIndex", batchIndex, "content", token));
                }
            });

            // 更新落库的批次结果
            Map<Integer, String> results = fromJsonIntMap(task.getBatchResults());
            results.put(batchIndex, content.toString());
            lambdaUpdate()
                    .eq(AiDocumentTask::getId, taskId)
                    .set(AiDocumentTask::getBatchResults, toJson(intMapToStringMap(results)))
                    .update();

            sendSseEvent(emitter, "batch_done", Map.of("batchIndex", batchIndex));
            sendSseEvent(emitter, "done", Map.of("message", "批次重试完成"));
            emitter.complete();
        } catch (Exception e) {
            log.error("doc-ai: 批次 {} 重试失败, taskId={}", batchIndex, taskId, e);
            sendSseError(emitter, "批次重试失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    // ==================== 核心分析逻辑：纯文本 ====================

    private void analyzeTextAsync(String taskId, String textModelKey, String prompt, String text) {
        DocumentAiStreamRegistry.ActiveAnalysis analysis = streamRegistry.start(taskId, 1);
        if (analysis == null) {
            log.warn("doc-ai: 纯文本任务 {} 已有进行中的分析，跳过重复启动", taskId);
            return;
        }
        Map<Integer, String> batchResults = new ConcurrentHashMap<>();
        try {
            analysis.setStage("ANALYZING", Map.of(
                    "message", "开始分析文本内容...",
                    "totalBatches", 1,
                    "batches", List.of(Map.of("index", 0, "pageRange", "全文"))));

            AiModelConfig textConfig = aiModelConfigService.requireEnabledByKey(textModelKey);
            AiProviderAdapter textAdapter = providerRegistry.getAdapter(textConfig);

            StringBuilder content = new StringBuilder();
            List<Map<String, Object>> messages = new ArrayList<>();
            if (StringUtils.hasText(textConfig.getSystemPrompt())) {
                messages.add(Map.of("role", "system", "content", textConfig.getSystemPrompt()));
            }
            messages.add(Map.of("role", "user", "content", prompt + "\n\n---\n以下是待分析的文本内容：\n\n" + text));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", textConfig.getModelName());
            body.put("messages", messages);
            textAdapter.applyStreamingParams(body, textConfig);

            aiChatClient.streamChat(textConfig.getBaseUrl(), textConfig.getApiKey(), body, chunk -> {
                String token = extractContentDelta(chunk);
                if (!token.isEmpty()) {
                    content.append(token);
                    analysis.appendBatchChunk(0, token);
                }
            });

            batchResults.put(0, content.toString());
            analysis.markBatchDone(0);
            persistBatchResults(taskId, batchResults);

            markDone(taskId, null);
            analysis.finish("done", Map.of("message", "分析完成"));
        } catch (Exception e) {
            log.error("doc-ai: 文本分析任务出错, taskId={}", taskId, e);
            markFailed(taskId, e.getMessage());
            analysis.finish("error", Map.of("message", "分析失败: " +
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
        } finally {
            streamRegistry.remove(taskId);
        }
    }

    // ==================== 合并 ====================

    private String runMerge(String textModelKey, List<String> descriptions, Map<Integer, String> results,
                            String originalPrompt, DocumentAiStreamRegistry.ActiveAnalysis analysis) throws IOException {
        AiModelConfig textConfig = aiModelConfigService.requireEnabledByKey(textModelKey);
        AiProviderAdapter textAdapter = providerRegistry.getAdapter(textConfig);

        String mergeUserPrompt = buildMergePrompt(descriptions, results, originalPrompt);
        List<Map<String, Object>> mergeMessages = new ArrayList<>();
        if (StringUtils.hasText(textConfig.getSystemPrompt())) {
            mergeMessages.add(Map.of("role", "system", "content", textConfig.getSystemPrompt()));
        }
        mergeMessages.add(Map.of("role", "user", "content", mergeUserPrompt));

        Map<String, Object> mergeBody = new LinkedHashMap<>();
        mergeBody.put("model", textConfig.getModelName());
        mergeBody.put("messages", mergeMessages);
        textAdapter.applyStreamingParams(mergeBody, textConfig);

        StringBuilder merged = new StringBuilder();
        aiChatClient.streamChat(textConfig.getBaseUrl(), textConfig.getApiKey(), mergeBody, chunk -> {
            String token = extractContentDelta(chunk);
            if (!token.isEmpty()) {
                merged.append(token);
                analysis.appendMergeChunk(token);
            }
        });
        return merged.toString();
    }

    // ==================== 落库辅助 ====================

    private void persistBatchResults(String taskId, Map<Integer, String> batchResults) {
        lambdaUpdate()
                .eq(AiDocumentTask::getId, taskId)
                .set(AiDocumentTask::getBatchResults, toJson(intMapToStringMap(batchResults)))
                .update();
    }

    private void markDone(String taskId, String mergeResult) {
        lambdaUpdate()
                .eq(AiDocumentTask::getId, taskId)
                .set(AiDocumentTask::getStatus, "DONE")
                .set(mergeResult != null, AiDocumentTask::getMergeResult, mergeResult)
                .set(AiDocumentTask::getExpireAt, LocalDateTime.now().plusMinutes(TASK_TTL_MINUTES))
                .update();
    }

    private void markFailed(String taskId, String errorMessage) {
        lambdaUpdate()
                .eq(AiDocumentTask::getId, taskId)
                .set(AiDocumentTask::getStatus, "FAILED")
                .set(AiDocumentTask::getErrorMessage, errorMessage != null
                        ? (errorMessage.length() > 1000 ? errorMessage.substring(0, 1000) : errorMessage) : "未知错误")
                .set(AiDocumentTask::getExpireAt, LocalDateTime.now().plusMinutes(TASK_TTL_MINUTES))
                .update();
    }

    // ==================== 文档转图片 ====================

    private List<byte[]> convertToImages(byte[] fileBytes, String filename) throws Exception {
        ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
        if (filename.endsWith(".doc") || filename.endsWith(".docx")) {
            AsposeWordsUtil.toImages(new ByteArrayInputStream(fileBytes), zipOut, null);
        } else if (filename.endsWith(".pdf")) {
            AsposePdfUtil.toImages(new ByteArrayInputStream(fileBytes), 150, zipOut, null);
        } else if (filename.endsWith(".ppt") || filename.endsWith(".pptx")) {
            AsposeSlidesUtil.toImages(new ByteArrayInputStream(fileBytes), 1280, 720, zipOut, null);
        } else if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            AsposeCellsUtil.toImages(new ByteArrayInputStream(fileBytes), zipOut, null);
        } else {
            throw new IllegalArgumentException("不支持的文件格式");
        }
        return unzipImages(zipOut.toByteArray());
    }

    private List<byte[]> unzipImages(byte[] zipBytes) throws IOException {
        Map<String, byte[]> named = new TreeMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".png")) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = zis.read(buf)) > 0) bos.write(buf, 0, len);
                    named.put(entry.getName(), bos.toByteArray());
                }
                zis.closeEntry();
            }
        }
        return new ArrayList<>(named.values());
    }

    // ==================== 消息构建 ====================

    private List<Map<String, Object>> buildVisionMessages(AiModelConfig config, String prompt,
                                                          List<byte[]> images, String pageRange) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("type", "text", "text", prompt + "\n（当前分析范围：" + pageRange + "）"));
        for (byte[] img : images) {
            String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(img);
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", dataUri)));
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", parts));
        return messages;
    }

    private String buildMergePrompt(List<String> descriptions, Map<Integer, String> results, String originalPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务：").append(originalPrompt).append("\n\n");
        sb.append("以下是文档各部分的分析结果：\n\n");
        for (int i = 0; i < descriptions.size(); i++) {
            String content = results.getOrDefault(i, "");
            if (!StringUtils.hasText(content)) content = "（该部分分析失败，无结果）";
            sb.append("【").append(descriptions.get(i)).append("】\n").append(content).append("\n\n");
        }
        sb.append("---\n");
        sb.append("请将以上各部分内容综合为一份完整的分析报告。");
        sb.append("要求：直接输出分析结论，不要以诸如[根据以上]、[综合以上]、[基于您提供]等描述分析过程的语句作为开头，");
        sb.append("不要添加任何解释性的前缀或后缀，直接从结论内容开始。");
        return sb.toString();
    }

    // ==================== 工具方法 ====================

    private String extractContentDelta(JsonNode chunk) {
        JsonNode choices = chunk.get("choices");
        if (choices == null || choices.isEmpty()) return "";
        JsonNode delta = choices.get(0).get("delta");
        if (delta == null) return "";
        JsonNode contentNode = delta.get("content");
        if (contentNode == null || contentNode.isNull()) return "";
        return contentNode.asString("");
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    private List<String> buildBatchDescriptions(List<List<byte[]>> batches, int totalPages) {
        List<String> descs = new ArrayList<>();
        int pageStart = 1;
        for (List<byte[]> batch : batches) {
            int pageEnd = pageStart + batch.size() - 1;
            if (totalPages == batch.size() && batches.size() == 1) {
                descs.add("全部 " + totalPages + " 页");
            } else if (pageStart == pageEnd) {
                descs.add("第 " + pageStart + " 页");
            } else {
                descs.add("第 " + pageStart + "~" + pageEnd + " 页（共 " + batch.size() + " 页）");
            }
            pageStart += batch.size();
        }
        return descs;
    }

    /**
     * 根据落盘的图片文件数与批次数反推各批次页面范围（0-based，含首尾）。
     * <p>提交时按 batchSize 均匀切分，末批可能不足；这里按同样规则重建，与提交时一致。</p>
     */
    private List<int[]> resolveBatchBounds(String taskId, int totalBatches) {
        // 统计实际落盘页数
        Path dir = IMAGE_ROOT.resolve(taskId);
        int totalPages = 0;
        if (Files.exists(dir)) {
            while (Files.exists(dir.resolve(totalPages + ".png"))) totalPages++;
        }
        List<int[]> bounds = new ArrayList<>();
        if (totalBatches <= 0 || totalPages <= 0) return bounds;
        int batchSize = (int) Math.ceil((double) totalPages / totalBatches);
        int start = 0;
        for (int i = 0; i < totalBatches; i++) {
            int end = Math.min(start + batchSize - 1, totalPages - 1);
            bounds.add(new int[]{start, end});
            start = end + 1;
        }
        return bounds;
    }

    private String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            log.warn("doc-ai: 序列化失败", e);
            return null;
        }
    }

    private List<String> fromJsonList(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<Integer, String> fromJsonIntMap(String json) {
        Map<Integer, String> result = new HashMap<>();
        if (!StringUtils.hasText(json)) return result;
        try {
            Map<String, String> raw = MAPPER.readValue(json, new TypeReference<>() {
            });
            raw.forEach((k, v) -> result.put(Integer.parseInt(k), v));
        } catch (Exception e) {
            log.debug("doc-ai: 解析批次结果 JSON 失败");
        }
        return result;
    }

    private Map<String, String> intMapToStringMap(Map<Integer, String> map) {
        Map<String, String> result = new LinkedHashMap<>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }

    // ==================== SSE 发送 ====================

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            log.debug("doc-ai: SSE 发送失败: event={}", eventName);
        }
    }

    private void sendSseError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
        } catch (Exception ignored) {
        }
        emitter.complete();
    }

    // ==================== 清理 ====================

    /**
     * 清理过期任务：删除记录与磁盘图片。过期条件为 {@code expireAt < now}。
     */
    private void cleanExpiredTasks() {
        List<AiDocumentTask> expired = lambdaQuery()
                .lt(AiDocumentTask::getExpireAt, LocalDateTime.now())
                .list();
        for (AiDocumentTask task : expired) {
            cleanupImages(task.getId());
            removeById(task.getId());
        }
    }

    @Scheduled(fixedDelay = 5L * 60 * 1000, initialDelay = 5L * 60 * 1000)
    public void scheduledCleanup() {
        try {
            cleanExpiredTasks();
        } catch (Exception e) {
            log.warn("doc-ai: 定时清理过期任务失败", e);
        }
    }
}
