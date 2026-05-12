package cn.projectan.strix.service.system;

import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionChunk;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 服务集成测试
 * <p>
 * 前置条件：数据库中需存在以下已启用的模型配置：
 * <ul>
 *   <li>{@code key=default} — 文本模型（qwen3.6-max-preview，开启思考模式）</li>
 *   <li>{@code key=default-vision} — 视觉模型（qwen3.5-omni-plus）</li>
 * </ul>
 * <p>
 * TTS/STT/IMAGE_GEN 暂未实现（百炼平台 OpenAI 兼容端点不支持这些模型类型）。
 *
 * @author ProjectAn
 */
@Slf4j
@SpringBootTest
class AiServiceTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private AiModelConfigService aiModelConfigService;

    // ============================================================
    //  文本模型测试
    // ============================================================

    @Test
    @DisplayName("加载 default 模型配置")
    void testLoadDefaultConfig() {
        AiModelConfig config = aiModelConfigService.requireEnabledByKey("default");
        assertNotNull(config, "应能加载到 key=default 的配置");
        assertEquals(AiModelType.TEXT, config.getType(), "应为文本模型");
        assertEquals(1, (int) config.getStatus(), "配置状态应为启用");
        log.info("✅ 模型配置加载成功: name={}, model={}, thinking={}",
                config.getName(), config.getModelName(), config.getEnableThinking());
    }

    @Test
    @DisplayName("同步单轮文本对话")
    void testSyncChat() {
        String response = aiService.chat("default", "你好，请详细的介绍你自己，包括具体的版本信息。");
        assertNotNull(response, "响应不应为 null");
        assertFalse(response.isBlank(), "响应内容不应为空");
        log.info("✅ 同步对话响应:\n{}", response);
    }

    @Test
    @DisplayName("流式对话（含思考内容提取）")
    void testStreamChatWithThinking() {
        List<Message> messages = List.of(new UserMessage("SpaceX 的 Starship V3 版本什么时候第一次试飞? 请联网搜索最新信息，并详细说明你的搜索过程和思考过程。"));
        Flux<ChatResponse> flux = aiService.chatStream("default", messages);

        StringBuilder content = new StringBuilder();
        StringBuilder thinking = new StringBuilder();

        flux.toIterable().forEach(response -> {
            if (response.getResult() == null) return;

            String text = response.getResult().getOutput().getText();
            if (text != null && !text.isBlank()) {
                content.append(text);
            }

            // 提取思考内容：从 chunkChoice.delta._additionalProperties 获取 reasoning_content
            Object chunkChoiceObj = response.getResult().getOutput().getMetadata().get("chunkChoice");
            if (chunkChoiceObj instanceof ChatCompletionChunk.Choice chunkChoice) {
                try {
                    JsonValue reasoningValue = chunkChoice.delta()._additionalProperties().get("reasoning_content");
                    if (reasoningValue != null) {
                        Object val = reasoningValue.asString().orElse(null);
                        if (val instanceof String s && !s.isBlank()) thinking.append(s);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        assertFalse(content.isEmpty(), "响应正文不应为空");
        log.info("✅ 流式对话完成");
        if (!thinking.isEmpty()) {
            log.info("💭 思考内容: {}", thinking);
        } else {
            log.warn("⚠️ 未检测到思考内容（模型可能未返回 reasoning_content）");
        }
        log.info("📝 响应正文: {}", content);
    }

    // ============================================================
    //  视觉模型测试
    // ============================================================

    @Test
    @DisplayName("视觉模型：分析图片内容")
    void testVisionAnalyzeImage() {
        // 使用阿里云官方文档示例图片
        String imageUrl = "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg";
        String result = aiService.analyzeMedia(
                "default-vision",
                "请详细描述这张图片的内容，包括人物、动物、场景等信息。",
                List.of(imageUrl),
                List.of("image/jpeg")
        );
        assertNotNull(result, "视觉模型响应不应为 null");
        assertFalse(result.isBlank(), "视觉模型响应内容不应为空");
        log.info("✅ 视觉模型分析结果:\n{}", result);
    }
}
