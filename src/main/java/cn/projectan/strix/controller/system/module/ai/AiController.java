package cn.projectan.strix.controller.system.module.ai;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.ai.AiFilePreviewSigner;
import cn.projectan.strix.core.module.ai.AiModelStore;
import cn.projectan.strix.core.module.ai.tts.TtsAudioListener;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.annotation.RateLimit;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiSession;
import cn.projectan.strix.model.db.system.OssFile;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.module.ai.*;
import cn.projectan.strix.model.response.system.ai.*;
import cn.projectan.strix.service.system.*;
import cn.projectan.strix.util.common.I18nUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * AI 对话交互接口
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Slf4j
@RestController
@RequestMapping("system/ai")
@RequiredArgsConstructor
@Tag(name = "系统模块 - AI 对话")
public class AiController extends BaseSystemController {

    private final AiService aiService;
    private final DashScopeAiService dashScopeAiService;
    private final AiSessionService aiSessionService;
    private final AiMessageService aiMessageService;
    private final AiModelConfigService aiModelConfigService;
    private final AiModelStore aiModelStore;
    private final AiTaskService aiTaskService;
    private final AiTtsVoiceService aiTtsVoiceService;
    private final AiFilePreviewSigner aiFilePreviewSigner;
    private final OssFileService ossFileService;
    @Qualifier("mvcAsyncExecutor")
    private final Executor mvcAsyncExecutor;

    // ============================================================
    //  会话管理
    // ============================================================

