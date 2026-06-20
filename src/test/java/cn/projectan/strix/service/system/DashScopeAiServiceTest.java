package cn.projectan.strix.service.system;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DashScope 原生 API 集成测试（TTS / 批量 ASR / 图片生成）
 *
 * <p>前置条件：数据库中需存在以下已启用的模型配置：
 * <ul>
 *   <li>{@code key=default-tts} — TTS 模型（cosyvoice-v3.5-plus），
 *       base_url=https://dashscope.aliyuncs.com/api/v1，
 *       prompt_audio_url 已配置参考音频 URL。
 *       <b>注意：首次运行需先执行 {@link #testTtsEnrollVoice()} 注册音色，之后语音合成测试才能通过。</b></li>
 *   <li>{@code key=default-stt} — STT 模型（qwen3-asr-flash），
 *       base_url=https://dashscope.aliyuncs.com/api/v1，ossConfigKey 和 ossBucketName 已配置</li>
 *   <li>{@code key=default-image-gen} — 图片生成模型（qwen-image-2.0-pro），
 *       base_url=https://dashscope.aliyuncs.com/api/v1</li>
 * </ul>
 *
 * @author ProjectAn
 */
@Slf4j
@SpringBootTest
class DashScopeAiServiceTest {

    @Autowired
    private DashScopeAiService dashScopeAiService;

    @Autowired
    private AiTtsVoiceService aiTtsVoiceService;

    // ============================================================
    //  TTS 音色复刻（须先运行，才能进行 TTS 合成测试）
    // ============================================================

    @Test
    @DisplayName("TTS：声音复刻（公网音频 URL）")
    void testTtsCloneVoice() {
        String sampleAudioUrl = "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/cosyvoice/cosyvoice-zeroshot-sample.wav";
        String voiceId = aiTtsVoiceService.cloneVoiceByUrl("default-tts", "测试复刻音色", sampleAudioUrl, "集成测试");
        assertNotNull(voiceId, "voice_id 不应为 null");
        assertFalse(voiceId.isBlank(), "voice_id 不应为空");
        log.info("✅ 声音复刻成功，voice_id: {}", voiceId);
    }

    // ============================================================
    //  TTS 测试（cosyvoice-v3.5-plus，需已注册音色，传入 voiceId）
    // ============================================================

    @Test
    @DisplayName("TTS：合成音频字节（需传入已注册音色 voiceId）")
    void testTtsSynthesizeBytes() {
        // 替换为实际已注册的 voice_id；或先运行 testTtsCloneVoice 获取
        String voiceId = aiTtsVoiceService.listByConfigKey("default-tts").stream()
                .findFirst().map(v -> v.getVoiceId()).orElse(null);
        byte[] audioBytes = dashScopeAiService.synthesizeSpeech("default-tts", "欢迎使用 Strix 智能平台。", voiceId, null);
        assertNotNull(audioBytes, "音频字节不应为 null");
        assertTrue(audioBytes.length > 0, "音频字节长度应大于 0，实际: " + audioBytes.length);
        log.info("✅ TTS 合成成功，音频大小: {} bytes", audioBytes.length);
    }

    // ============================================================
    //  ASR 测试（通过 TTS 合成字节链式测试，无需上传文件）
    // ============================================================

    @Test
    @DisplayName("ASR：批量转录（公网音频 URL）")
    void testSttTranscribeViaUrl() {
        String audioUrl = "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/cosyvoice/cosyvoice-zeroshot-sample.wav";
        String transcription = dashScopeAiService.transcribeAudioUrl("default-stt", null, audioUrl);
        assertNotNull(transcription, "转录结果不应为 null");
        assertFalse(transcription.isBlank(), "转录结果不应为空");
        log.info("✅ ASR 转录结果: {}", transcription);
    }

    // ============================================================
    //  图片生成测试（qwen-image-2.0-pro 多参考图片模式）
    // ============================================================

    @Test
    @DisplayName("图片生成：多参考图片 + 文字提示词（qwen-image-2.0-pro）")
    void testImageGenerate() {
        // 官方多图参考示例
        List<String> imageUrls = List.of(
                "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250925/thtclx/input1.png",
                "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250925/iclsnx/input2.png",
                "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250925/gborgw/input3.png"
        );
        String textPrompt = "图1中的女生穿着图2中的黑色裙子按图3的姿势坐下";

        String imageUrl = dashScopeAiService.generateImage(
                "default-image-gen",
                imageUrls,
                textPrompt,
                "1024*1024"
        );
        assertNotNull(imageUrl, "图片 URL 不应为 null");
        assertFalse(imageUrl.isBlank(), "图片 URL 不应为空");
        assertTrue(imageUrl.startsWith("http"), "图片 URL 应以 http 开头，实际: " + imageUrl);
        log.info("✅ 图片生成成功，图片 URL: {}", imageUrl);
    }

    @Test
    @DisplayName("图片生成：纯文字提示词（无参考图片）")
    void testImageGenerateTextOnly() {
        String imageUrl = dashScopeAiService.generateImage(
                "default-image-gen",
                null,
                "一幅中国水墨画风格的山间溪流，清晨薄雾，竹林掩映",
                "1024*1024"
        );
        assertNotNull(imageUrl, "图片 URL 不应为 null");
        assertFalse(imageUrl.isBlank(), "图片 URL 不应为空");
        assertTrue(imageUrl.startsWith("http"), "图片 URL 应以 http 开头，实际: " + imageUrl);
        log.info("✅ 图片生成（纯文字）成功，图片 URL: {}", imageUrl);
    }
}
