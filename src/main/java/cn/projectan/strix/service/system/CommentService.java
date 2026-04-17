package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.CommentMapper;
import cn.projectan.strix.model.db.system.Comment;
import cn.projectan.strix.model.db.system.CommentReaction;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.dict.common.CommonFlag;
import cn.projectan.strix.model.dict.system.NotificationJumpType;
import cn.projectan.strix.model.request.system.comment.CommentAddReq;
import cn.projectan.strix.model.request.system.comment.CommentListReq;
import cn.projectan.strix.model.request.system.comment.CommentUpdateReq;
import cn.projectan.strix.model.response.system.comment.CommentListResp;
import cn.projectan.strix.model.response.system.comment.CommentResp;
import cn.projectan.strix.model.response.system.comment.CommentResp.ReactionUser;
import cn.projectan.strix.util.system.SecurityUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用评论服务
 *
 * @author ProjectAn
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService extends ServiceImpl<CommentMapper, Comment> {

    private final CommentReactionService commentReactionService;
    private final NotificationService notificationService;
    private final SystemManagerService systemManagerService;

    /**
     * 编辑时限（分钟）
     */
    private static final int EDIT_TIME_LIMIT_MINUTES = 5;

    /**
     * 获取评论列表（时间正序，置顶优先）
     */
    public CommentListResp list(CommentListReq req) {
        List<Comment> comments = lambdaQuery()
                .eq(Comment::getBizType, req.getBizType())
                .eq(Comment::getBizId, req.getBizId())
                .like(StringUtils.hasText(req.getKeyword()), Comment::getContent, req.getKeyword())
                .orderByDesc(Comment::getPinned)
                .orderByAsc(Comment::getCreatedTime)
                .list();

        long total = comments.size();
        if (comments.isEmpty()) {
            return new CommentListResp(List.of(), 0);
        }

        // 批量查询作者信息
        Set<String> authorIds = comments.stream()
                .map(Comment::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> authorNameMap = getManagerNameMap(authorIds);

        // 批量查询反应
        List<String> commentIds = comments.stream().map(Comment::getId).toList();
        Map<String, List<CommentReaction>> reactionsMap = commentReactionService.listByCommentIds(commentIds);

        // 收集反应中涉及的所有操作人 ID
        Set<String> reactionOperatorIds = new HashSet<>();
        reactionsMap.values().forEach(reactions ->
                reactions.forEach(r -> {
                    if (r.getCreatedBy() != null) {
                        reactionOperatorIds.add(r.getCreatedBy());
                    }
                })
        );
        reactionOperatorIds.removeAll(authorIds);
        Map<String, String> allNameMap = new HashMap<>(authorNameMap);
        if (!reactionOperatorIds.isEmpty()) {
            allNameMap.putAll(getManagerNameMap(reactionOperatorIds));
        }

        String currentOperatorId = SecurityUtil.getOperatorId();
        LocalDateTime editDeadline = LocalDateTime.now().minusMinutes(EDIT_TIME_LIMIT_MINUTES);

        List<CommentResp> respList = comments.stream().map(comment -> {
            CommentResp resp = new CommentResp(comment);
            resp.setAuthorName(allNameMap.getOrDefault(comment.getCreatedBy(), "未知用户"));
            resp.setMine(Objects.equals(currentOperatorId, comment.getCreatedBy()));
            resp.setEditable(resp.isMine() && comment.getCreatedTime() != null
                    && comment.getCreatedTime().isAfter(editDeadline));

            // 组装反应数据
            List<CommentReaction> commentReactions = reactionsMap.getOrDefault(comment.getId(), List.of());
            Map<String, List<ReactionUser>> reactionMap = new LinkedHashMap<>();
            for (CommentReaction r : commentReactions) {
                reactionMap.computeIfAbsent(r.getEmoji(), k -> new ArrayList<>())
                        .add(new ReactionUser(r.getCreatedBy(),
                                allNameMap.getOrDefault(r.getCreatedBy(), "未知用户")));
            }
            resp.setReactions(reactionMap);
            return resp;
        }).toList();

        return new CommentListResp(respList, total);
    }

    /**
     * 新增评论
     */
    @Transactional(rollbackFor = Exception.class)
    public Comment add(CommentAddReq req) {
        Comment comment = new Comment()
                .setBizType(req.getBizType())
                .setBizId(req.getBizId())
                .setContent(req.getContent())
                .setMentionedIds(req.getMentionedIds())
                .setAttachmentIds(req.getAttachmentIds())
                .setPinned(CommonFlag.NO);

        Assert.isTrue(save(comment), "评论保存失败");

        // @提及通知
        sendMentionNotifications(comment);

        return comment;
    }

    /**
     * 编辑评论（仅本人，限时5分钟内）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, CommentUpdateReq req) {
        Comment comment = getById(id);
        Assert.notNull(comment, "评论不存在");

        String currentOperatorId = SecurityUtil.getOperatorId();
        Assert.isTrue(Objects.equals(currentOperatorId, comment.getCreatedBy()),
                "只能编辑自己的评论");

        LocalDateTime editDeadline = comment.getCreatedTime().plusMinutes(EDIT_TIME_LIMIT_MINUTES);
        Assert.isTrue(LocalDateTime.now().isBefore(editDeadline),
                "已超过编辑时限（" + EDIT_TIME_LIMIT_MINUTES + "分钟）");

        lambdaUpdate()
                .eq(Comment::getId, id)
                .set(Comment::getContent, req.getContent())
                .set(Comment::getMentionedIds, req.getMentionedIds())
                .set(Comment::getAttachmentIds, req.getAttachmentIds())
                .update();

        // 处理新增的 @提及
        comment.setContent(req.getContent());
        comment.setMentionedIds(req.getMentionedIds());
        sendMentionNotifications(comment);
    }

    /**
     * 删除评论（本人或有删除权限）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, boolean hasDeletePermission) {
        Comment comment = getById(id);
        Assert.notNull(comment, "评论不存在");

        String currentOperatorId = SecurityUtil.getOperatorId();
        boolean isMine = Objects.equals(currentOperatorId, comment.getCreatedBy());
        Assert.isTrue(isMine || hasDeletePermission, "无权删除此评论");

        removeById(id);
    }

    /**
     * 切换置顶
     */
    public void togglePin(String id) {
        Comment comment = getById(id);
        Assert.notNull(comment, "评论不存在");

        short newPinned = comment.getPinned() == CommonFlag.YES ? CommonFlag.NO : CommonFlag.YES;
        lambdaUpdate()
                .eq(Comment::getId, id)
                .set(Comment::getPinned, newPinned)
                .update();
    }

    /**
     * 批量获取评论数
     */
    public Map<String, Long> batchCount(String bizType, List<String> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return Map.of();
        }

        List<Comment> comments = lambdaQuery()
                .select(Comment::getBizId)
                .eq(Comment::getBizType, bizType)
                .in(Comment::getBizId, bizIds)
                .list();

        Map<String, Long> countMap = comments.stream()
                .collect(Collectors.groupingBy(Comment::getBizId, Collectors.counting()));

        // 确保所有请求的 bizId 都有结果（没有评论的为 0）
        Map<String, Long> result = new HashMap<>();
        for (String bizId : bizIds) {
            result.put(bizId, countMap.getOrDefault(bizId, 0L));
        }
        return result;
    }

    /**
     * 发送 @提及通知
     */
    private void sendMentionNotifications(Comment comment) {
        if (CollectionUtils.isEmpty(comment.getMentionedIds())) {
            return;
        }

        try {
            String operatorId = SecurityUtil.getOperatorId();
            SystemManager operator = SecurityUtil.getSystemManager();
            String operatorName = operator != null ? operator.getNickname() : "系统";

            // 过滤掉自己
            List<String> receiverIds = comment.getMentionedIds().stream()
                    .filter(id -> !Objects.equals(id, operatorId))
                    .toList();

            if (receiverIds.isEmpty()) {
                return;
            }

            String title = operatorName + " 在评论中提到了你";
            String content = comment.getContent().length() > 100
                    ? comment.getContent().substring(0, 100) + "..."
                    : comment.getContent();

            notificationService.sendNotification(
                    "COMMENT_MENTION",
                    comment.getId(),
                    title,
                    content,
                    NotificationJumpType.NONE,
                    null,
                    null,
                    operatorId,
                    receiverIds
            );
        } catch (Exception e) {
            log.warn("评论 @提及通知发送失败，不影响评论: commentId={}", comment.getId(), e);
        }
    }

    /**
     * 批量查询管理员昵称
     */
    private Map<String, String> getManagerNameMap(Set<String> managerIds) {
        if (managerIds == null || managerIds.isEmpty()) {
            return Map.of();
        }
        List<SystemManager> managers = systemManagerService.lambdaQuery()
                .select(SystemManager::getId, SystemManager::getNickname)
                .in(SystemManager::getId, managerIds)
                .list();
        return managers.stream()
                .collect(Collectors.toMap(SystemManager::getId, SystemManager::getNickname,
                        (a, b) -> a));
    }

}
