package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.CommentReactionMapper;
import cn.projectan.strix.model.db.system.CommentReaction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论反应服务
 *
 * @author ProjectAn
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentReactionService extends ServiceImpl<CommentReactionMapper, CommentReaction> {

    /**
     * 切换反应：如果已存在则删除，不存在则新增
     *
     * @return true=添加, false=移除
     */
    public boolean toggle(String commentId, String emoji, String operatorId) {
        CommentReaction existing = lambdaQuery()
                .eq(CommentReaction::getCommentId, commentId)
                .eq(CommentReaction::getEmoji, emoji)
                .eq(CommentReaction::getCreatedBy, operatorId)
                .one();

        if (existing != null) {
            removeById(existing.getId());
            return false;
        } else {
            CommentReaction reaction = new CommentReaction()
                    .setCommentId(commentId)
                    .setEmoji(emoji);
            save(reaction);
            return true;
        }
    }

    /**
     * 批量查询评论的反应列表
     *
     * @param commentIds 评论 ID 列表
     * @return commentId -> [CommentReaction]
     */
    public Map<String, List<CommentReaction>> listByCommentIds(List<String> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }
        List<CommentReaction> reactions = lambdaQuery()
                .in(CommentReaction::getCommentId, commentIds)
                .list();
        return reactions.stream()
                .collect(Collectors.groupingBy(CommentReaction::getCommentId));
    }

}
