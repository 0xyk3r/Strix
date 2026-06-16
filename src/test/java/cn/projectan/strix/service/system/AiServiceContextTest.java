package cn.projectan.strix.service.system;

import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link AiService#buildMessages} 上下文构建的纯单元测试（不依赖 Spring 容器 / 数据库 / 网络）。
 * <p>
 * 守护缺陷 #1/#A2：{@code listContextMessages} 已通过 {@code ne(status, GENERATING)} 过滤占位
 * assistant 消息，返回列表末尾仅剩"刚保存的 user 消息"一条需跳过，因此 {@code skipLast} 必须为 1；
 * 若为 2 会多删上一轮真实 assistant 回复，导致多轮上下文缺失。
 *
 * @author ProjectAn
 */
class AiServiceContextTest {

    /**
     * buildMessages 不使用任何注入依赖，传 null 即可构造
     */
    private final AiService aiService = new AiService(null, null, null, null);

    private static AiMessage msg(String role, String content) {
        return new AiMessage().setRole(role).setContent(content);
    }

    @Test
    @DisplayName("buildMessages 仅跳过刚保存的 user 消息，保留上一轮 assistant 回复")
    void buildMessagesKeepsLastAssistant() {
        AiModelConfig config = new AiModelConfig().setType(AiModelType.TEXT);

        // 模拟 listContextMessages 的返回（按 id 升序、已过滤 GENERATING 占位）：
        // 末尾 u3 即本轮刚保存的用户消息
        List<AiMessage> history = List.of(
                msg("user", "u1"), msg("assistant", "a1"),
                msg("user", "u2"), msg("assistant", "a2"),
                msg("user", "u3"));

        List<Message> messages = aiService.buildMessages(config, history, "u3", null);

        // 期望：历史保留 [u1,a1,u2,a2]（仅跳过末尾 u3），再追加本轮当前输入 u3
        // 关键断言：上一轮 assistant 回复 a2 必须存在
        List<String> texts = messages.stream().map(Message::getText).toList();
        assertEquals(List.of("u1", "a1", "u2", "a2", "u3"), texts,
                "应保留上一轮 assistant 回复 a2，且当前输入 u3 仅出现一次");
    }

    @Test
    @DisplayName("buildMessages 含 system prompt 时置于首位")
    void buildMessagesWithSystemPrompt() {
        AiModelConfig config = new AiModelConfig().setType(AiModelType.TEXT).setSystemPrompt("你是助手");

        List<AiMessage> history = List.of(msg("user", "hi"));
        List<Message> messages = aiService.buildMessages(config, history, "hi", null);

        List<String> texts = messages.stream().map(Message::getText).toList();
        assertEquals(List.of("你是助手", "hi"), texts, "system prompt 应在首位，当前输入随后");
    }

    @Test
    @DisplayName("buildMessages 首轮无历史时仅含当前输入")
    void buildMessagesFirstTurn() {
        AiModelConfig config = new AiModelConfig().setType(AiModelType.TEXT);

        // 首轮：listContextMessages 仅返回刚保存的 user 消息
        List<AiMessage> history = List.of(msg("user", "first"));
        List<Message> messages = aiService.buildMessages(config, history, "first", null);

        List<String> texts = messages.stream().map(Message::getText).toList();
        assertEquals(List.of("first"), texts, "首轮应只含当前输入一条");
    }
}
