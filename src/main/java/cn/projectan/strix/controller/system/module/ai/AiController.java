package cn.projectan.strix.controller.system.module.ai;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.ai.AiModelStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiSession;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.module.ai.*;
import cn.projectan.strix.model.response.system.ai.AiMessageResp;
import cn.projectan.strix.model.response.system.ai.AiSessionResp;
import cn.projectan.strix.service.system.*;
import cn.projectan.strix.util.common.I18nUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.Executor;

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
    @Qualifier("mvcAsyncExecutor")
    private final Executor mvcAsyncExecutor;

    // ============================================================
    //  会话管理
    // ============================================================

    /**
     * 我的会话列表
     */
    @GetMapping("session")
    @Operation(summary = "AI 会话列表")
    public RetResult<List<AiSessionResp>> getSessionList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Page<AiSession> pageResult = aiSessionService.listByManagerId(loginManagerId(), page, pageSize);
        return RetBuilder.success(pageResult.getRecords().stream().map(session -> {
            AiSessionResp resp = AiSessionResp.from(session);
            AiModelConfig config = aiModelConfigService.getById(session.getModelConfigId());
            if (config != null) resp.setModelConfigName(config.getName());
            return resp;
        }).toList());
    }

    /**
     * 创建新会话
     */
    @PostMapping("session/create")
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
    @StrixLog(operationGroup = "AI 对话", operationName = "删除会话", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除 AI 会话")
    public RetResult<Void> removeSession(@Parameter(description = "会话 ID") @PathVariable String id) {
        Assert.isTrue(aiSessionService.isOwner(id, loginManagerId()), I18nUtil.notFound("field.originalData"));
        aiSessionService.removeById(id);
        aiMessageService.lambdaUpdate()
                .eq(AiMessage::getSessionId, id)
                .remove();
        return RetBuilder.success();
    }

    /**
     * 重命名会话标题
     */
    @PatchMapping("session/{id}/title")
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
    @Operation(summary = "获取 AI 对话历史消息")
    public RetResult<List<AiMessageResp>> getMessages(
            @Parameter(description = "会话 ID") @PathVariable String id) {
        Assert.isTrue(aiSessionService.isOwner(id, loginManagerId()), I18nUtil.notFound("field.originalData"));

        // 关联查询模型配置名称
        List<AiMessageResp> messages = aiMessageService.listBySessionId(id).stream()
                .map(message -> {
                    AiMessageResp resp = AiMessageResp.from(message);
                    // 根据 modelConfigId 查询模型配置名称
                    if (message.getModelConfigId() != null) {
                        AiModelConfig config = aiModelConfigService.getById(message.getModelConfigId());
                        if (config != null) {
                            resp.setModelConfigName(config.getName());
                        }
                    }
                    return resp;
                })
                .toList();

        return RetBuilder.success(messages);
    }

    /**
     * 清空会话所有消息
     */
    @DeleteMapping("chat/{sessionId}/messages/all")
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
    @StrixLog(operationGroup = "AI 对话", operationName = "截断消息", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除指定消息及其之后的所有消息")
    public RetResult<Void> deleteMessagesFrom(
            @Parameter(description = "会话 ID") @PathVariable String sessionId,
            @Parameter(description = "起始消息 ID（含）") @PathVariable String messageId) {
        Assert.isTrue(aiSessionService.isOwner(sessionId, loginManagerId()), I18nUtil.notFound("field.originalData"));
        AiMessage target = aiMessageService.getById(messageId);
        Assert.notNull(target, I18nUtil.notFound("field.originalData"));
        Assert.isTrue(sessionId.equals(target.getSessionId()), I18nUtil.notFound("field.originalData"));
        aiMessageService.lambdaUpdate()
                .eq(AiMessage::getSessionId, sessionId)
                .ge(AiMessage::getCreatedTime, target.getCreatedTime())
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
     * TTS 语音合成
     * <p>返回音频字节流，Content-Type 根据模型配置的 responseFormat 决定（默认 audio/wav）</p>
     */
    @PostMapping("tts/synthesize")
    @Operation(summary = "TTS 语音合成（返回音频文件）")
    @IgnoreEncryption
    public ResponseEntity<byte[]> synthesizeSpeech(@RequestBody @Validated AiTtsSynthesizeReq req) {
        byte[] audioBytes = dashScopeAiService.synthesizeSpeech(req.getConfigKey(), req.getText());

        AiModelConfig config = aiModelConfigService.requireEnabledByKey(req.getConfigKey());
        String format = config.getResponseFormat() != null ? config.getResponseFormat().toLowerCase() : "wav";
        MediaType mediaType = switch (format) {
            case "mp3" -> MediaType.valueOf("audio/mpeg");
            case "ogg" -> MediaType.valueOf("audio/ogg");
            case "pcm" -> MediaType.valueOf("audio/pcm");
            default -> MediaType.valueOf("audio/wav");
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"speech." + format + "\"")
                .contentType(mediaType)
                .body(audioBytes);
    }

    /**
     * TTS 音色注册（声音复刻）
     * <p>读取指定配置的 {@code prompt_audio_url} 作为参考音频，向 DashScope 提交音色注册任务，
     * 等待审核通过后将 voice_id 写入数据库，之后 TTS 合成将自动使用该音色。</p>
     */
    @PostMapping("tts/enroll/{configKey}")
    @StrixLog(operationGroup = "AI TTS", operationName = "音色注册", operationType = SystemLogOperType.ADD)
    @Operation(summary = "TTS 音色注册（声音复刻）")
    public RetResult<String> enrollTtsVoice(
            @Parameter(description = "TTS 模型配置 Key") @PathVariable String configKey) {
        String voiceId = dashScopeAiService.enrollTtsVoice(configKey);
        return RetBuilder.success(voiceId);
    }

    // ============================================================
    //  STT 语音识别（批量）
    // ============================================================

    /**
     * STT 批量语音转录
     * <p>上传音频文件，通过 DashScope 异步任务进行转录，返回识别文本</p>
     */
    @PostMapping("stt/transcribe")
    @Operation(summary = "STT 语音转录（上传音频文件）")
    @IgnoreEncryption
    public RetResult<String> transcribeAudio(
            @Parameter(description = "音频文件") @RequestParam("audio") MultipartFile audio,
            @Parameter(description = "STT 模型配置 Key") @RequestParam("configKey") String configKey) throws Exception {

        Assert.notNull(audio, "音频文件不能为空");
        Assert.isTrue(!audio.isEmpty(), "音频文件不能为空");
        Assert.hasText(configKey, "模型配置 Key 不能为空");

        String text = dashScopeAiService.transcribeAudio(
                configKey,
                audio.getInputStream(),
                audio.getSize(),
                audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.wav"
        );
        return RetBuilder.success(text);
    }

    // ============================================================
    //  图片生成
    // ============================================================

    /**
     * 图片生成（qwen-image-2.0-pro 多模态同步模式）
     * <p>支持多张参考图片 + 文字提示词，同步返回图片 URL</p>
     */
    @PostMapping("image/generate")
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
    //  模型配置增强
    // ============================================================

    /**
     * 获取云服务商可用模型列表
     * <p>从远程 API 获取服务商支持的模型列表，包含模型名称、类型、描述等信息</p>
     */
    @PostMapping("model-config/fetch-models")
    @Operation(summary = "获取云服务商可用模型列表")
    public RetResult<List<cn.projectan.strix.model.response.system.ai.AiModelInfoResp>> fetchModels(
            @RequestBody @Validated AiFetchModelsReq req) {
        try {
            String apiKey = req.getApiKey();

            // 如果 API Key 是占位符，尝试从现有配置中获取
            if ("__USE_EXISTING__".equals(apiKey)) {
                // 根据 Base URL 查找已有配置
                AiModelConfig existingConfig = aiModelConfigService.lambdaQuery()
                        .eq(AiModelConfig::getBaseUrl, req.getBaseUrl())
                        .last("LIMIT 1")
                        .one();

                if (existingConfig != null && existingConfig.getApiKey() != null) {
                    apiKey = existingConfig.getApiKey();
                    log.debug("使用现有配置的 API Key");
                } else {
                    return RetBuilder.error("未找到现有配置的 API Key，请填写 API Key");
                }
            }

            List<cn.projectan.strix.model.response.system.ai.AiModelInfoResp> models =
                    aiModelConfigService.fetchAvailableModels(req.getBaseUrl(), apiKey);
            return RetBuilder.success(models);
        } catch (Exception e) {
            log.error("Failed to fetch models from base URL: {}", req.getBaseUrl(), e);
            return RetBuilder.error("获取模型列表失败: " + e.getMessage());
        }
    }

}