    /**
     * 我的会话列表
     */
    @GetMapping("session")
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @Operation(summary = "AI 会话列表")
    public RetResult<List<AiSessionResp>> getSessionList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Page<AiSession> pageResult = aiSessionService.listByManagerId(loginManagerId(), page, pageSize);
        List<AiSession> records = pageResult.getRecords();
        Map<String, String> nameMap = loadModelConfigNames(
                records.stream().map(AiSession::getModelConfigId).toList());
        return RetBuilder.success(records.stream().map(session -> {
            AiSessionResp resp = AiSessionResp.from(session);
            resp.setModelConfigName(nameMap.get(session.getModelConfigId()));
            return resp;
        }).toList());
    }

    /**
     * 批量加载模型配置 id→name 映射，避免循环内逐条 getById（N+1 查询）
     */
    private Map<String, String> loadModelConfigNames(Collection<String> configIds) {
        Set<String> ids = configIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return aiModelConfigService.listByIds(ids).stream()
                .collect(Collectors.toMap(AiModelConfig::getId, AiModelConfig::getName, (a, b) -> a));
    }

    /**
     * 创建新会话
     */
    @PostMapping("session/create")
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @StrixLog(operationGroup = "AI 对话", operationName = "创建会话", operationType = SystemLogOperType.ADD)
    @Operation(summary = "新建 AI 会话")
    public RetResult<AiSessionResp> createSession(@RequestBody @Validated AiSessionCreateReq req) {
        AiModelConfig config = aiModelConfigService.getById(req.getModelConfigId());
        Assert.notNull(config, I18nUtil.notFound("field.config"));
        Assert.isTrue(config.getStatus() != null && config.getStatus() == 1, "模型配置未启用");

        AiSession session = new AiSession()
                .setModelConfigId(req.getModelConfigId())
                .setManagerId(loginManagerId())
                .setTitle(req.getTitle())
                .setStatus((short) 0);

        Assert.isTrue(aiSessionService.save(session), "创建会话失败");
        AiSessionResp resp = AiSessionResp.from(session);
        resp.setModelConfigName(config.getName());
        return RetBuilder.success(resp);
    }

    /**
     * 删除会话（同时删除所有消息）
     */
    @PostMapping("session/remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @StrixLog(operationGroup = "AI 对话", operationName = "删除会话", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除 AI 会话")
    public RetResult<Void> removeSession(@Parameter(description = "会话 ID") @PathVariable String id) {
        Assert.isTrue(aiSessionService.isOwner(id, loginManagerId()), I18nUtil.notFound("field.originalData"));
        aiSessionService.removeWithMessages(id);
        return RetBuilder.success();
    }

    /**
     * 重命名会话标题
     */
    @PatchMapping("session/{id}/title")
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @StrixLog(operationGroup = "AI 对话", operationName = "重命名会话", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "重命名 AI 会话标题")
    public RetResult<Void> renameSession(
            @Parameter(description = "会话 ID") @PathVariable String id,
            @RequestBody @Validated AiSessionRenameTitleReq req) {
        Assert.isTrue(aiSessionService.isOwner(id, loginManagerId()), I18nUtil.notFound("field.originalData"));
        aiSessionService.renameTitle(id, req.getTitle());
        return RetBuilder.success();
    }

    /**
     * 切换会话模型
     */
    @PatchMapping("session/{id}/model")
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @StrixLog(operationGroup = "AI 对话", operationName = "切换模型", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "切换 AI 会话模型")
    public RetResult<Void> switchModel(
            @Parameter(description = "会话 ID") @PathVariable String id,
            @RequestBody @Validated AiSessionSwitchModelReq req) {
        // 验证会话所有权
        Assert.isTrue(aiSessionService.isOwner(id, loginManagerId()), I18nUtil.notFound("field.originalData"));

        // 验证模型配置存在且已启用
        AiModelConfig config = aiModelConfigService.getById(req.getModelConfigId());
        Assert.notNull(config, I18nUtil.notFound("field.config"));
        Assert.isTrue(config.getStatus() != null && config.getStatus() == 1, "模型配置未启用");

        // 调用 Service 层切换模型
        aiSessionService.switchModel(id, req.getModelConfigId());
        return RetBuilder.success();
    }

    // ============================================================
    //  消息历史
    // ============================================================

    /**
     * 获取会话历史消息
     */
    @GetMapping("session/{id}/messages")
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @Operation(summary = "获取 AI 对话历史消息")
    public RetResult<List<AiMessageResp>> getMessages(
            @Parameter(description = "会话 ID") @PathVariable String id) {
        Assert.isTrue(aiSessionService.isOwner(id, loginManagerId()), I18nUtil.notFound("field.originalData"));

        // 批量关联查询模型配置名称（避免逐条 getById 的 N+1）
        List<AiMessage> rawMessages = aiMessageService.listBySessionId(id);
        Map<String, String> nameMap = loadModelConfigNames(
                rawMessages.stream().map(AiMessage::getModelConfigId).toList());
        List<AiMessageResp> messages = rawMessages.stream()
                .map(msg -> {
                    AiMessageResp resp = AiMessageResp.from(msg);
                    if (msg.getModelConfigId() != null) {
                        resp.setModelConfigName(nameMap.get(msg.getModelConfigId()));
                    }
                    return resp;
                })
                .toList();

        // 为附件填充带签名的预览 URL
        messages.forEach(msg -> {
            if (msg.getAttachments() != null) {
                msg.getAttachments().forEach(att ->
                        att.setPreviewUrl(aiFilePreviewSigner.generatePreviewUrl(att.getFileId())));
            }
        });

        return RetBuilder.success(messages);
    }

    /**
     * 清空会话所有消息
     */
    @DeleteMapping("chat/{sessionId}/messages/all")
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @StrixLog(operationGroup = "AI 对话", operationName = "清空消息", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "清空指定会话的所有消息")
    public RetResult<Void> clearMessages(
            @Parameter(description = "会话 ID") @PathVariable String sessionId) {
        Assert.isTrue(aiSessionService.isOwner(sessionId, loginManagerId()), I18nUtil.notFound("field.originalData"));
        aiMessageService.lambdaUpdate()
                .eq(AiMessage::getSessionId, sessionId)
                .remove();
        return RetBuilder.success();
    }

    /**
     * 删除指定消息及其之后的所有消息（用于编辑重发）
     */
    @DeleteMapping("chat/{sessionId}/messages/from/{messageId}")
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @StrixLog(operationGroup = "AI 对话", operationName = "截断消息", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除指定消息及其之后的所有消息")
    public RetResult<Void> deleteMessagesFrom(
            @Parameter(description = "会话 ID") @PathVariable String sessionId,
            @Parameter(description = "起始消息 ID（含）") @PathVariable String messageId) {
        Assert.isTrue(aiSessionService.isOwner(sessionId, loginManagerId()), I18nUtil.notFound("field.originalData"));
        AiMessage target = aiMessageService.getById(messageId);
        Assert.notNull(target, I18nUtil.notFound("field.originalData"));
        Assert.isTrue(sessionId.equals(target.getSessionId()), I18nUtil.notFound("field.originalData"));
        // 按雪花 id 截断（id 单调递增，"该消息及之后"语义精确且唯一；created_time 为秒级会因并列误删）
        aiMessageService.lambdaUpdate()
                .eq(AiMessage::getSessionId, sessionId)
                .ge(AiMessage::getId, target.getId())
                .remove();
        return RetBuilder.success();
    }

    // ============================================================
    //  流式对话 SSE
    // ============================================================

    /**
     * 发送消息（SSE 流式响应）
     * <p>
     * SSE 事件类型：
     * <ul>
     *   <li>{@code thinking} — 思考内容块（qwen3 思考模式）</li>
     *   <li>{@code content}  — 正文内容块</li>
     *   <li>{@code done}     — 流式完成，含 messageId/Token 消耗信息</li>
     *   <li>{@code error}    — 出错</li>
     * </ul>
     */
    @PostMapping(value = "chat/{sessionId}/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @RateLimit(limit = 30, window = 60, key = "ai:chat", message = "AI 对话请求过于频繁，请稍后再试")
    @Operation(summary = "发送 AI 消息（SSE 流式）")
    public SseEmitter sendMessage(
            @Parameter(description = "会话 ID") @PathVariable String sessionId,
            @RequestBody @Validated AiChatMessageReq req) {

        SseEmitter emitter = new SseEmitter(180_000L);
        String managerId = loginManagerId();

        mvcAsyncExecutor.execute(() ->
                aiService.streamChat(sessionId, req.getContent(), req.getAttachments(), emitter, managerId)
        );

        return emitter;
    }

    /**
     * 重新生成最后一条 AI 回复（SSE 流式）
     * <p>删除最后一条 assistant 消息并重新生成，事件格式与 sendMessage 相同。</p>
     */
    @PostMapping(value = "chat/{sessionId}/regenerate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@ss.hasPermission('system:ai:chat')")
    @RateLimit(limit = 30, window = 60, key = "ai:chat", message = "AI 对话请求过于频繁，请稍后再试")
    @Operation(summary = "重新生成最后一条 AI 回复（SSE 流式）")
    public SseEmitter regenerate(
            @Parameter(description = "会话 ID") @PathVariable String sessionId) {

        SseEmitter emitter = new SseEmitter(180_000L);
        String managerId = loginManagerId();

        mvcAsyncExecutor.execute(() ->
                aiService.streamRegenerate(sessionId, emitter, managerId)
        );

        return emitter;
    }

    // ============================================================
    //  TTS 语音合成
    // ============================================================

    /**
     * TTS 语音合成（非流式）
     * <p>返回音频字节流，Content-Type 根据合成参数（params/模型配置）的 format 决定（默认 audio/mpeg）</p>
     */
    @PostMapping("tts/synthesize")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @RateLimit(limit = 10, window = 60, key = "ai:media", message = "AI 生成请求过于频繁，请稍后再试")
    @Operation(summary = "TTS 语音合成（返回音频文件）")
    @IgnoreEncryption
    public ResponseEntity<byte[]> synthesizeSpeech(@RequestBody @Validated AiTtsSynthesizeReq req) {
        byte[] audioBytes = dashScopeAiService.synthesizeSpeech(
                req.getConfigKey(), req.getText(), req.getVoiceId(), req.getParams());

        AiModelConfig config = aiModelConfigService.requireEnabledByKey(req.getConfigKey());
        String format = dashScopeAiService.mergeTtsParams(config, req.getVoiceId(), req.getParams()).format();
        format = format != null ? format.toLowerCase() : "mp3";
        MediaType mediaType = switch (format) {
            case "wav" -> MediaType.valueOf("audio/wav");
            case "ogg", "opus" -> MediaType.valueOf("audio/ogg");
            case "pcm" -> MediaType.valueOf("audio/pcm");
            default -> MediaType.valueOf("audio/mpeg");
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"speech." + format + "\"")
                .contentType(mediaType)
                .body(audioBytes);
    }

    /**
     * TTS 流式语音合成（HTTP SSE）
     * <p>逐段返回 Base64 音频块，事件类型 {@code audio}/{@code done}/{@code error}。</p>
     */
    @PostMapping(value = "tts/synthesize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @RateLimit(limit = 10, window = 60, key = "ai:media", message = "AI 生成请求过于频繁，请稍后再试")
    @Operation(summary = "TTS 流式语音合成（SSE，逐段 Base64 音频）")
    public SseEmitter synthesizeSpeechStream(@RequestBody @Validated AiTtsSynthesizeReq req) {
        SseEmitter emitter = new SseEmitter(180_000L);
        mvcAsyncExecutor.execute(() -> {
            try {
                dashScopeAiService.synthesizeSpeechStream(
                        req.getConfigKey(), req.getText(), req.getVoiceId(), req.getParams(),
                        new TtsAudioListener() {
                            @Override
                            public void onAudio(byte[] audio) {
                                try {
                                    emitter.send(SseEmitter.event().name("audio")
                                            .data(Base64.getEncoder().encodeToString(audio)));
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            }

                            @Override
                            public void onError(String message) {
                                try {
                                    emitter.send(SseEmitter.event().name("error").data(message));
                                } catch (Exception ignored) {
                                }
                                emitter.complete();
                            }

                            @Override
                            public void onCompleted() {
                                try {
                                    emitter.send(SseEmitter.event().name("done").data("{}"));
                                } catch (Exception ignored) {
                                }
                                emitter.complete();
                            }
                        });
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    // ============================================================
    //  TTS 音色管理（声音复刻 / 声音设计）
    // ============================================================

    /**
     * 声音复刻（上传音频文件，经 OSS 转公网 URL）—— 异步任务
     */
    @PostMapping("tts/voice/clone/upload")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @RateLimit(limit = 10, window = 60, key = "ai:media", message = "AI 生成请求过于频繁，请稍后再试")
    @StrixLog(operationGroup = "AI TTS", operationName = "声音复刻(上传)", operationType = SystemLogOperType.ADD)
    @Operation(summary = "声音复刻 - 上传音频（异步，返回 taskId）")
    @IgnoreEncryption
    public RetResult<String> cloneVoiceByUpload(
            @Parameter(description = "参考音频文件") @RequestParam("audio") MultipartFile audio,
            @Parameter(description = "TTS 模型配置 Key") @RequestParam("configKey") String configKey,
            @Parameter(description = "音色名称") @RequestParam("name") String name,
            @Parameter(description = "备注") @RequestParam(value = "remark", required = false) String remark)
            throws Exception {
        Assert.notNull(audio, "参考音频不能为空");
        Assert.isTrue(!audio.isEmpty(), "参考音频不能为空");
        Assert.hasText(configKey, "模型配置 Key 不能为空");
        Assert.hasText(name, "音色名称不能为空");

        byte[] audioBytes = audio.getBytes();
        String filename = audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "voice.wav";
        String taskId = aiTaskService.submit("tts-voice-clone", () ->
                aiTtsVoiceService.cloneVoiceByUpload(configKey, name,
                        new ByteArrayInputStream(audioBytes), audioBytes.length, filename, remark));
        return RetBuilder.success(taskId);
    }

    /**
     * 声音复刻（公网音频 URL）—— 异步任务
     */
    @PostMapping("tts/voice/clone")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @RateLimit(limit = 10, window = 60, key = "ai:media", message = "AI 生成请求过于频繁，请稍后再试")
    @StrixLog(operationGroup = "AI TTS", operationName = "声音复刻(URL)", operationType = SystemLogOperType.ADD)
    @Operation(summary = "声音复刻 - 公网 URL（异步，返回 taskId）")
    public RetResult<String> cloneVoiceByUrl(@RequestBody @Validated AiTtsVoiceCloneReq req) {
        String taskId = aiTaskService.submit("tts-voice-clone", () ->
                aiTtsVoiceService.cloneVoiceByUrl(req.getConfigKey(), req.getName(), req.getAudioUrl(), req.getRemark()));
        return RetBuilder.success(taskId);
    }

    /**
     * 声音设计（文字描述）—— 异步任务
     * <p>成功后 result 为 {@code voiceId|previewBase64}（预览音频 Base64，| 分隔）。</p>
     */
    @PostMapping("tts/voice/design")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @RateLimit(limit = 10, window = 60, key = "ai:media", message = "AI 生成请求过于频繁，请稍后再试")
    @StrixLog(operationGroup = "AI TTS", operationName = "声音设计", operationType = SystemLogOperType.ADD)
    @Operation(summary = "声音设计（异步，返回 taskId，result 为 voiceId|预览Base64）")
    public RetResult<String> designVoice(@RequestBody @Validated AiTtsVoiceDesignReq req) {
        String taskId = aiTaskService.submit("tts-voice-design", () ->
                aiTtsVoiceService.designVoice(req.getConfigKey(), req.getName(),
                        req.getVoicePrompt(), req.getPreviewText(), req.getRemark()));
        return RetBuilder.success(taskId);
    }

    /**
     * 音色列表（按 TTS 配置 Key）
     */
    @GetMapping("tts/voice/list")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @Operation(summary = "TTS 音色列表")
    public RetResult<List<AiTtsVoiceResp>> listVoices(
            @Parameter(description = "TTS 模型配置 Key") @RequestParam String configKey) {
        List<AiTtsVoiceResp> list =
                aiTtsVoiceService.listByConfigKey(configKey).stream()
                        .map(AiTtsVoiceResp::from)
                        .toList();
        return RetBuilder.success(list);
    }

    /**
     * 同步云端音色（拉取 DashScope 账号下属于该模型的历史音色到本地）
     */
    @PostMapping("tts/voice/sync")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @RateLimit(limit = 10, window = 60, key = "ai:media", message = "AI 生成请求过于频繁，请稍后再试")
    @Operation(summary = "同步云端 TTS 音色（返回新增数量）")
    public RetResult<Integer> syncVoices(
            @Parameter(description = "TTS 模型配置 Key") @RequestParam String configKey) {
        return RetBuilder.success(aiTtsVoiceService.syncVoices(configKey));
    }

    /**
     * 删除音色（同步删除 DashScope 音色 + 本地）
     */
    @PostMapping("tts/voice/remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @StrixLog(operationGroup = "AI TTS", operationName = "删除音色", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除 TTS 音色")
    public RetResult<Void> removeVoice(@Parameter(description = "音色记录 ID") @PathVariable String id) {
        aiTtsVoiceService.deleteVoice(id);
        return RetBuilder.success();
    }

    // ============================================================
    //  STT 语音识别（批量）
    // ============================================================

    /**
     * STT 批量语音转录 —— 异步任务
     * <p>转录通过 DashScope 异步任务完成（可达数分钟），改为提交后立即返回 taskId，
     * 前端通过 {@code GET task/{taskId}} 轮询；成功后 result 为识别文本。</p>
     */
    @PostMapping("stt/transcribe")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @RateLimit(limit = 10, window = 60, key = "ai:media", message = "AI 生成请求过于频繁，请稍后再试")
    @Operation(summary = "STT 语音转录（异步，返回 taskId）")
    @IgnoreEncryption
    public RetResult<String> transcribeAudio(
            @Parameter(description = "音频文件") @RequestParam("audio") MultipartFile audio,
            @Parameter(description = "STT 模型配置 Key") @RequestParam("configKey") String configKey,
            @Parameter(description = "请求级参数(JSON，可选)") @RequestParam(value = "params", required = false) String params)
            throws Exception {

        Assert.notNull(audio, "音频文件不能为空");
        Assert.isTrue(!audio.isEmpty(), "音频文件不能为空");
        Assert.hasText(configKey, "模型配置 Key 不能为空");

        // 同步读取上传字节：请求结束后 MultipartFile 临时文件会被清理，异步线程内无法再读流
        byte[] audioBytes = audio.getBytes();
        String filename = audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.wav";

        String taskId = aiTaskService.submit("stt-transcribe", () ->
                dashScopeAiService.transcribeAudio(
                        configKey, params, new ByteArrayInputStream(audioBytes), audioBytes.length, filename));
        return RetBuilder.success(taskId);
    }

    // ============================================================
    //  异步任务状态查询
    // ============================================================

    /**
     * 查询 AI 异步任务状态（TTS 音色注册 / STT 转写）
     * <p>任务按当前用户隔离，仅能查询本人提交的任务。</p>
     */
    @GetMapping("task/{taskId}")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @Operation(summary = "查询 AI 异步任务状态")
    public RetResult<AiTaskStatusResp> getTaskStatus(
            @Parameter(description = "任务 ID") @PathVariable String taskId) {
        AiTaskStatusResp status = aiTaskService.get(taskId);
        Assert.notNull(status, I18nUtil.notFound("field.originalData"));
        return RetBuilder.success(status);
    }

    // ============================================================
    //  图片生成
    // ============================================================

    /**
     * 图片生成（qwen-image-2.0-pro 多模态同步模式）
     * <p>支持多张参考图片 + 文字提示词，同步返回图片 URL</p>
     */
    @PostMapping("image/generate")
    @PreAuthorize("@ss.hasPermission('system:ai:workshop')")
    @RateLimit(limit = 10, window = 60, key = "ai:media", message = "AI 生成请求过于频繁，请稍后再试")
    @Operation(summary = "图片生成（同步返回图片 URL）")
    public RetResult<String> generateImage(@RequestBody @Validated AiImageGenerateReq req) {
        String imageUrl = dashScopeAiService.generateImage(
                req.getConfigKey(),
                req.getImageUrls(),
                req.getPrompt(),
                req.getSize()
        );
        return RetBuilder.success(imageUrl);
    }

    // ============================================================
    //  FIM 续写
    // ============================================================

    /**
     * AI 文本续写（FIM Beta）
     * <p>
     * 使用 DeepSeek Beta {@code /completions} 端点实现通用文本续写（续写模式）或填充（FIM 模式）。
     * 仅支持 DeepSeek 提供商模型。
     * </p>
     */
    @PostMapping("fim")
    @PreAuthorize("@ss.hasPermission('system:ai:fim')")
    @StrixLog(operationGroup = "AI 续写", operationName = "文本续写")
    @RateLimit(limit = 30, window = 60, key = "ai:fim", message = "AI 续写请求过于频繁，请稍后再试")
    @Operation(summary = "AI 文本续写（FIM Beta）")
    public RetResult<cn.projectan.strix.model.response.system.ai.AiFimResp> fim(
            @RequestBody @Validated cn.projectan.strix.model.request.system.module.ai.AiFimReq req) {
        cn.projectan.strix.model.response.system.ai.AiFimResp resp = aiService.fim(
                req.getModelKey(), req.getPrompt(), req.getSuffix(),
                req.getMaxTokens(), req.getTemperature());
        return RetBuilder.success(resp);
    }

    /**
     * AI 文本续写（FIM Beta，流式 SSE）
     * <p>
     * 流式版本，SSE 事件格式：content（逐 token）/ done（完成+token统计）/ error（出错）。
     * </p>
     */
    @PostMapping(value = "fim/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@ss.hasPermission('system:ai:fim')")
    @RateLimit(limit = 30, window = 60, key = "ai:fim", message = "AI 续写请求过于频繁，请稍后再试")
    @Operation(summary = "AI 文本续写 SSE 流式（FIM Beta）")
    public SseEmitter streamFim(
            @RequestBody @Validated cn.projectan.strix.model.request.system.module.ai.AiFimReq req) {
        SseEmitter emitter = new SseEmitter(120_000L);
        mvcAsyncExecutor.execute(() ->
                aiService.streamFim(req.getModelKey(), req.getPrompt(), req.getSuffix(),
                        req.getSystemPrompt(), req.getUserContent(), req.getChatPrefix(),
                        req.getMaxTokens(), req.getTemperature(), emitter)
        );
        return emitter;
    }

    // ============================================================
    //  模型配置增强
    // ============================================================

    /**
     * 获取云服务商可用模型列表
     * <p>从远程 API 获取服务商支持的模型列表，包含模型名称、类型、描述等信息</p>
     */
    @PostMapping("model-config/fetch-models")
    @PreAuthorize("@ss.hasPermission('system:ai:model-config')")
    @RateLimit(limit = 10, window = 60, key = "ai:model-fetch", message = "获取模型列表过于频繁，请稍后再试")
    @Operation(summary = "获取云服务商可用模型列表")
    public RetResult<List<AiModelInfoResp>> fetchModels(
            @RequestBody @Validated AiFetchModelsReq req) {
        try {
            String apiKey = req.getApiKey();

            // 如果 API Key 是占位符，从现有配置中取真实 Key：优先按 configId 精确匹配，其次回退按 Base URL 匹配
            if ("__USE_EXISTING__".equals(apiKey)) {
                AiModelConfig existingConfig = null;
                if (org.springframework.util.StringUtils.hasText(req.getConfigId())) {
                    existingConfig = aiModelConfigService.getById(req.getConfigId());
                }
                if (existingConfig == null) {
                    existingConfig = aiModelConfigService.lambdaQuery()
                            .eq(AiModelConfig::getBaseUrl, req.getBaseUrl())
                            .last("LIMIT 1")
                            .one();
                }

                if (existingConfig != null && existingConfig.getApiKey() != null) {
                    apiKey = existingConfig.getApiKey();
                    log.debug("使用现有配置的 API Key");
                } else {
                    return RetBuilder.error("未找到现有配置的 API Key，请填写 API Key");
                }
            }

            List<AiModelInfoResp> models =
                    aiModelConfigService.fetchAvailableModels(req.getBaseUrl(), apiKey);
            return RetBuilder.success(models);
        } catch (Exception e) {
            // 详细的 URL/状态码/异常仅记录在后端日志，不回传前端（避免内部信息泄漏 / SSRF 探测反馈）
            return RetBuilder.error("不兼容的 API 端点");
        }
    }

    // ============================================================
    //  文件预览
    // ============================================================

    /**
     * 文件预览（签名验证，免登录）
     */
    @Anonymous
    @IgnoreEncryption
    @GetMapping("file/{fileId}/preview")
    @Operation(summary = "AI 附件文件预览（签名鉴权）")
    public void previewFile(@PathVariable String fileId,
                            @RequestParam String sign,
                            @RequestParam long expire,
                            HttpServletResponse response) throws IOException {
        if (!aiFilePreviewSigner.verifySign(fileId, sign, expire)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        OssFile ossFile = ossFileService.getById(fileId);
        if (ossFile == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(ossFile.getContentType());
        response.setHeader("Cache-Control", "private, max-age=300");
        ossFileService.downloadToStream(fileId, response.getOutputStream());
        response.flushBuffer();
    }

}
