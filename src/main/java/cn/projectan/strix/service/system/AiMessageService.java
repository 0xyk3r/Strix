package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.AiMessageMapper;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.dict.system.AiMessageStatus;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 对话消息服务
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Service
@RequiredArgsConstructor
public class AiMessageService extends ServiceImpl<AiMessageMapper, AiMessage> {

    /**
     * 默认加载的历史消息条数
     */
    private static final int DEFAULT_CONTEXT_LIMIT = 20;

    /**
     * 获取会话的历史消息列表（按消息 ID 升序）
     * <p>
     * 注意：必须按雪花主键 {@code id} 排序而非 {@code createdTime}。{@code created_time} 列为秒级
     * DATETIME，同一轮内毫秒级先后保存的 user/assistant 消息会得到相同秒值，按时间排序会出现
     * 不稳定结果（消息顺序错乱）。雪花 id 单调递增且唯一，能保证插入顺序。
     */
    public List<AiMessage> listBySessionId(String sessionId) {
        return lambdaQuery()
                .eq(AiMessage::getSessionId, sessionId)
                .orderByAsc(AiMessage::getId)
                .list();
    }

    /**
     * 获取最近 N 条历史消息作为上下文（不含状态为"生成中"的消息），按消息 ID 升序返回
     * <p>
     * 同 {@link #listBySessionId}，按雪花 id 排序以保证顺序稳定。
     */
    public List<AiMessage> listContextMessages(String sessionId) {
        return lambdaQuery()
                .eq(AiMessage::getSessionId, sessionId)
                .ne(AiMessage::getStatus, AiMessageStatus.GENERATING)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT " + DEFAULT_CONTEXT_LIMIT)
                .list()
                .reversed();
    }

    /**
     * 将消息标记为完成
     */
    public void markCompleted(String messageId, String content, String thinkingContent,
                              Integer promptTokens, Integer completionTokens,
                              Integer cacheHitTokens, Integer cacheWriteTokens, Integer reasoningTokens,
                              String modelConfigId, Long durationMs) {
        lambdaUpdate()
                .eq(AiMessage::getId, messageId)
                .set(AiMessage::getStatus, AiMessageStatus.COMPLETED)
                .set(AiMessage::getContent, content)
                .set(thinkingContent != null, AiMessage::getThinkingContent, thinkingContent)
                .set(promptTokens != null, AiMessage::getPromptTokens, promptTokens)
                .set(completionTokens != null, AiMessage::getCompletionTokens, completionTokens)
                .set(cacheHitTokens != null, AiMessage::getCacheHitTokens, cacheHitTokens)
                .set(cacheWriteTokens != null, AiMessage::getCacheWriteTokens, cacheWriteTokens)
                .set(reasoningTokens != null, AiMessage::getReasoningTokens, reasoningTokens)
                .set(modelConfigId != null, AiMessage::getModelConfigId, modelConfigId)
                .set(durationMs != null, AiMessage::getDurationMs, durationMs)
                .update();
    }

    /**
     * 将消息标记为出错
     */
    public void markError(String messageId, String errorMsg) {
        lambdaUpdate()
                .eq(AiMessage::getId, messageId)
                .set(AiMessage::getStatus, AiMessageStatus.ERROR)
                .set(AiMessage::getErrorMsg, errorMsg)
                .update();
    }

}
