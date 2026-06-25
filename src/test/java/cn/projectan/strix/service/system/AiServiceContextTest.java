package cn.projectan.strix.service.system;

import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link AiService#buildRawMessages} 上下文构建的纯单元测试（不依赖 Spring 容器 / 数据库 / 网络）。
 */
class AiServiceContextTest {

    /**
     * buildRawMessages 不使用任何注入依赖，传 null 即可构造（6 个依赖全为 null）
     */
    private final AiService aiService = new AiService(null, null, null, null, null, null);

    private static AiMessage msg(String role, String content) {
        return new AiMessage().setRole(role).setContent(content);
    }

    private static String textOf(Map<String, Object> msg) {
        Object content = msg.get("content");
        return content instanceof String s ? s : "";
    }

    @Test
    @DisplayName("buildRawMessages 仅跳过刚保存的 user 消息，保留上一轮 assistant 回复")
    void buildMessagesKeepsLastAssistant() {
        AiModelConfig config = new AiModelConfig().setType(AiModelType.TEXT);

        List<AiMessage> history = List.of(
                msg("user", "u1"), msg("assistant", "a1"),
                msg("user", "u2"), msg("assistant", "a2"),
                msg("user", "u3"));

        List<Map<String, Object>> messages = aiService.buildRawMessages(config, history, "u3", null);

        List<String> texts = messages.stream().map(AiServiceContextTest::textOf).toList();
        assertEquals(List.of("u1", "a1", "u2", "a2", "u3"), texts,
                "应保留上一轮 assistant 回复 a2，且当前输入 u3 仅出现一次");
    }

    @Test
    @DisplayName("buildRawMessages 含 system prompt 时置于首位")
    void buildMessagesWithSystemPrompt() {
        AiModelConfig config = new AiModelConfig().setType(AiModelType.TEXT).setSystemPrompt("你是助手");

        List<AiMessage> history = List.of(msg("user", "hi"));
        List<Map<String, Object>> messages = aiService.buildRawMessages(config, history, "hi", null);

        List<String> texts = messages.stream().map(AiServiceContextTest::textOf).toList();
        assertEquals(List.of("你是助手", "hi"), texts, "system prompt 应在首位，当前输入随后");
    }

    @Test
    @DisplayName("buildRawMessages 首轮无历史时仅含当前输入")
    void buildMessagesFirstTurn() {
        AiModelConfig config = new AiModelConfig().setType(AiModelType.TEXT);

        List<AiMessage> history = List.of(msg("user", "first"));
        List<Map<String, Object>> messages = aiService.buildRawMessages(config, history, "first", null);

        List<String> texts = messages.stream().map(AiServiceContextTest::textOf).toList();
        assertEquals(List.of("first"), texts, "首轮应只含当前输入一条");
    }
}