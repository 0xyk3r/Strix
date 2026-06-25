package cn.projectan.strix.service.system;

import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 服务集成测试
 * <p>
 * 前置条件：数据库中需存在以下已启用的模型配置：
 * <ul>
 *   <li>{@code key=default} — 文本模型</li>
 *   <li>{@code key=default-vision} — 视觉模型</li>
 * </ul>
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
    @DisplayName("视觉模型：分析图片内容")
    void testVisionAnalyzeImage() {
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