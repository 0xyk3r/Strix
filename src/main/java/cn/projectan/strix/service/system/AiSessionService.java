package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.AiSessionMapper;
import cn.projectan.strix.model.db.system.AiMessage;
import cn.projectan.strix.model.db.system.AiSession;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 对话会话服务
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Service
@RequiredArgsConstructor
public class AiSessionService extends ServiceImpl<AiSessionMapper, AiSession> {

    private final AiMessageService aiMessageService;

    /**
     * 分页查询指定管理员的会话列表
     */
    public Page<AiSession> listByManagerId(String managerId, int pageNum, int pageSize) {
        return lambdaQuery()
                .eq(AiSession::getManagerId, managerId)
                .orderByDesc(AiSession::getCreatedTime)
                .page(new Page<>(pageNum, pageSize));
    }

    /**
     * 删除会话及其全部消息（同一事务，避免半失败留下孤儿消息）
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeWithMessages(String sessionId) {
        removeById(sessionId);
        aiMessageService.lambdaUpdate()
                .eq(AiMessage::getSessionId, sessionId)
                .remove();
    }

    /**
     * 校验会话归属权
     */
    public boolean isOwner(String sessionId, String managerId) {
        return lambdaQuery()
                .eq(AiSession::getId, sessionId)
                .eq(AiSession::getManagerId, managerId)
                .exists();
    }

    /**
     * 重命名会话标题
     */
    public void renameTitle(String sessionId, String newTitle) {
        lambdaUpdate()
                .eq(AiSession::getId, sessionId)
                .set(AiSession::getTitle, newTitle)
                .update();
    }

    /**
     * 切换会话使用的模型配置
     */
    public void switchModel(String sessionId, String modelConfigId) {
        lambdaUpdate()
                .eq(AiSession::getId, sessionId)
                .set(AiSession::getModelConfigId, modelConfigId)
                .update();
    }

    /**
     * 更新会话级系统提示词覆盖。
     * <p>空白（null/空串）表示清除覆盖：显式写入 {@code null}，令 {@code buildRawMessages} 回退使用
     * 模型配置的默认 systemPrompt。故用 {@code setSql(...= null)} 而非条件 set（后者无法把已有值清空）。
     */
    public void updateSystemPrompt(String sessionId, String systemPrompt) {
        String normalized = org.springframework.util.StringUtils.hasText(systemPrompt) ? systemPrompt.trim() : null;
        lambdaUpdate()
                .eq(AiSession::getId, sessionId)
                .set(AiSession::getSystemPrompt, normalized)
                .update();
    }

}
