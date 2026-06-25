package cn.projectan.strix.core.module.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 客户端工厂（保留为扩展点）
 * <p>
 * 聊天类调用已统一由 {@link AiChatClient}（OkHttp）处理，无需此工厂创建聊天客户端。
 * 此类保留用于未来可能的特殊客户端创建需求（如需要特殊认证的提供商）。
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Slf4j
@Component
public class AiClientFactory {
    // 聊天客户端统一使用 AiChatClient（Bean 注入），无需工厂方法
    // 此类保留为扩展点，暂无实现
}

