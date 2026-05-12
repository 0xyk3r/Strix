package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.AiSessionMapper;
import cn.projectan.strix.model.db.system.AiSession;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 对话会话服务
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Service
@RequiredArgsConstructor
public class AiSessionService extends ServiceImpl<AiSessionMapper, AiSession> {

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
     * 校验会话归属权
     */
    public boolean isOwner(String sessionId, String managerId) {
        return lambdaQuery()
                .eq(AiSession::getId, sessionId)
                .eq(AiSession::getManagerId, managerId)
                .exists();
    }

}
