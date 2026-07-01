package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.ai.AiChatClient;
import cn.projectan.strix.core.module.ai.AiJson;
import cn.projectan.strix.core.module.ai.provider.AiProviderAdapter;
import cn.projectan.strix.core.module.ai.provider.AiProviderRegistry;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import cn.projectan.strix.model.request.system.tool.document.DocumentAiAnalyzeReq;
import cn.projectan.strix.model.response.system.tool.document.DocumentAiModelResp;
import cn.projectan.strix.model.response.system.tool.document.DocumentAiSubmitResp;
import cn.projectan.strix.util.document.AsposeCellsUtil;
import cn.projectan.strix.util.document.AsposePdfUtil;
import cn.projectan.strix.util.document.AsposeSlidesUtil;
import cn.projectan.strix.util.document.AsposeWordsUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
 * 将文档页面转换为图片，分批并行调用视觉模型分析，可选使用文本模型合并结果。
 * 任务通过 ConcurrentHashMap 存储（内存级别，不持久化），SSE 推送实时进度。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/6/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAiAnalyzeService {

    private static final ObjectMapper MAPPER = AiJson.mapper();
    private static final long TASK_TTL_MS = 30L * 60 * 1000; // 30 分钟

    private final AiModelConfigService aiModelConfigService;
    private final AiChatClient aiChatClient;
    private final AiProviderRegistry providerRegistry;

    /**
     * 任务存储（taskId → 任务信息）
     */
    private final ConcurrentHashMap<String, DocumentAiTask> taskMap = new ConcurrentHashMap<>();
    /**
     * SSE 发射器存储（taskId → SseEmitter）
     */
    private final ConcurrentHashMap<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

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
     * 列出已启用的文本模型（用于合并步骤）
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
     *
     * @param file 文档文件
     * @param req  分析请求参数
     * @return 任务提交响应（含 taskId、批次信息和图片索引范围）
     */
    public DocumentAiSubmitResp submitAnalyze(MultipartFile file, DocumentAiAnalyzeReq req) throws Exception {
        cleanExpiredTasks();

        // 校验 merge 参数
        if (req.isMerge()) {
            Assert.hasText(req.getTextModelKey(), "合并模式下，文本模型不能为空");
        }

        // 转换文档为图片
        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        List<byte[]> images = convertToImages(fileBytes, originalFilename);
        Assert.notEmpty(images, "文档转图片失败，请检查文件格式和内容");

        // 分批
        List<List<byte[]>> batches = partition(images, req.getBatchSize());
        List<String> batchDescriptions = buildBatchDescriptions(batches, images.size());

        // 计算每批次的页面索引范围（0-based，含首尾）
        List<List<Integer>> batchPageRanges = new ArrayList<>();
        int pageStart = 0;
        for (List<byte[]> batch : batches) {
            batchPageRanges.add(List.of(pageStart, pageStart + batch.size() - 1));
            pageStart += batch.size();
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        DocumentAiTask task = new DocumentAiTask(taskId, images.size(), batches.size());
        task.setPageImages(images); // 存储页面图片以供前端获取
        taskMap.put(taskId, task);

        // 异步执行分析（虚拟线程）
        Thread.ofVirtual().name("doc-ai-" + taskId).start(() ->
                analyzeAsync(task, batches, batchDescriptions, req));

        return new DocumentAiSubmitResp(taskId, images.size(), batches.size(),
                batchDescriptions, batchPageRanges);
    }

    /**
     * 获取指定任务的页面图片字节数组
     *
     * @param taskId    任务 ID
     * @param pageIndex 页面下标（0-based）
     * @return 图片字节数组，任务不存在或索引越界时返回 null
     */
    public byte[] getPageImage(String taskId, int pageIndex) {
        DocumentAiTask task = taskMap.get(taskId);
        if (task == null) return null;
        List<byte[]> images = task.getPageImages();
        if (images == null || pageIndex < 0 || pageIndex >= images.size()) return null;
        return images.get(pageIndex);
    }

    // ==================== SSE ====================

    /**
     * 为指定任务创建 SSE 发射器
     */
    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 分钟超时
        emitterMap.put(taskId, emitter);

        emitter.onCompletion(() -> emitterMap.remove(taskId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitterMap.remove(taskId);
        });
        emitter.onError(e -> emitterMap.remove(taskId));

        DocumentAiTask task = taskMap.get(taskId);
        if (task == null) {
            sendSseEvent(emitter, "error", Map.of("message", "任务不存在"));
            emitter.complete();
        }
        return emitter;
    }

    // ==================== 核心分析逻辑 ====================

    private void analyzeAsync(DocumentAiTask task, List<List<byte[]>> batches,
                              List<String> batchDescriptions, DocumentAiAnalyzeReq req) {
        SseEmitter emitter = null;
        // 等待 SSE 连接就绪（最多等 10 秒）
        for (int i = 0; i < 100; i++) {
            emitter = emitterMap.get(task.getTaskId());
            if (emitter != null) break;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        if (emitter == null) {
            log.warn("doc-ai: 任务 {} 在 10s 内未建立 SSE 连接，跳过", task.getTaskId());
            return;
        }

        final SseEmitter finalEmitter = emitter;

        try {
            // 推送 CONVERTING 阶段
            sendSseEvent(finalEmitter, "stage", Map.of(
                    "stage", "CONVERTING",
                    "message", "文档已转换为图片，准备分析...",
                    "totalPages", task.getTotalPages()));

            // 加载视觉模型配置
            AiModelConfig visionConfig = aiModelConfigService.requireEnabledByKey(req.getVisionModelKey());
            AiProviderAdapter visionAdapter = providerRegistry.getAdapter(visionConfig);

            // 构建批次信息并推送 ANALYZING 阶段
            List<Map<String, Object>> batchInfoList = new ArrayList<>();
            for (int i = 0; i < batches.size(); i++) {
                batchInfoList.add(Map.of("index", i, "pageRange", batchDescriptions.get(i)));
            }
            sendSseEvent(finalEmitter, "stage", Map.of(
                    "stage", "ANALYZING",
                    "message", "开始并行分析各批次...",
                    "totalBatches", batches.size(),
                    "batches", batchInfoList));

            // 并行批次分析
            Map<Integer, String> batchResults = new ConcurrentHashMap<>();
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int i = 0; i < batches.size(); i++) {
                final int batchIndex = i;
                final List<byte[]> batchImages = batches.get(i);
                final String pageRange = batchDescriptions.get(i);

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        StringBuilder content = new StringBuilder();
                        List<Map<String, Object>> messages = buildVisionMessages(
                                visionConfig, req.getPrompt(), batchImages, pageRange);

                        Map<String, Object> body = new LinkedHashMap<>();
                        body.put("model", visionConfig.getModelName());
                        body.put("messages", messages);
                        visionAdapter.applyStreamingParams(body, visionConfig);

                        aiChatClient.streamChat(visionConfig.getBaseUrl(), visionConfig.getApiKey(), body, chunk -> {
                            JsonNode choices = chunk.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JsonNode delta = choices.get(0).get("delta");
                                if (delta != null) {
                                    JsonNode contentNode = delta.get("content");
                                    if (contentNode != null && !contentNode.isNull()) {
                                        String token = contentNode.asString("");
                                        if (!token.isEmpty()) {
                                            content.append(token);
                                            sendSseEvent(finalEmitter, "batch_chunk",
                                                    Map.of("batchIndex", batchIndex, "content", token));
                                        }
                                    }
                                }
                            }
                        });

                        batchResults.put(batchIndex, content.toString());
                        sendSseEvent(finalEmitter, "batch_done", Map.of("batchIndex", batchIndex));

                    } catch (Exception e) {
                        log.error("doc-ai: 批次 {} 分析失败, taskId={}", batchIndex, task.getTaskId(), e);
                        batchResults.put(batchIndex, "");
                        sendSseEvent(finalEmitter, "batch_error",
                                Map.of("batchIndex", batchIndex, "message", e.getMessage() != null
                                        ? e.getMessage() : "分析失败"));
                    }
                }, executor);

                futures.add(future);
            }

            // 等待所有批次完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();

            // 合并步骤
            if (req.isMerge() && StringUtils.hasText(req.getTextModelKey())) {
                sendSseEvent(finalEmitter, "stage", Map.of(
                        "stage", "MERGING",
                        "message", "正在合并分析结果..."));

                AiModelConfig textConfig = aiModelConfigService.requireEnabledByKey(req.getTextModelKey());
                AiProviderAdapter textAdapter = providerRegistry.getAdapter(textConfig);

                String mergeUserPrompt = buildMergePrompt(batchDescriptions, batchResults, req.getPrompt());
                List<Map<String, Object>> mergeMessages = new ArrayList<>();
                if (StringUtils.hasText(textConfig.getSystemPrompt())) {
                    mergeMessages.add(Map.of("role", "system", "content", textConfig.getSystemPrompt()));
                }
                mergeMessages.add(Map.of("role", "user", "content", mergeUserPrompt));

                Map<String, Object> mergeBody = new LinkedHashMap<>();
                mergeBody.put("model", textConfig.getModelName());
                mergeBody.put("messages", mergeMessages);
                textAdapter.applyStreamingParams(mergeBody, textConfig);

                aiChatClient.streamChat(textConfig.getBaseUrl(), textConfig.getApiKey(), mergeBody, chunk -> {
                    JsonNode choices = chunk.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        JsonNode delta = choices.get(0).get("delta");
                        if (delta != null) {
                            JsonNode contentNode = delta.get("content");
                            if (contentNode != null && !contentNode.isNull()) {
                                String token = contentNode.asString("");
                                if (!token.isEmpty()) {
                                    sendSseEvent(finalEmitter, "merge_chunk", Map.of("content", token));
                                }
                            }
                        }
                    }
                });
            }

            task.setStatus("DONE");
            task.setExpireAt(System.currentTimeMillis() + TASK_TTL_MS);
            sendSseEvent(finalEmitter, "done", Map.of("message", "分析完成"));
            finalEmitter.complete();

        } catch (Exception e) {
            log.error("doc-ai: 分析任务出错, taskId={}", task.getTaskId(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            // 失败任务也设置过期时间，及时回收其持有的 pageImages，不必等创建时间兜底
            task.setExpireAt(System.currentTimeMillis() + TASK_TTL_MS);
            sendSseEvent(finalEmitter, "error", Map.of("message", "分析失败: " +
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
            finalEmitter.complete();
        }
    }

    // ==================== 文档转图片 ====================

    /**
     * 根据文件扩展名选择对应 Aspose 工具，将文档转换为各页图片字节数组列表。
     * 内部调用各工具的 toImages() 输出 ZIP，再解压得到有序图片列表。
     */
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
            throw new IllegalArgumentException("不支持的文件格式，支持：.doc .docx .pdf .ppt .pptx .xls .xlsx");
        }
        return unzipImages(zipOut.toByteArray());
    }

    /**
     * 将 ZIP 字节数组解压，按文件名排序后返回各页图片字节列表（PNG）
     */
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

    /**
     * 构建视觉模型分析消息（prompt + 批次图片）
     */
    private List<Map<String, Object>> buildVisionMessages(AiModelConfig config, String prompt,
                                                          List<byte[]> images, String pageRange) {
        List<Map<String, Object>> parts = new ArrayList<>();
        String batchPrompt = prompt + "\n（当前分析范围：" + pageRange + "）";
        parts.add(Map.of("type", "text", "text", batchPrompt));
        for (byte[] img : images) {
            String base64 = Base64.getEncoder().encodeToString(img);
            String dataUri = "data:image/png;base64," + base64;
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", dataUri)));
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        if (StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", parts));
        return messages;
    }

    /**
     * 构建合并提示词（将各批次结果拼接送给文本模型）
     * <p>
     * 提示词要求模型直接输出结论，禁止输出"基于以上内容..."等解释性前缀，确保结果干净。
     * </p>
     */
    private String buildMergePrompt(List<String> descriptions, Map<Integer, String> results,
                                    String originalPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务：").append(originalPrompt).append("\n\n");
        sb.append("以下是文档各部分的分析结果：\n\n");
        for (int i = 0; i < descriptions.size(); i++) {
            String content = results.getOrDefault(i, "（该部分分析失败，无结果）");
            sb.append("【").append(descriptions.get(i)).append("】\n");
            sb.append(content).append("\n\n");
        }
        sb.append("---\n");
        sb.append("请将以上各部分内容综合为一份完整的分析报告。");
        sb.append("要求：直接输出分析结论，不要以诸如[根据以上]、[综合以上]、[基于您提供]等描述分析过程的语句作为开头，");
        sb.append("不要添加任何解释性的前缀或后缀，直接从结论内容开始。");
        return sb.toString();
    }

    // ==================== 工具方法 ====================

    /**
     * 将列表按指定大小分组
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    /**
     * 构建批次页面范围描述，如 "第 1~10 页（共 10 页）"
     */
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
     * 线程安全的 SSE 事件发送
     */
    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            String json = MAPPER.writeValueAsString(data);
            synchronized (emitter) {
                emitter.send(SseEmitter.event().name(eventName).data(json));
            }
        } catch (Exception e) {
            log.debug("doc-ai: SSE 发送失败: event={}, reason={}", eventName, e.getMessage());
        }
    }

    /**
     * 清理已过期任务
     * <p>
     * 两条清理规则：
     * <ol>
     *   <li>正常完成任务：{@code expireAt}（DONE 时设为完成后 30 分钟）到期后清理；</li>
     *   <li>兜底：任何任务（含长期 PROCESSING 未连接 SSE、FAILED 未设 expireAt 的）
     *       自创建起超过 {@link #TASK_TTL_MS} 一律清理。避免 PROCESSING/FAILED 任务
     *       持有整份文档的 {@code pageImages} 字节数组永不释放导致 OOM。</li>
     * </ol>
     * 同时清理已无对应任务的孤儿 SSE 发射器。
     */
    private void cleanExpiredTasks() {
        long now = System.currentTimeMillis();
        taskMap.entrySet().removeIf(e -> {
            DocumentAiTask t = e.getValue();
            boolean expired = t.getExpireAt() > 0 && t.getExpireAt() < now;
            boolean tooOld = now - t.getCreatedAt() > TASK_TTL_MS;
            return expired || tooOld;
        });
        // 清理无主 SSE 发射器（对应任务已被移除）
        emitterMap.keySet().removeIf(taskId -> !taskMap.containsKey(taskId));
    }

    /**
     * 定时清理过期任务（每 5 分钟），确保 PROCESSING/FAILED 任务的图片字节最终被释放，
     * 不再仅依赖新任务提交时触发清理。
     */
    @Scheduled(fixedDelay = 5L * 60 * 1000, initialDelay = 5L * 60 * 1000)
    public void scheduledCleanup() {
        cleanExpiredTasks();
    }

    // ==================== 任务 POJO ====================

    @Data
    public static class DocumentAiTask {
        private final String taskId;
        private final int totalPages;
        private final int totalBatches;
        private String status = "PROCESSING";
        private String errorMessage;
        private long expireAt;
        private final long createdAt = System.currentTimeMillis();
        /**
         * 文档各页图片字节数组（PNG，0-based），用于前端展示转换结果。
         * 与任务共享生命周期，30 分钟后随任务一起被清理。
         */
        private List<byte[]> pageImages;

        public DocumentAiTask(String taskId, int totalPages, int totalBatches) {
            this.taskId = taskId;
            this.totalPages = totalPages;
            this.totalBatches = totalBatches;
        }
    }

}
