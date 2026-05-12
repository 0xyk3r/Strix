package cn.projectan.strix.controller.system.module.ai;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.ai.AiModelStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.db.system.AiSession;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.module.ai.AiChatMessageReq;
import cn.projectan.strix.model.request.system.module.ai.AiImageGenerateReq;
import cn.projectan.strix.model.request.system.module.ai.AiSessionCreateReq;
import cn.projectan.strix.model.request.system.module.ai.AiTtsSynthesizeReq;
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
                .eq(cn.projectan.strix.model.db.system.AiMessage::getSessionId, id)
                .remove();
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
        return RetBuilder.success(
                aiMessageService.listBySessionId(id).stream().map(AiMessageResp::from).toList()
        );
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

    // ============================================================
    //  TTS 语音合成
    // ============================================================

    /**
     * TTS 语音合成
     * <p>返回音频字节流，Content-Type 根据模型配置的 responseFormat 决定（默认 audio/wav）</p>
     */
    @PostMapping("tts/synthesize")
    @Operation(summary = "TTS 语音合成（返回音频文件）")
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

}
