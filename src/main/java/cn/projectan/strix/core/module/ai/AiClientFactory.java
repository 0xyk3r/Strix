package cn.projectan.strix.core.module.ai;

import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.AiModelType;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.core.Timeout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 客户端工厂
 * <p>
 * 根据数据库中的 {@link AiModelConfig} 配置，创建对应的 Spring AI 客户端实例。
 * 使用 Spring AI 2.0.0-M6 Options-based 构建方式，baseUrl/apiKey 注入 Options 后由
 * {@code OpenAiSetup.setupSyncClient} 自动创建底层 {@code OpenAIClient}。
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Slf4j
@Component
public class AiClientFactory {

    /**
     * 聊天模型默认超时配置
     * <p>
     * 联网搜索场景下模型需先检索网页再生成内容，响应时间可能超过 60s，
     * 因此将 read 和 request 超时设置为较大值，避免 OkHttp 流式超时中断。
     */
    private static final Timeout CHAT_TIMEOUT = Timeout.builder()
            .connect(Duration.ofSeconds(30))
            .read(Duration.ofMinutes(5))
            .write(Duration.ofSeconds(30))
            .request(Duration.ofMinutes(10))
            .build();

    /**
     * 创建文本/视觉对话模型客户端（TEXT / VISION）
     * <p>
     * 使用 {@link OpenAIOkHttpClient} 直接构建底层客户端，以便配置自定义超时，
     * 避免联网搜索等长耗时场景触发默认 OkHttp 超时。
     * <p>
     * {@link OpenAiChatModel} 内部同时维护同步与异步客户端，两者均需要自定义超时配置。
     */
    public OpenAiChatModel createChatModel(AiModelConfig config) {
        OpenAIClient openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .timeout(CHAT_TIMEOUT)
                .build();
        OpenAIClientAsync openAIClientAsync = OpenAIOkHttpClientAsync.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .timeout(CHAT_TIMEOUT)
                .build();
        return OpenAiChatModel.builder()
                .openAiClient(openAIClient)
                .openAiClientAsync(openAIClientAsync)
                .options(OpenAiChatOptions.builder()
                        .model(config.getModelName())
                        .build())
                .build();
    }

    /**
     * 创建底层同步客户端（用于 SSE 流式调用，绕过 Spring AI 的 Flux 缓冲问题）
     */
    public OpenAIClient createSyncClient(AiModelConfig config) {
        return OpenAIOkHttpClient.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .timeout(CHAT_TIMEOUT)
                .build();
    }

    /**
     * 根据模型类型创建对应客户端实例
     * <p>
     * 当前仅支持通过阿里云百炼 OpenAI 兼容端点调用的模型类型：TEXT 和 VISION。
     * TTS/STT/IMAGE_GEN 需要 DashScope 原生 API（非 OpenAI 兼容），暂未实现。
     */
    public Object createClient(AiModelConfig config) {
        return switch (config.getType()) {
            case AiModelType.TEXT, AiModelType.VISION -> createChatModel(config);
            default -> throw new UnsupportedOperationException(
                    "模型类型 " + config.getType() + " 暂不支持（需 DashScope 原生 API）");
        };
    }

}
